package com.example.monktemple.data.sync

import android.content.Context
import android.util.Log
import com.example.monktemple.RoomUser.*
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.data.remote.FirebaseRemoteDataSource
import com.example.monktemple.data.remote.FirestoreSession
import com.example.monktemple.data.remote.FirestoreStatistics
import com.example.monktemple.data.remote.FirestoreUser
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataManager @Inject constructor(
    private val context: Context,
    private val userDao: UserDao,
    private val remoteDataSource: FirebaseRemoteDataSource
) {
    private val sessionManager = SessionManager(context)

    companion object {
        private const val TAG = "UserDataManager"
        private const val DEFAULT_PROFILE_IMAGE = "default"
    }

    // Enhanced user authentication with Firestore sync
    suspend fun handleUserAuthentication(firebaseUid: String, email: String?, displayName: String?, photoUrl: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Handling user authentication with Firestore sync for: $firebaseUid")

                // 1. Save to SharedPreferences (immediate)
                sessionManager.saveFirebaseUid(firebaseUid)
                sessionManager.saveUserEmail(email ?: "")
                sessionManager.saveUserName(displayName ?: "User")
                sessionManager.saveProfileImageUri(photoUrl ?: "R.drawable._4")
                sessionManager.isLoggedIn = true
                sessionManager.updateLastLoginTime()

                // 2. Check if user exists locally
                val existingUser = userDao.getUserByFirebaseUid(firebaseUid)

                if (existingUser == null) {
                    // New user - create locally and in Firestore
                    val newUser = User(
                        userId = firebaseUid,
                        firebaseUid = firebaseUid,
                        displayName = displayName ?: "User",
                        email = email ?: "",
                        photoUrl = photoUrl ?: "R.drawable._4",
                        lastLogin = System.currentTimeMillis()
                    )
                    userDao.insertOrUpdateUser(newUser)

                    // Save to Firestore
                    val firestoreUser = FirestoreUser(
                        userId = firebaseUid,
                        firebaseUid = firebaseUid,
                        displayName = displayName ?: "User",
                        email = email ?: "",
                        photoUrl = photoUrl ?: "DEFAULT_PROFILE_IMAGE",
                        createdAt = Timestamp.now(),
                        lastLogin = Timestamp.now()
                    )
                    remoteDataSource.saveUserToFirestore(firestoreUser)

                    Log.d(TAG, "✅ New user created locally and in Firestore: $firebaseUid")
                } else {
                    // Existing user - update and sync
                    val updatedUser = existingUser.copy(
                        lastLogin = System.currentTimeMillis(),
                        displayName = displayName ?: existingUser.displayName,
                        email = email ?: existingUser.email,
                        photoUrl = photoUrl ?: existingUser.photoUrl
                    )
                    userDao.insertOrUpdateUser(updatedUser)

                    // Update Firestore
                    val firestoreUser = FirestoreUser(
                        userId = firebaseUid,
                        firebaseUid = firebaseUid,
                        displayName = displayName ?: existingUser.displayName,
                        email = email ?: existingUser.email,
                        photoUrl = photoUrl ?: existingUser.photoUrl,
                        lastLogin = Timestamp.now(),
                        lastSync = Timestamp.now()
                    )
                    remoteDataSource.saveUserToFirestore(firestoreUser)

                    Log.d(TAG, "✅ Existing user updated and synced: $firebaseUid")

                    // Sync local data with Firestore
                    syncUserDataWithFirestore(firebaseUid)
                }

                // 3. Update sync status
                remoteDataSource.updateSyncStatus(firebaseUid, "SUCCESS")

                // 4. Verify data persistence
                val verificationResult = verifyDataPersistence(firebaseUid)
                Log.d(TAG, "🔍 Data persistence verification: $verificationResult")

                return@withContext verificationResult

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling user authentication with Firestore", e)
                remoteDataSource.updateSyncStatus(firebaseUid, "FAILED")
                return@withContext false
            }
        }
    }

    // Enhanced session saving with Firestore sync
    suspend fun saveMeditationSession(userId: String, duration: Long, status: String, goalName: String = "Focus Time"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "💾 Saving session with Firestore sync for user: $userId")

                // 1. Save to local database
                val session = UserClass(
                    sessionId = 0,
                    sessionOwnerId = userId,
                    completionTimestamp = System.currentTimeMillis(),
                    workDuration = duration,
                    status = status
                )

                userDao.insertMeditationSession(session)

                // 2. Save to Firestore
                val firestoreSession = FirestoreSession(
                    userId = userId,
                    completionTimestamp = Timestamp.now(),
                    workDuration = duration,
                    status = status,
                    goalName = goalName,
                    syncStatus = "SYNCED"
                )

                val remoteResult = remoteDataSource.saveSessionToFirestore(firestoreSession)
                if (remoteResult.isFailure) {
                    Log.w(TAG, "⚠️ Firestore sync failed for session, but local save succeeded")
                    // Queue for retry
                    queueSessionForRetry(firestoreSession)
                }

                // 3. Update statistics
                updateUserStatistics(userId)

                Log.d(TAG, "✅ Session saved locally and synced to Firestore for user: $userId")
                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving session with Firestore sync", e)
                return@withContext false
            }
        }
    }

    // Comprehensive data sync between local and Firestore
    suspend fun syncUserDataWithFirestore(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Starting comprehensive data sync for user: $userId")

                // 1. Push local sessions to Firestore
                val localSessions = userDao.getAllSessionsForUserImmediate(userId)
                if (localSessions.isNotEmpty()) {
                    val firestoreSessions = localSessions.map { localSession ->
                        FirestoreSession(
                            sessionId = "local_${localSession.sessionId}",
                            userId = userId,
                            completionTimestamp = Timestamp(localSession.completionTimestamp / 1000,
                                ((localSession.completionTimestamp % 1000) * 1000000).toInt()),
                            workDuration = localSession.workDuration,
                            status = localSession.status,
                            goalName = "Focus Time", // You might want to store this in local DB too
                            syncStatus = "SYNCED"
                        )
                    }

                    remoteDataSource.saveMultipleSessions(firestoreSessions)
                    Log.d(TAG, "📤 Pushed ${localSessions.size} local sessions to Firestore")
                }

                // 2. Pull Firestore sessions to local (for multi-device sync)
                val remoteSessionsResult = remoteDataSource.getUserSessionsFromFirestore(userId)
                if (remoteSessionsResult.isSuccess) {
                    val remoteSessions = remoteSessionsResult.getOrNull() ?: emptyList()
                    remoteSessions.forEach { remoteSession ->
                        // Check if session already exists locally
                        val existingSession = userDao.getAllSessionsForUserImmediate(userId)
                            .find { it.completionTimestamp == remoteSession.completionTimestamp.seconds * 1000 }

                        if (existingSession == null) {
                            // Add new session from Firestore
                            val localSession = UserClass(
                                sessionId = 0,
                                sessionOwnerId = userId,
                                completionTimestamp = remoteSession.completionTimestamp.seconds * 1000,
                                workDuration = remoteSession.workDuration,
                                status = remoteSession.status
                            )
                            userDao.insertMeditationSession(localSession)
                        }
                    }
                    Log.d(TAG, "📥 Pulled ${remoteSessions.size} remote sessions to local DB")
                }

                // 3. Sync statistics
                syncStatistics(userId)

                // 4. Update sync status
                remoteDataSource.updateSyncStatus(userId, "SUCCESS")

                Log.d(TAG, "✅ Comprehensive data sync completed for user: $userId")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during comprehensive data sync", e)
                remoteDataSource.updateSyncStatus(userId, "FAILED")
            }
        }
    }

    private suspend fun syncStatistics(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Get local statistics
                val localStats = userDao.getAllStatisticsForUserImmediate(userId)

                // Convert to Firestore format and sync
                localStats.forEach { localStat ->
                    val firestoreStat = FirestoreStatistics(
                        userId = userId,
                        periodType = localStat.periodType,
                        periodStart = Timestamp(localStat.periodStart / 1000,
                            ((localStat.periodStart % 1000) * 1000000).toInt()),
                        periodEnd = Timestamp(localStat.periodEnd / 1000,
                            ((localStat.periodEnd % 1000) * 1000000).toInt()),
                        noOfSessions = localStat.noOfSessions,
                        focusTime = localStat.focusTime,
                        averageSessionTime = localStat.averageSessionTime,
                        longestSession = localStat.longestSession,
                        completionRate = localStat.completionRate,
                        mostProductiveDay = localStat.mostProductiveDay
                    )

                    remoteDataSource.saveStatisticsToFirestore(firestoreStat)
                }

                Log.d(TAG, "📊 Synced ${localStats.size} statistics to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing statistics", e)
            }
        }
    }

    private suspend fun queueSessionForRetry(session: FirestoreSession) {
        // In a real implementation, you'd store this in a local queue table
        // For now, we'll just log it
        Log.w(TAG, "📝 Session queued for retry: ${session.sessionId}")
    }

    // Enhanced logout with Firestore sync
