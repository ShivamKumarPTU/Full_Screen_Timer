package com.example.monktemple.RoomUser

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.Calendar

class UserRepository(private val userDao: UserDao) {

    // --- User-related operations ---

    suspend fun insertOrUpdateUser(user: User) {
        userDao.insertOrUpdateUser(user)
    }

    suspend fun getUserById(userId: String?): User? {
        return userDao.getUserById(userId)
    }

    suspend fun getUserByFirebaseUid(firebaseUid: String): User? {
        return userDao.getUserByFirebaseUid(firebaseUid)
    }

    fun getAllUsers(): LiveData<List<User>> {
        return userDao.getAllUsers()
    }

    // --- Meditation Session operations ---

    suspend fun insertMeditationSession(session: UserClass) {
        userDao.insertMeditationSession(session)
    }

    // --- Data-fetching operations for Statistics ---

    fun getAllSessionsForUser(userId: String): LiveData<List<UserClass>> {
        return userDao.getAllSessionsForUser(userId)
    }

    fun getSessionsForUserInRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): LiveData<List<UserClass>> {
        return userDao.getSessionsForUserInRange(userId, startDate, endDate)
    }

    // Immediate call for statistics (without LiveData)
    suspend fun getSessionsForUserInRangeImmediate(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<UserClass> {
        return userDao.getSessionsForUserInRangeImmediate(userId, startDate, endDate)
    }

    // Get all sessions for a user immediately (for migration)
    suspend fun getAllSessionsForUserImmediate(userId: String): List<UserClass> {
        return userDao.getAllSessionsForUserImmediate(userId)
    }

    // Get session count for debugging
    suspend fun getSessionCount(userId: String): Int {
        return userDao.getSessionCount(userId)
    }

    // --- Statistics operations ---

    suspend fun insertOrUpdateStatistics(statistics: NewStatistics) {
        userDao.insertOrUpdateStatistics(statistics)
    }

    suspend fun getStatistics(userId: String, periodType: String, periodStart: Long, periodEnd: Long): NewStatistics? {
        return userDao.getStatistics(userId, periodType, periodStart, periodEnd)
    }

    suspend fun getStatisticsByFirebaseUid(firebaseUid: String, periodType: String, periodStart: Long, periodEnd: Long): NewStatistics? {
        return userDao.getStatisticsByFirebaseUid(firebaseUid, periodType, periodStart, periodEnd)
    }

    fun getStatisticsForUser(userId: String, periodType: String): LiveData<List<NewStatistics>> {
        return userDao.getStatisticsForUser(userId, periodType)
    }

    fun getStatisticsForFirebaseUser(firebaseUid: String, periodType: String): LiveData<List<NewStatistics>> {
        return userDao.getStatisticsForFirebaseUser(firebaseUid, periodType)
    }

    suspend fun getCompletedSessionsInRange(userId: String, startDate: Long, endDate: Long): List<UserClass> {
        return userDao.getCompletedSessionsInRange(userId, startDate, endDate)
    }

    suspend fun calculateAndSaveStatistics(userId: String, firebaseUid: String?, periodType: String, startDate: Long, endDate: Long) {
        val sessions = getCompletedSessionsInRange(userId, startDate, endDate)
        val statistics = calculateStatistics(userId, firebaseUid, periodType, startDate, endDate, sessions)
        insertOrUpdateStatistics(statistics)
        Log.d("UserRepository", "Statistics calculated and saved for user: $userId, period: $periodType")
    }

    private fun calculateStatistics(
        userId: String,
        firebaseUid: String?,
        periodType: String,
        startDate: Long,
        endDate: Long,
        sessions: List<UserClass>
    ): NewStatistics {
        val completedSessions = sessions.filter { it.status == "COMPLETED" }
        val totalAttempts = sessions.size

        // Calculate statistics
        val totalFocusTime = completedSessions.sumOf { it.workDuration }
        val averageSessionTime = if (completedSessions.isNotEmpty()) totalFocusTime / completedSessions.size else 0L
        val longestSession = completedSessions.maxByOrNull { it.workDuration }?.workDuration ?: 0L
        val completionRate = if (totalAttempts > 0) (completedSessions.size.toFloat() / totalAttempts.toFloat() * 100) else 0f
        val mostProductiveDay = calculateMostProductiveDay(completedSessions)

        return NewStatistics(
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
    }

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

    // Migration: Transfer data from anonymous to Firebase user
    suspend fun migrateUserData(fromUserId: String, toFirebaseUid: String, toUserId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get all sessions from old user
                val oldSessions = userDao.getAllSessionsForUserImmediate(fromUserId)

                // 2. Update session ownership to new user
                oldSessions.forEach { session ->
                    val updatedSession = session.copy(sessionOwnerId = toUserId)
                    userDao.insertMeditationSession(updatedSession)
                }

                // 3. Get all statistics from old user
                val oldStats = userDao.getAllStatisticsForUserImmediate(fromUserId)

                // 4. Update statistics ownership to new user
                oldStats.forEach { stat ->
                    val updatedStat = stat.copy(userId = toUserId, firebaseUid = toFirebaseUid)
                    userDao.insertOrUpdateStatistics(updatedStat)
                }

                // 5. Delete old user's data (optional)
                userDao.deleteSessionsByUserId(fromUserId)
                userDao.deleteStatisticsByUserId(fromUserId)

                Log.d("UserRepository", "Data migration completed: $fromUserId -> $toUserId")
                true
            } catch (e: Exception) {
                Log.e("UserRepository", "Error migrating user data", e)
                false
            }
        }
    }

    // Enhanced method to refresh data
    fun refreshData() {
        // This forces LiveData to refresh
        // Room automatically handles this, but we can add any cleanup here
        Log.d("UserRepository", "Data refresh triggered")
    }

    // Get all statistics for user immediately
    suspend fun getAllStatisticsForUserImmediate(userId: String): List<NewStatistics> {
        return userDao.getAllStatisticsForUserImmediate(userId)
    }
}