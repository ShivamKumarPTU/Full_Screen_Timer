package com.example.monktemple.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.database.PropertyName


data class FirestoreUser(
    @PropertyName("user_id") val userId: String = "",
    @PropertyName("firebase_uid") val firebaseUid: String = "",
    @PropertyName("display_name") val displayName: String? = "",
    @PropertyName("email") val email: String? = "",
    @PropertyName("photo_url") val photoUrl: String? = null,
    @PropertyName("created_at") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("last_login") val lastLogin: Timestamp = Timestamp.now(),
    @PropertyName("last_sync") val lastSync: Timestamp = Timestamp.now(),
    @PropertyName("data_version") val dataVersion: Int = 1
)

data class FirestoreSession(
    @PropertyName("session_id") val sessionId: String = "",
    @PropertyName("user_id") val userId: String = "",
    @PropertyName("completion_timestamp") val completionTimestamp: Timestamp = Timestamp.now(),
    @PropertyName("work_duration") val workDuration: Long = 0,
    @PropertyName("status") val status: String = "",
    @PropertyName("goal_name") val goalName: String = "",
    @PropertyName("created_at") val createdAt: Timestamp = Timestamp.now(),
    @PropertyName("last_updated") val lastUpdated: Timestamp = Timestamp.now(),
    @PropertyName("sync_status") val syncStatus: String = "SYNCED" // SYNCED, PENDING, FAILED
)

data class FirestoreStatistics(
    @PropertyName("stat_id") val statId: String = "",
    @PropertyName("user_id") val userId: String = "",
    @PropertyName("period_type") val periodType: String = "",
    @PropertyName("period_start") val periodStart: Timestamp = Timestamp.now(),
    @PropertyName("period_end") val periodEnd: Timestamp = Timestamp.now(),
    @PropertyName("no_of_sessions") val noOfSessions: Int = 0,
    @PropertyName("focus_time") val focusTime: Long = 0,
    @PropertyName("average_session_time") val averageSessionTime: Long = 0,
    @PropertyName("longest_session") val longestSession: Long = 0,
    @PropertyName("completion_rate") val completionRate: Float = 0f,
    @PropertyName("most_productive_day") val mostProductiveDay: String = "",
    @PropertyName("last_updated") val lastUpdated: Timestamp = Timestamp.now()
)

data class SyncStatus(
    @PropertyName("user_id") val userId: String = "",
    @PropertyName("last_sync_time") val lastSyncTime: Timestamp = Timestamp.now(),
    @PropertyName("pending_syncs") val pendingSyncs: Int = 0,
    @PropertyName("last_sync_status") val lastSyncStatus: String = "SUCCESS", // SUCCESS, FAILED, PENDING
    @PropertyName("sync_version") val syncVersion: Int = 1
)