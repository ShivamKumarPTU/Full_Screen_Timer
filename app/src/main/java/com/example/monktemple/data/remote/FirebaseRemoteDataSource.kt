package com.example.monktemple.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteDataSource @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_SESSIONS = "sessions"
        private const val COLLECTION_STATISTICS = "statistics"
        private const val COLLECTION_SYNC_STATUS = "sync_status"
    }

    // User Operations
    suspend fun saveUserToFirestore(user: FirestoreUser): Result<Boolean> {
        return try {
            db.collection(COLLECTION_USERS)
                .document(user.userId)
                .set(user, SetOptions.merge())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFromFirestore(userId: String): Result<FirestoreUser?> {
        return try {
            val document = db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val user = document.toObject<FirestoreUser>()
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserLastSync(userId: String): Result<Boolean> {
        return try {
            val updateData = mapOf(
                "last_sync" to Timestamp.now(),
                "last_login" to Timestamp.now()
            )
            db.collection(COLLECTION_USERS)
                .document(userId)
                .update(updateData)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Session Operations
    suspend fun saveSessionToFirestore(session: FirestoreSession): Result<Boolean> {
        return try {
            // Use sessionId as document ID for easy updates
            val documentId = if (session.sessionId.isNotEmpty()) {
                session.sessionId
            } else {
                // Generate a unique ID for new sessions
                "session_${System.currentTimeMillis()}"
            }

            val sessionWithId = session.copy(sessionId = documentId)

            db.collection(COLLECTION_SESSIONS)
                .document(documentId)
                .set(sessionWithId, SetOptions.merge())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSessionsFromFirestore(userId: String): Result<List<FirestoreSession>> {
        return try {
            val querySnapshot = db.collection(COLLECTION_SESSIONS)
                .whereEqualTo("user_id", userId)
                .orderBy("completion_timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val sessions = querySnapshot.documents.mapNotNull { document ->
                document.toObject<FirestoreSession>()
            }
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessionsInRange(userId: String, startTime: Long, endTime: Long): Result<List<FirestoreSession>> {
        return try {
            val startTimestamp = Timestamp(startTime / 1000, ((startTime % 1000) * 1000000).toInt())
            val endTimestamp = Timestamp(endTime / 1000, ((endTime % 1000) * 1000000).toInt())

            val querySnapshot = db.collection(COLLECTION_SESSIONS)
                .whereEqualTo("user_id", userId)
                .whereGreaterThanOrEqualTo("completion_timestamp", startTimestamp)
                .whereLessThanOrEqualTo("completion_timestamp", endTimestamp)
                .get()
                .await()

            val sessions = querySnapshot.documents.mapNotNull { document ->
                document.toObject<FirestoreSession>()
            }
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Statistics Operations
    suspend fun saveStatisticsToFirestore(statistics: FirestoreStatistics): Result<Boolean> {
        return try {
            val documentId = if (statistics.statId.isNotEmpty()) {
                statistics.statId
            } else {
                "stat_${statistics.userId}_${statistics.periodType}_${statistics.periodStart.seconds}"
            }

            val statsWithId = statistics.copy(statId = documentId)

            db.collection(COLLECTION_STATISTICS)
                .document(documentId)
                .set(statsWithId, SetOptions.merge())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserStatisticsFromFirestore(userId: String): Result<List<FirestoreStatistics>> {
        return try {
            val querySnapshot = db.collection(COLLECTION_STATISTICS)
                .whereEqualTo("user_id", userId)
                .orderBy("period_start", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val stats = querySnapshot.documents.mapNotNull { document ->
                document.toObject<FirestoreStatistics>()
            }
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sync Status Operations
    suspend fun updateSyncStatus(userId: String, status: String, pendingSyncs: Int = 0): Result<Boolean> {
        return try {
            val syncStatus = SyncStatus(
                userId = userId,
                lastSyncTime = Timestamp.now(),
                lastSyncStatus = status,
                pendingSyncs = pendingSyncs
            )

            db.collection(COLLECTION_SYNC_STATUS)
                .document(userId)
                .set(syncStatus, SetOptions.merge())
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSyncStatus(userId: String): Result<SyncStatus?> {
        return try {
            val document = db.collection(COLLECTION_SYNC_STATUS)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val status = document.toObject<SyncStatus>()
                Result.success(status)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Batch Operations for better performance
    suspend fun saveMultipleSessions(sessions: List<FirestoreSession>): Result<Boolean> {
        return try {
            val batch = db.batch()

            sessions.forEach { session ->
                val documentId = if (session.sessionId.isNotEmpty()) {
                    session.sessionId
                } else {
                    "session_${System.currentTimeMillis()}_${session.hashCode()}"
                }

                val sessionWithId = session.copy(sessionId = documentId)
                val docRef = db.collection(COLLECTION_SESSIONS).document(documentId)
                batch.set(docRef, sessionWithId, SetOptions.merge())
            }

            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete operations (for cleanup)
    suspend fun deleteUserData(userId: String): Result<Boolean> {
        return try {
            // Note: In production, you might want to soft delete instead
            val batch = db.batch()

            // Delete user sessions
            val sessionsQuery = db.collection(COLLECTION_SESSIONS)
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            sessionsQuery.documents.forEach { document ->
                batch.delete(document.reference)
            }

            // Delete user statistics
            val statsQuery = db.collection(COLLECTION_STATISTICS)
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            statsQuery.documents.forEach { document ->
                batch.delete(document.reference)
            }

            // Delete sync status
            batch.delete(db.collection(COLLECTION_SYNC_STATUS).document(userId))

            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}