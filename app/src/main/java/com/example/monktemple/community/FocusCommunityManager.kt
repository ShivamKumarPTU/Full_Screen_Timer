package com.example.monktemple.community

import android.content.Context
import android.util.Log
import com.example.monktemple.data.remote.FirebaseRemoteDataSource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusCommunityManager @Inject constructor(
    private val context: Context,
    private val remoteDataSource: FirebaseRemoteDataSource
) {

    companion object {
        private const val TAG = "FocusCommunityManager"
        private const val COLLECTION_COMMUNITIES = "focus_communities"
        private const val COLLECTION_GROUP_SESSIONS = "group_sessions"
        private const val COLLECTION_LEADERBOARDS = "leaderboards"
        private const val COLLECTION_INVITES = "community_invites"
    }

    data class FocusCommunity(
        val communityId: String = "",
        val name: String = "",
        val description: String = "",
        val memberCount: Int = 0,
        val focusGoal: String = "",
        val createdBy: String = "",
        val createdAt: Date = Date(),
        val isPublic: Boolean = true,
        val tags: List<String> = emptyList(),
        val rules: List<String> = emptyList(),
        val members: List<String> = emptyList() // ADDED THIS FIELD
    )

    data class GroupSession(
        val sessionId: String = "",
        val communityId: String = "",
        val hostId: String = "",
        val sessionName: String = "",
        val startTime: Date = Date(),
        val duration: Long = 0,
        val participants: List<String> = emptyList(),
        val maxParticipants: Int = 10,
        val status: String = "scheduled", // scheduled, active, completed, cancelled
        val focusTopic: String = ""
    )

    data class LeaderboardEntry(
        val userId: String = "",
        val userName: String = "",
        val totalFocusTime: Long = 0,
        val sessionsCompleted: Int = 0,
        val streak: Int = 0,
        val rank: Int = 0,
        val lastActive: Date = Date()
    )

    data class CommunityInvite(
        val inviteId: String = "",
        val communityId: String = "",
        val inviterId: String = "",
        val inviteeEmail: String = "",
        val status: String = "pending", // pending, accepted, declined
        val createdAt: Date = Date()
    )

    suspend fun createCommunity(community: FocusCommunity): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val communityWithId = community.copy(
                    communityId = "community_${System.currentTimeMillis()}_${UUID.randomUUID()}"
                )

                // Use the Firestore instance from remoteDataSource
                val db = remoteDataSource.getFirestore()
                db.collection(COLLECTION_COMMUNITIES)
                    .document(communityWithId.communityId)
                    .set(communityWithId)
                    .await()

                Log.d(TAG, "✅ Community created: ${communityWithId.communityId}")
                Result.success(communityWithId.communityId)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating community", e)
                Result.failure(e)
            }
        }
    }

    suspend fun joinCommunity(communityId: String, userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val db = remoteDataSource.getFirestore()

                // Add user to community members
                db.collection(COLLECTION_COMMUNITIES)
                    .document(communityId)
                    .update(
                        "members", FieldValue.arrayUnion(userId),
                        "memberCount", FieldValue.increment(1)
                    )
                    .await()

                Log.d(TAG, "✅ User $userId joined community $communityId")
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error joining community", e)
                Result.failure(e)
            }
        }
    }

    suspend fun leaveCommunity(communityId: String, userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val db = remoteDataSource.getFirestore()

                db.collection(COLLECTION_COMMUNITIES)
                    .document(communityId)
                    .update(
                        "members", FieldValue.arrayRemove(userId),
                        "memberCount", FieldValue.increment(-1)
                    )
                    .await()

                Log.d(TAG, "✅ User $userId left community $communityId")
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error leaving community", e)
                Result.failure(e)
            }
        }
    }

    suspend fun startGroupSession(session: GroupSession): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionWithId = session.copy(
                    sessionId = "session_${System.currentTimeMillis()}_${UUID.randomUUID()}"
                )

                val db = remoteDataSource.getFirestore()
                db.collection(COLLECTION_GROUP_SESSIONS)
                    .document(sessionWithId.sessionId)
                    .set(sessionWithId)
                    .await()

                Log.d(TAG, "✅ Group session started: ${sessionWithId.sessionId}")
                Result.success(sessionWithId.sessionId)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error starting group session", e)
                Result.failure(e)
            }
        }
    }

    suspend fun joinGroupSession(sessionId: String, userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val db = remoteDataSource.getFirestore()

                db.collection(COLLECTION_GROUP_SESSIONS)
                    .document(sessionId)
                    .update("participants", FieldValue.arrayUnion(userId))
                    .await()

                Log.d(TAG, "✅ User $userId joined group session $sessionId")
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error joining group session", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getCommunityLeaderboard(communityId: String): Result<List<LeaderboardEntry>> {
        return withContext(Dispatchers.IO) {
            try {
                val db = remoteDataSource.getFirestore()

                val snapshot = db.collection(COLLECTION_LEADERBOARDS)
                    .whereEqualTo("communityId", communityId)
                    .orderBy("totalFocusTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(20)
                    .get()
                    .await()

                val leaderboard = snapshot.documents.mapNotNull { document ->
                    try {
                        LeaderboardEntry(
                            userId = document.getString("userId") ?: "",
                            userName = document.getString("userName") ?: "Anonymous",
                            totalFocusTime = document.getLong("totalFocusTime") ?: 0,
                            sessionsCompleted = document.getLong("sessionsCompleted")?.toInt() ?: 0,
                            streak = document.getLong("streak")?.toInt() ?: 0,
                            rank = document.getLong("rank")?.toInt() ?: 0,
                            lastActive = document.getDate("lastActive") ?: Date()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                // Add ranks
                val rankedLeaderboard = leaderboard.mapIndexed { index, entry ->
                    entry.copy(rank = index + 1)
                }

                Result.success(rankedLeaderboard)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting leaderboard", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getPublicCommunities(): Result<List<FocusCommunity>> {
        return withContext(Dispatchers.IO) {
            try {
                val db = remoteDataSource.getFirestore()

                val snapshot = db.collection(COLLECTION_COMMUNITIES)
                    .whereEqualTo("isPublic", true)
                    .orderBy("memberCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val communities = snapshot.documents.mapNotNull { document ->
                    try {
                        FocusCommunity(
                            communityId = document.id,
                            name = document.getString("name") ?: "",
                            description = document.getString("description") ?: "",
                            memberCount = document.getLong("memberCount")?.toInt() ?: 0,
                            focusGoal = document.getString("focusGoal") ?: "",
                            createdBy = document.getString("createdBy") ?: "",
                            createdAt = document.getDate("createdAt") ?: Date(),
                            isPublic = document.getBoolean("isPublic") ?: true,
                            tags = document.get("tags") as? List<String> ?: emptyList(),
                            members = document.get("members") as? List<String> ?: emptyList()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                Result.success(communities)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting public communities", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getUserCommunities(userId: String): Result<List<FocusCommunity>> {
        return withContext(Dispatchers.IO) {
            try {
                val db = remoteDataSource.getFirestore()

                val snapshot = db.collection(COLLECTION_COMMUNITIES)
                    .whereArrayContains("members", userId)
                    .get()
                    .await()

                val communities = snapshot.documents.mapNotNull { document ->
                    try {
                        FocusCommunity(
                            communityId = document.id,
                            name = document.getString("name") ?: "",
                            description = document.getString("description") ?: "",
                            memberCount = document.getLong("memberCount")?.toInt() ?: 0,
                            focusGoal = document.getString("focusGoal") ?: "",
                            createdBy = document.getString("createdBy") ?: "",
                            createdAt = document.getDate("createdAt") ?: Date(),
                            isPublic = document.getBoolean("isPublic") ?: true,
                            members = document.get("members") as? List<String> ?: emptyList()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                Result.success(communities)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting user communities", e)
                Result.failure(e)
            }
        }
    }

    suspend fun inviteToCommunity(invite: CommunityInvite): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val inviteWithId = invite.copy(
                    inviteId = "invite_${System.currentTimeMillis()}_${UUID.randomUUID()}"
                )

                val db = remoteDataSource.getFirestore()
                db.collection(COLLECTION_INVITES)
                    .document(inviteWithId.inviteId)
                    .set(inviteWithId)
                    .await()

                Log.d(TAG, "✅ Invite sent to ${invite.inviteeEmail} for community ${invite.communityId}")
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending invite", e)
                Result.failure(e)
            }
        }
    }

    suspend fun updateLeaderboard(communityId: String, userId: String, focusTime: Long, sessionCompleted: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val db = remoteDataSource.getFirestore()
                val leaderboardRef = db.collection(COLLECTION_LEADERBOARDS)
                    .document("${communityId}_$userId")

                val updateData = hashMapOf<String, Any>(
                    "userId" to userId,
                    "communityId" to communityId,
                    "totalFocusTime" to FieldValue.increment(focusTime),
                    "lastActive" to Timestamp.now()
                )

                if (sessionCompleted) {
                    updateData["sessionsCompleted"] = FieldValue.increment(1)
                    // Streak logic would be more complex in production
                    updateData["streak"] = FieldValue.increment(1)
                }

                leaderboardRef.set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()

                Log.d(TAG, "📊 Leaderboard updated for user $userId in community $communityId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating leaderboard", e)
            }
        }
    }

    // ADDED: Get community by ID
    suspend fun getCommunityById(communityId: String): Result<FocusCommunity?> {
        return withContext(Dispatchers.IO) {
            try {
                val db = remoteDataSource.getFirestore()
                val document = db.collection(COLLECTION_COMMUNITIES)
                    .document(communityId)
                    .get()
                    .await()

                if (document.exists()) {
                    val community = FocusCommunity(
                        communityId = document.id,
                        name = document.getString("name") ?: "",
                        description = document.getString("description") ?: "",
                        memberCount = document.getLong("memberCount")?.toInt() ?: 0,
                        focusGoal = document.getString("focusGoal") ?: "",
                        createdBy = document.getString("createdBy") ?: "",
                        createdAt = document.getDate("createdAt") ?: Date(),
                        isPublic = document.getBoolean("isPublic") ?: true,
                        tags = document.get("tags") as? List<String> ?: emptyList(),
                        members = document.get("members") as? List<String> ?: emptyList()
                    )
                    Result.success(community)
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting community by ID", e)
                Result.failure(e)
            }
        }
    }

    // ADDED: Get active group sessions
    suspend fun getActiveGroupSessions(communityId: String): Result<List<GroupSession>> {
        return withContext(Dispatchers.IO) {
            try {
                val db = remoteDataSource.getFirestore()
                val snapshot = db.collection(COLLECTION_GROUP_SESSIONS)
                    .whereEqualTo("communityId", communityId)
                    .whereEqualTo("status", "active")
                    .get()
                    .await()

                val sessions = snapshot.documents.mapNotNull { document ->
                    try {
                        GroupSession(
                            sessionId = document.id,
                            communityId = document.getString("communityId") ?: "",
                            hostId = document.getString("hostId") ?: "",
                            sessionName = document.getString("sessionName") ?: "",
                            startTime = document.getDate("startTime") ?: Date(),
                            duration = document.getLong("duration") ?: 0,
                            participants = document.get("participants") as? List<String> ?: emptyList(),
                            maxParticipants = document.getLong("maxParticipants")?.toInt() ?: 10,
                            status = document.getString("status") ?: "scheduled",
                            focusTopic = document.getString("focusTopic") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                Result.success(sessions)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting active group sessions", e)
                Result.failure(e)
            }
        }
    }
}