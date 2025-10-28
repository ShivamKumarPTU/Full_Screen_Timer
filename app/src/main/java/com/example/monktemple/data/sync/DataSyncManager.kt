package com.example.monktemple.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.monktemple.data.remote.FirebaseRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class DataSyncManager @Inject constructor(
    private val context: Context,
    private val userDataManager: UserDataManager,
    private val remoteDataSource: FirebaseRemoteDataSource
){

    companion object {
        private const val TAG = "DataSyncManager"
        private const val SYNC_WORKER_TAG = "firestore_sync_worker"
    }

    // Enhanced sync scheduling with Firestore
    fun schedulePeriodicDataSync(userId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false) // Sync even when not charging
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<FirestoreSyncWorker>(
            2, TimeUnit.HOURS, // Sync every 2 hours for better real-time experience
            15, TimeUnit.MINUTES // Flexible interval
        ).setConstraints(constraints)
            .addTag("$SYNC_WORKER_TAG:$userId")
            .setInputData(workDataOf("userId" to userId))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "$SYNC_WORKER_TAG:$userId",
            ExistingPeriodicWorkPolicy.REPLACE, // Replace existing to avoid duplicates
            syncRequest
        )

        Log.d(TAG, "🔄 Periodic Firestore sync scheduled for user: $userId")
    }

    // Real-time sync trigger
    fun triggerImmediateSync(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "⚡ Triggering immediate Firestore sync for user: $userId")

                // Update sync status
                remoteDataSource.updateSyncStatus(userId, "PENDING")

                // Perform sync
                userDataManager.syncUserDataWithFirestore(userId)

                Log.d(TAG, "✅ Immediate Firestore sync completed for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Immediate Firestore sync failed", e)
                remoteDataSource.updateSyncStatus(userId, "FAILED")
            }
        }
    }

    // Cancel sync when user logs out
    fun cancelDataSync(userId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("$SYNC_WORKER_TAG:$userId")
        Log.d(TAG, "🛑 Firestore data sync cancelled for user: $userId")
    }

    // Force immediate sync via WorkManager (for background)
    fun syncDataImmediately(userId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf("userId" to userId, "immediate" to true))
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
        Log.d(TAG, "🚀 Immediate Firestore sync triggered via WorkManager for user: $userId")
    }

    // Get sync status
    suspend fun getSyncStatus(userId: String): String {
        return try {
            val statusResult = remoteDataSource.getSyncStatus(userId)
            if (statusResult.isSuccess) {
                statusResult.getOrNull()?.lastSyncStatus ?: "UNKNOWN"
            } else {
                "ERROR"
            }
        } catch (e: Exception) {
            "ERROR"
        }
    }
}

class FirestoreSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        val isImmediate = inputData.getBoolean("immediate", false)

        return try {
            Log.d("FirestoreSyncWorker", "🔄 Starting Firestore sync for user: $userId")

            // In a real implementation, you'd inject these
            val remoteDataSource = FirebaseRemoteDataSource()

            // Update sync status to pending
            remoteDataSource.updateSyncStatus(userId, "SYNC_IN_PROGRESS")

            // Simulate sync work (in real app, this would be actual sync logic)
            if (isImmediate) {
                // More comprehensive sync for immediate requests
                performComprehensiveSync(userId)
            } else {
                // Light sync for periodic background sync
                performLightSync(userId)
            }

            // Update sync status to success
            remoteDataSource.updateSyncStatus(userId, "SUCCESS")

            Log.d("FirestoreSyncWorker", "✅ Firestore sync completed successfully for user: $userId")
            Result.success()

        } catch (e: Exception) {
            Log.e("FirestoreSyncWorker", "❌ Firestore sync failed", e)

            // Update sync status to failed
            val remoteDataSource = FirebaseRemoteDataSource()
            remoteDataSource.updateSyncStatus(userId, "FAILED")

            Result.retry()
        }
    }

    private suspend fun performComprehensiveSync(userId: String) {
        // Comprehensive sync logic
        // This would include:
        // 1. Push all local changes to Firestore
        // 2. Pull all remote changes to local
        // 3. Resolve conflicts
        // 4. Update statistics

        kotlinx.coroutines.delay(5000) // Simulate work
    }

    private suspend fun performLightSync(userId: String) {
        // Light sync for background - only essential data
        // This would include:
        // 1. Push recent local changes
        // 2. Pull critical remote changes

        kotlinx.coroutines.delay(2000) // Simulate work
    }
}