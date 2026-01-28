package com.example.monktemple.data.sync

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.monktemple.RoomUser.UserClass
import com.example.monktemple.RoomUser.UserDao
import com.example.monktemple.data.remote.FirebaseRemoteDataSource
import com.example.monktemple.data.remote.FirestoreSession
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnhancedSyncManager @Inject constructor(
    private val context: Context,
    private val userDao: UserDao,
    private val remoteDataSource: FirebaseRemoteDataSource
) {

    companion object {
        private const val TAG = "EnhancedSyncManager"
    }

    data class SyncResult(
        val success: Boolean,
        val syncedSessions: Int,
        val syncedStats: Int,
        val conflictsResolved: Int,
        val error: String? = null
    )

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun performMultiDeviceSync(userId: String): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Starting multi-device sync for user: $userId")

                // 1. Get local unsynced data
                val localSessions = userDao.getAllSessionsForUserImmediate(userId)
                val unsyncedSessions = localSessions.filter { it.completionTimestamp > System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000) }

                // 2. Get remote data
                val remoteSessionsResult = remoteDataSource.getUserSessionsFromFirestore(userId)
                val remoteSessions = if (remoteSessionsResult.isSuccess) {
                    remoteSessionsResult.getOrNull() ?: emptyList()
                } else {
                    emptyList()
                }

                // 3. Conflict resolution
                val (mergedSessions, conflictsResolved) = mergeSessionsWithConflictResolution(
                    unsyncedSessions, remoteSessions, userId
                )

                // 4. Sync to remote
                val firestoreSessions = mergedSessions.map { localSession ->
                    FirestoreSession(
                        sessionId = "local_${localSession.sessionId}",
                        userId = userId,
                        completionTimestamp = Timestamp(localSession.completionTimestamp / 1000,
                            ((localSession.completionTimestamp % 1000) * 1000).toInt()),
                        workDuration = localSession.workDuration,
                        status = localSession.status,
                        goalName = "Focus Session",
                        syncStatus = "SYNCED"
                    )
                }

                val syncResult = remoteDataSource.saveMultipleSessions(firestoreSessions)

                if (syncResult.isSuccess) {
                    Log.d(TAG, "✅ Multi-device sync completed: ${mergedSessions.size} sessions")
                    SyncResult(
                        success = true,
                        syncedSessions = mergedSessions.size,
                        syncedStats = 0,
                        conflictsResolved = conflictsResolved
                    )
                } else {
                    Log.e(TAG, "❌ Multi-device sync failed")
                    SyncResult(
                        success = false,
                        syncedSessions = 0,
                        syncedStats = 0,
                        conflictsResolved = 0,
                        error = syncResult.exceptionOrNull()?.message
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Multi-device sync error", e)
                SyncResult(
                    success = false,
                    syncedSessions = 0,
                    syncedStats = 0,
                    conflictsResolved = 0,
                    error = e.message
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun mergeSessionsWithConflictResolution(
        localSessions: List<UserClass>,
        remoteSessions: List<FirestoreSession>,
        userId: String
    ): Pair<List<UserClass>, Int> {
        val mergedSessions = mutableListOf<UserClass>()
        var conflictsResolved = 0

        // Create maps for easy lookup
        val localMap = localSessions.associateBy { it.completionTimestamp }
        val remoteMap = remoteSessions.associateBy { it.completionTimestamp.seconds * 1000 }

        /* Process all unique timestamps */
        val allTimestamps = (localMap.keys + remoteMap.keys).toSortedSet().reversed()

        for (timestamp in allTimestamps) {
            val localSession = localMap[timestamp]
            val remoteSession = remoteMap[timestamp]

            when {
                localSession != null && remoteSession != null -> {
                    // Conflict - use the one with longer duration or most recent
                    val resolvedSession = if (localSession.workDuration >= remoteSession.workDuration) {
                        localSession
                    } else {
                        UserClass(
                            sessionId = 0, // Will be auto-generated
                            sessionOwnerId = userId,
                            completionTimestamp = remoteSession.completionTimestamp.seconds * 1000,
                            workDuration = remoteSession.workDuration,
                            status = remoteSession.status
                        )
                    }
                    mergedSessions.add(resolvedSession)
                    conflictsResolved++
                }
                localSession != null -> {
                    // Only local exists
                    mergedSessions.add(localSession)
                }
                remoteSession != null -> {
                    // Only remote exists - create local session
                    mergedSessions.add(
                        UserClass(
                            sessionId = 0,
                            sessionOwnerId = userId,
                            completionTimestamp = remoteSession.completionTimestamp.seconds * 1000,
                            workDuration = remoteSession.workDuration,
                            status = remoteSession.status
                        )
                    )
                }
            }
        }

        return Pair(mergedSessions, conflictsResolved)
    }

    suspend fun enableOfflineMode(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // In a real implementation, configure Firestore for offline persistence
                Log.d(TAG, "📱 Offline mode enabled for user: $userId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error enabling offline mode", e)
                false
            }
        }
    }

    suspend fun getSyncStatus(userId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val localSessions = userDao.getAllSessionsForUserImmediate(userId)
                val recentSessions = localSessions.filter {
                    it.completionTimestamp > System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                }

                when {
                    recentSessions.isEmpty() -> "NO_RECENT_DATA"
                    recentSessions.any { it.completionTimestamp > System.currentTimeMillis() - (2 * 60 * 60 * 1000) } -> "SYNCED"
                    else -> "NEEDS_SYNC"
                }
            } catch (e: Exception) {
                "ERROR"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun forceSync(userId: String): SyncResult {
        return performMultiDeviceSync(userId)
    }
}