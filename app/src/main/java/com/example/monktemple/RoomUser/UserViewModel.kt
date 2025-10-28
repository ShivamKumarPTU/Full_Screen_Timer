package com.example.monktemple.RoomUser

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.monktemple.UserManager
import com.example.monktemple.Utlis.SessionManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserRepository

    init {
        val userDao = UserDatabase.getDatabase(application).userDao()
        repository = UserRepository(userDao)
    }

    // User methods
    fun saveUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOrUpdateUser(user)
        }
    }

    // Session methods
    fun saveNewSession(session: UserClass) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertMeditationSession(session)
                Log.d("UserViewModel", "Session saved successfully: ${session.sessionId} for user: ${session.sessionOwnerId}")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error saving session", e)
            }
        }
    }

    // Function to get data for the charts
    fun getSessionsForDateRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): LiveData<List<UserClass>> {
        return repository.getSessionsForUserInRange(userId, startDate, endDate)
    }

    // Immediate call for statistics
    suspend fun getSessionsForDateRangeImmediate(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<UserClass> {
        return repository.getSessionsForUserInRangeImmediate(userId, startDate, endDate)
    }

    suspend fun getUserById(userId: String?): User? {
        return repository.getUserById(userId)
    }

    suspend fun getUserByFirebaseUid(firebaseUid: String): User? {
        return repository.getUserByFirebaseUid(firebaseUid)
    }

    suspend fun getAllSessionsImmediate(userId: String): List<UserClass> {
        return withContext(Dispatchers.IO) {
            try {
                repository.getAllSessionsForUserImmediate(userId)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Debug method to check if sessions are being saved
    suspend fun debugSessionCount(userId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                repository.getSessionCount(userId)
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error getting session count", e)
                0
            }
        }
    }

    // Method to get all sessions for debugging
    suspend fun debugGetAllSessions(userId: String): List<UserClass> {
        return withContext(Dispatchers.IO) {
            try {
                repository.getAllSessionsForUserImmediate(userId)
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error getting all sessions", e)
                emptyList()
            }
        }
    }

    // --- Statistics Methods ---

    suspend fun calculateAndSaveStatistics(userId: String, firebaseUid: String?, periodType: String, startDate: Long, endDate: Long) {
        withContext(Dispatchers.IO) {
            repository.calculateAndSaveStatistics(userId, firebaseUid, periodType, startDate, endDate)
        }
    }

    suspend fun getStatistics(userId: String, periodType: String, startDate: Long, endDate: Long): NewStatistics? {
        return withContext(Dispatchers.IO) {
            repository.getStatistics(userId, periodType, startDate, endDate)
        }
    }

    suspend fun getStatisticsByFirebaseUid(firebaseUid: String, periodType: String, startDate: Long, endDate: Long): NewStatistics? {
        return withContext(Dispatchers.IO) {
            repository.getStatisticsByFirebaseUid(firebaseUid, periodType, startDate, endDate)
        }
    }

    fun getStatisticsForUser(userId: String, periodType: String): LiveData<List<NewStatistics>> {
        return repository.getStatisticsForUser(userId, periodType)
    }

    fun getStatisticsForFirebaseUser(firebaseUid: String, periodType: String): LiveData<List<NewStatistics>> {
        return repository.getStatisticsForFirebaseUser(firebaseUid, periodType)
    }

    // Enhanced update method with Firebase support
    suspend fun updateAllStatistics(userId: String, firebaseUid: String?) {
        withContext(Dispatchers.IO) {
            // Update daily statistics
            val (todayStart, todayEnd) = getTodayRange()
            repository.calculateAndSaveStatistics(userId, firebaseUid, "day", todayStart, todayEnd)

            // Update weekly statistics
            val (weekStart, weekEnd) = getWeekRange()
            repository.calculateAndSaveStatistics(userId, firebaseUid, "week", weekStart, weekEnd)
        }
    }

    // Enhanced user management methods
    suspend fun handleUserLogin(firebaseUser: FirebaseUser): String {
        return withContext(Dispatchers.IO) {
            val firebaseUid = firebaseUser.uid
            val userEmail = firebaseUser.email
            val displayName = firebaseUser.displayName ?: "User"
            val photoUrl = firebaseUser.photoUrl?.toString()

            // Check if user exists in local database
            val existingUser = repository.getUserByFirebaseUid(firebaseUid)

            if (existingUser == null) {
                // Create new user with Firebase UID
                val newUser = User(
                    userId = firebaseUid,
                    firebaseUid = firebaseUid,
                    displayName = displayName,
                    email = userEmail,
                    photoUrl = photoUrl,
                    lastLogin = System.currentTimeMillis()
                )
                repository.insertOrUpdateUser(newUser)

                // Check if we need to migrate from anonymous data
                val userManager = UserManager(SessionManager(getApplication()))
                val anonymousUserId = userManager.getCurrentUserID()

                if (!anonymousUserId.isNullOrEmpty() && anonymousUserId != firebaseUid) {
                    // Migrate data from anonymous to Firebase user
                    repository.migrateUserData(anonymousUserId, firebaseUid, firebaseUid)
                }
            } else {
                // Update last login for existing user
                val updatedUser = existingUser.copy(lastLogin = System.currentTimeMillis())
                repository.insertOrUpdateUser(updatedUser)
            }

            firebaseUid
        }
    }

    suspend fun handleUserLogout() {
        withContext(Dispatchers.IO) {
            // All user data (sessions, statistics) remain in database linked to Firebase UID
            // They will be available when user logs back in
            Log.d("UserViewModel", "User logged out - ALL DATA PRESERVED in database")

            // Force refresh any LiveData observers
            repository.refreshData()
        }
    }

    // Enhanced method to ensure data persistence
    suspend fun verifyDataPersistence(firebaseUid: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val user = repository.getUserByFirebaseUid(firebaseUid)
                val sessions = repository.getAllSessionsForUserImmediate(firebaseUid)
                val stats = repository.getAllStatisticsForUserImmediate(firebaseUid)

                Log.d("UserViewModel", "Data persistence check - User: ${user != null}, Sessions: ${sessions.size}, Stats: ${stats.size}")

                user != null
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error verifying data persistence", e)
                false
            }
        }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }

    private fun getWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }
}