package com.example.monktemple.RoomUser

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    // --- User Methods ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: User)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String?): User?

    @Query("SELECT * FROM users WHERE firebaseUid = :firebaseUid")
    suspend fun getUserByFirebaseUid(firebaseUid: String): User?

    @Query("SELECT * FROM users")
    fun getAllUsers(): LiveData<List<User>>

    // --- Meditation Session Methods ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeditationSession(session: UserClass)

    // --- Query Methods for Statistics ---

    @Query("SELECT * FROM focus_sessions WHERE sessionOwnerId = :userId AND completionTimestamp BETWEEN :startDate AND :endDate ORDER BY completionTimestamp DESC")
    fun getSessionsForUserInRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): LiveData<List<UserClass>>

    @Query("SELECT * FROM focus_sessions WHERE sessionOwnerId = :userId")
    fun getAllSessionsForUser(userId: String): LiveData<List<UserClass>>

    // Add method to get sessions for statistics without LiveData for immediate use
    @Query("SELECT * FROM focus_sessions WHERE sessionOwnerId = :userId AND completionTimestamp BETWEEN :startDate AND :endDate")
    suspend fun getSessionsForUserInRangeImmediate(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<UserClass>

    // Get all sessions for a user immediately (no LiveData)
    @Query("SELECT * FROM focus_sessions WHERE sessionOwnerId = :userId")
    suspend fun getAllSessionsForUserImmediate(userId: String): List<UserClass>

    // Get session count for debugging
    @Query("SELECT COUNT(*) FROM focus_sessions WHERE sessionOwnerId = :userId")
    suspend fun getSessionCount(userId: String): Int

    // Delete sessions by user ID (for migration cleanup)
    @Query("DELETE FROM focus_sessions WHERE sessionOwnerId = :userId")
    suspend fun deleteSessionsByUserId(userId: String)

    // Get completed sessions for statistics calculation
    @Query("SELECT * FROM focus_sessions WHERE sessionOwnerId = :userId AND status = 'COMPLETED' AND completionTimestamp BETWEEN :startDate AND :endDate")
    suspend fun getCompletedSessionsInRange(userId: String, startDate: Long, endDate: Long): List<UserClass>

    // --- Statistics Methods ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStatistics(statistics: NewStatistics)

    @Query("SELECT * FROM user_statistics WHERE userId = :userId AND periodType = :periodType AND periodStart = :periodStart AND periodEnd = :periodEnd")
    suspend fun getStatistics(userId: String, periodType: String, periodStart: Long, periodEnd: Long): NewStatistics?

    @Query("SELECT * FROM user_statistics WHERE firebaseUid = :firebaseUid AND periodType = :periodType AND periodStart = :periodStart AND periodEnd = :periodEnd")
    suspend fun getStatisticsByFirebaseUid(firebaseUid: String, periodType: String, periodStart: Long, periodEnd: Long): NewStatistics?

    @Query("SELECT * FROM user_statistics WHERE userId = :userId AND periodType = :periodType ORDER BY periodStart DESC")
    fun getStatisticsForUser(userId: String, periodType: String): LiveData<List<NewStatistics>>

    @Query("SELECT * FROM user_statistics WHERE firebaseUid = :firebaseUid AND periodType = :periodType ORDER BY periodStart DESC")
    fun getStatisticsForFirebaseUser(firebaseUid: String, periodType: String): LiveData<List<NewStatistics>>

    @Query("SELECT * FROM user_statistics WHERE userId = :userId")
    suspend fun getAllStatisticsForUserImmediate(userId: String): List<NewStatistics>

    @Query("DELETE FROM user_statistics WHERE userId = :userId")
    suspend fun deleteStatisticsByUserId(userId: String)

    @Query("DELETE FROM user_statistics WHERE userId = :userId AND periodType = :periodType AND periodStart = :periodStart AND periodEnd = :periodEnd")
    suspend fun deleteStatistics(userId: String, periodType: String, periodStart: Long, periodEnd: Long)
}