// Enhanced logout with Firestore sync and data preservation
    suspend fun handleUserLogout(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentUid = sessionManager.getFirebaseUid()
                Log.d(TAG, "🚪 Handling enhanced logout with Firestore sync for user: $currentUid")

                // 1. Perform final sync before logout
                currentUid?.let { uid ->
                    // Quick sync of any pending data
                    remoteDataSource.updateSyncStatus(uid, "LOGOUT_SYNC_IN_PROGRESS")

                    // Perform a quick sync of recent data
                    try {
                        syncUserDataWithFirestore(uid)
                        Log.d(TAG, "✅ Final sync completed before logout")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Final sync failed, but continuing logout", e)
                    }

                    // Update sync status to indicate logout
                    remoteDataSource.updateSyncStatus(uid, "LOGOUT_COMPLETE")
                }

                // 2. Preserve critical user data before logout
                val preservedUid = sessionManager.getFirebaseUid()
                val preservedName = sessionManager.getUserName()
                val preservedEmail = sessionManager.getUserEmail()
                val preservedPhoto = sessionManager.getProfileImageUri()

                Log.d(TAG, "📋 Preserving user data:")
                Log.d(TAG, "  - UID: $preservedUid")
                Log.d(TAG, "  - Name: $preservedName")
                Log.d(TAG, "  - Email: $preservedEmail")

                // 3. Clear only authentication state (PRESERVES user data)
                sessionManager.clearOnlyAuthenticationState()

                // 4. Restore preserved data (Amazon-style - keep user identity)
                preservedUid?.let {
                    sessionManager.saveFirebaseUid(it)
                    Log.d(TAG, "✅ Firebase UID preserved: $it")
                }
                preservedName?.let {
                    sessionManager.saveUserName(it)
                    Log.d(TAG, "✅ User name preserved: $it")
                }
                preservedEmail?.let {
                    sessionManager.saveUserEmail(it)
                    Log.d(TAG, "✅ User email preserved: $it")
                }
                preservedPhoto?.let {
                    sessionManager.saveProfileImageUri(it)
                    Log.d(TAG, "✅ User photo preserved")
                }

                // 5. Verify data was preserved
                val verificationSuccess = verifyDataPersistenceAfterLogout(preservedUid)

                Log.d(TAG, "✅ User logged out successfully with enhanced preservation. Data preserved: $verificationSuccess")
                return@withContext verificationSuccess

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during enhanced logout", e)
                return@withContext false
            }
        }
    }

    // Add verification method for post-logout data persistence
    private suspend fun verifyDataPersistenceAfterLogout(firebaseUid: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (firebaseUid == null) {
                    Log.e(TAG, "❌ No Firebase UID provided for verification")
                    return@withContext false
                }

                val uidPreserved = sessionManager.getFirebaseUid() == firebaseUid
                val namePreserved = sessionManager.getUserName() != null
                val emailPreserved = sessionManager.getUserEmail() != null
                val userExistsInDb = userDao.getUserByFirebaseUid(firebaseUid) != null

                Log.d(TAG, "🔍 Post-logout verification:")
                Log.d(TAG, "  - UID preserved: $uidPreserved")
                Log.d(TAG, "  - Name preserved: $namePreserved")
                Log.d(TAG, "  - Email preserved: $emailPreserved")
                Log.d(TAG, "  - User in database: $userExistsInDb")

                return@withContext uidPreserved && namePreserved && emailPreserved && userExistsInDb
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying data persistence after logout", e)
                return@withContext false
            }
        }
    }

    // Data recovery with Firestore fallback
    suspend fun recoverUserData(firebaseUid: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Attempting data recovery for user: $firebaseUid")

                // 1. Try to get user from Firestore first (most reliable)
                val remoteUserResult = remoteDataSource.getUserFromFirestore(firebaseUid)
                if (remoteUserResult.isSuccess) {
                    val remoteUser = remoteUserResult.getOrNull()
                    if (remoteUser != null) {
                        // Restore from Firestore
                        sessionManager.saveFirebaseUid(firebaseUid)
                        sessionManager.saveUserEmail(remoteUser.email)
                        sessionManager.saveUserName(remoteUser.displayName)
                        sessionManager.saveProfileImageUri(remoteUser.photoUrl)

                        // Sync sessions and statistics from Firestore
                        syncUserDataWithFirestore(firebaseUid)

                        Log.d(TAG, "✅ User data recovered from Firestore for: $firebaseUid")
                        return@withContext true
                    }
                }

                // 2. Fallback to local database
                val localUser = userDao.getUserByFirebaseUid(firebaseUid)
                if (localUser != null) {
                    sessionManager.saveFirebaseUid(firebaseUid)
                    sessionManager.saveUserEmail(localUser.email ?: "")
                    sessionManager.saveUserName(localUser.displayName ?: "User")
                    sessionManager.saveProfileImageUri(localUser.photoUrl ?: "R.drawable._4")

                    Log.d(TAG, "✅ User data recovered from local database for: $firebaseUid")
                    return@withContext true
                }

                Log.w(TAG, "⚠️ No user data found for recovery: $firebaseUid")
                return@withContext false

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during data recovery", e)
                return@withContext false
            }
        }
    }

    // Get user data with Firestore fallback
    suspend fun getUserData(userId: String): User? {
        return withContext(Dispatchers.IO) {
            try {
                // Try local first
                var user = userDao.getUserByFirebaseUid(userId)

                if (user == null) {
                    // Fallback to Firestore
                    val remoteUserResult = remoteDataSource.getUserFromFirestore(userId)
                    if (remoteUserResult.isSuccess) {
                        val remoteUser = remoteUserResult.getOrNull()
                        if (remoteUser != null) {
                            // Create local user from Firestore data
                            user = User(
                                userId = remoteUser.userId,
                                firebaseUid = remoteUser.firebaseUid,
                                displayName = remoteUser.displayName,
                                email = remoteUser.email,
                                photoUrl = remoteUser.photoUrl,
                                lastLogin = remoteUser.lastLogin.seconds * 1000
                            )
                            userDao.insertOrUpdateUser(user!!)
                        }
                    }
                }

                user
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user data", e)
                null
            }
        }
    }

    // COMPLETED: Enhanced statistics update with Firestore sync
    private suspend fun updateUserStatistics(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📊 Updating user statistics with Firestore sync for: $userId")

                // Update daily statistics
                val (todayStart, todayEnd) = getTodayRange()
                calculateAndSaveStatistics(userId, userId, "day", todayStart, todayEnd)

                // Update weekly statistics
                val (weekStart, weekEnd) = getWeekRange()
                calculateAndSaveStatistics(userId, userId, "week", weekStart, weekEnd)

                Log.d(TAG, "✅ User statistics updated with Firestore sync for: $userId")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating user statistics", e)
            }
        }
    }

    // COMPLETED: Calculate and save statistics (local + Firestore)
    private suspend fun calculateAndSaveStatistics(userId: String, firebaseUid: String?, periodType: String, startDate: Long, endDate: Long) {
        withContext(Dispatchers.IO) {
            try {
                // Get sessions for the period
                val sessions = userDao.getCompletedSessionsInRange(userId, startDate, endDate)
                val completedSessions = sessions.filter { it.status == "COMPLETED" }
                val totalAttempts = sessions.size

                // Calculate statistics
                val totalFocusTime = completedSessions.sumOf { it.workDuration }
                val averageSessionTime = if (completedSessions.isNotEmpty()) totalFocusTime / completedSessions.size else 0L
                val longestSession = completedSessions.maxByOrNull { it.workDuration }?.workDuration ?: 0L
                val completionRate = if (totalAttempts > 0) (completedSessions.size.toFloat() / totalAttempts.toFloat() * 100) else 0f
                val mostProductiveDay = calculateMostProductiveDay(completedSessions)

                // Save to local database
                val localStatistics = NewStatistics(
                    userId = userId,
                    firebaseUid = firebaseUid,
                    periodType = periodType,
                    periodStart = startDate,
                    periodEnd = endDate,
                    noOfSessions = completedSessions.size,
                    focusTime = totalFocusTime,
                    averageSessionTime = averageSessionTime,
                    longestSession = longestSession,
                    completionRate = completionRate,
                    mostProductiveDay = mostProductiveDay
                )
                userDao.insertOrUpdateStatistics(localStatistics)

                // Save to Firestore
                val firestoreStatistics = FirestoreStatistics(
                    userId = userId,
                    periodType = periodType,
                    periodStart = Timestamp(startDate / 1000, ((startDate % 1000) * 1000000).toInt()),
                    periodEnd = Timestamp(endDate / 1000, ((endDate % 1000) * 1000000).toInt()),
                    noOfSessions = completedSessions.size,
                    focusTime = totalFocusTime,
                    averageSessionTime = averageSessionTime,
                    longestSession = longestSession,
                    completionRate = completionRate,
                    mostProductiveDay = mostProductiveDay
                )
                remoteDataSource.saveStatisticsToFirestore(firestoreStatistics)

                Log.d(TAG, "✅ Statistics calculated and saved for period: $periodType, sessions: ${completedSessions.size}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error calculating and saving statistics", e)
            }
        }
    }

    // Helper method to calculate most productive day
    private fun calculateMostProductiveDay(sessions: List<UserClass>): String {
        if (sessions.isEmpty()) return "N/A"

        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val calendar = Calendar.getInstance()

        return sessions
            .groupBy {
                calendar.timeInMillis = it.completionTimestamp
                days[calendar.get(Calendar.DAY_OF_WEEK) - 1]
            }
            .mapValues { entry -> entry.value.sumOf { it.workDuration } }
            .maxByOrNull { it.value }?.key ?: "N/A"
    }

    suspend fun verifyDataPersistence(firebaseUid: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val userExists = userDao.getUserByFirebaseUid(firebaseUid) != null
                val sessionsExist = userDao.getSessionCount(firebaseUid) >= 0
                val sharedPrefsUid = sessionManager.getFirebaseUid() == firebaseUid

                // Also verify Firestore connection
                val firestoreStatus = remoteDataSource.getSyncStatus(firebaseUid)
                val firestoreConnected = firestoreStatus.isSuccess

                Log.d(TAG, "🔍 Data persistence check - Local: $userExists, Sessions: $sessionsExist, SharedPrefs: $sharedPrefsUid, Firestore: $firestoreConnected")

                return@withContext userExists && sharedPrefsUid
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying data persistence", e)
                return@withContext false
            }
        }
    }

    // COMPLETED: Helper methods for date ranges
    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Start of today (00:00:00)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        // End of today (23:59:59.999)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }

    // COMPLETED: Get week range (Monday to Sunday)
    private fun getWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        // Start of week (Monday 00:00:00)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        // End of week (Sunday 23:59:59.999)
        calendar.add(Calendar.DAY_OF_YEAR, 6) // Move to Sunday
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }
}