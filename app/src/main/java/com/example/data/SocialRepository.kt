package com.example.data

import android.content.Context
import com.example.data.supabase.SupabaseClient
import com.example.util.PushNotificationManager
import io.github.jan-tennert.supabase.postgrest.from
import io.github.jan-tennert.supabase.postgrest.postgrest
import io.github.jan-tennert.supabase.postgrest.filter.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName

// --------------- Backward-compat extension ---------------

/** Allows UI code that accesses .profilePhotoUri to compile against SupabaseProfile. */
val SupabaseProfile.profilePhotoUri: String? get() = profile_image

// --------------- Supabase notification model ---------------

@kotlinx.serialization.Serializable
data class SupabaseNotification(
    val id: String = "",
    val user_id: String = "",
    val type: String = "",
    val related_user_id: String? = null,
    val related_request_id: String? = null,
    @SerialName("is_read")
    val isRead: Boolean = false,
    val created_at: String = "",
    // ---- Populated after fetch for backward compat with UI ----
    val senderUsername: String = "",
    val message: String = "",
    val status: String = ""
) {
    /** Epoch millis derived from ISO-8601 created_at. */
    val timestamp: Long
        get() = try {
            java.time.Instant.parse(created_at).toEpochMilli()
        } catch (_: Exception) { 0L }
}

// --------------- Internal Supabase row types ---------------

@kotlinx.serialization.Serializable
private data class FriendshipRow(
    val id: String = "",
    val user_id: String = "",
    val friend_id: String = "",
    val created_at: String = ""
)

@kotlinx.serialization.Serializable
private data class FriendRequestRow(
    val id: String = "",
    val sender_id: String = "",
    val receiver_id: String = "",
    val status: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)

@kotlinx.serialization.Serializable
private data class BlockedUserRow(
    val id: String = "",
    val blocker_id: String = "",
    val blocked_id: String = "",
    val created_at: String = ""
)

/** Minimal model for inserting a block — only non-default columns. */
@kotlinx.serialization.Serializable
private data class BlockedUserInsert(
    val blocker_id: String,
    val blocked_id: String
)

// --------------- Public types (API surface) ---------------

enum class UserRelationshipStatus {
    SELF,
    FRIENDS,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    BLOCKED_BY_ME,
    BLOCKED_BY_THEM,
    NOT_FRIENDS
}

data class FriendProfile(
    val user: SupabaseProfile,
    val isOnline: Boolean = true,
    val sinceTime: Long = System.currentTimeMillis()
)

data class UserSearchResult(
    val user: SupabaseProfile,
    val relationshipStatus: UserRelationshipStatus
)

data class NotificationItemWithUser(
    val notification: SupabaseNotification,
    val senderUser: SupabaseProfile?
)

// --------------- Repository ---------------

class SocialRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val postgrest get() = SupabaseClient.postgrest

    // Simple in-memory caches for username ↔ UUID resolution
    private val uuidByUsernameCache = mutableMapOf<String, String>()
    private val usernameByUuidCache = mutableMapOf<String, String>()

    companion object {
        @Volatile
        private var INSTANCE: SocialRepository? = null

        fun getInstance(context: Context): SocialRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SocialRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ===================== HELPERS =====================

    private suspend fun resolveUuid(username: String): String? = withContext(Dispatchers.IO) {
        val key = username.trim().lowercase()
        uuidByUsernameCache[key]?.let { return@withContext it }
        try {
            val profile = postgrest.from("profiles")
                .select { filter { eq("normalized_username", key) } }
                .decodeSingleOrNull<SupabaseProfile>()
            profile?.let {
                uuidByUsernameCache[key] = it.id
                usernameByUuidCache[it.id] = it.username
            }
            profile?.id
        } catch (_: Exception) { null }
    }

    private suspend fun resolveUsername(uuid: String): String? = withContext(Dispatchers.IO) {
        usernameByUuidCache[uuid]?.let { return@withContext it }
        try {
            val profile = postgrest.from("profiles")
                .select { filter { eq("id", uuid) } }
                .decodeSingleOrNull<SupabaseProfile>()
            profile?.let {
                uuidByUsernameCache[it.username] = it.id
                usernameByUuidCache[it.id] = it.username
            }
            profile?.username
        } catch (_: Exception) { null }
    }

    /** Batch-fetch profiles for a list of UUIDs. Falls back to individual fetches. */
    private suspend fun fetchProfiles(uuids: List<String>): List<SupabaseProfile> =
        withContext(Dispatchers.IO) {
            if (uuids.isEmpty()) return@withContext emptyList()
            val distinct = uuids.distinct()
            try {
                postgrest.from("profiles").select {
                    filter { `in`("id", distinct) }
                }.decodeList<SupabaseProfile>()
            } catch (_: Exception) {
                // Fallback: fetch one-by-one
                distinct.mapNotNull { uuid ->
                    try {
                        postgrest.from("profiles").select { filter { eq("id", uuid) } }
                            .decodeSingleOrNull<SupabaseProfile>()
                    } catch (_: Exception) { null }
                }
            }
        }

    /** Enrich a raw SupabaseNotification with derived senderUsername / message / status. */
    private fun SupabaseNotification.enrich(senderProfile: SupabaseProfile?): SupabaseNotification {
        val senderUsername = senderProfile?.username ?: ""
        val derivedStatus = when (type) {
            "FRIEND_REQUEST" -> "PENDING"
            "REQUEST_ACCEPTED" -> "ACCEPTED"
            "REQUEST_REJECTED" -> "REJECTED"
            else -> ""
        }
        val derivedMessage = when (type) {
            "FRIEND_REQUEST" -> "@$senderUsername sent you a friend request."
            "REQUEST_ACCEPTED" -> "@$senderUsername accepted your friend request. You can now chat securely."
            "REQUEST_REJECTED" -> "@$senderUsername rejected your friend request."
            else -> ""
        }
        return copy(
            senderUsername = senderUsername,
            message = derivedMessage,
            status = derivedStatus
        )
    }

    // ===================== FRIENDS LIST =====================

    fun getFriendsListFlow(currentUsername: String): Flow<List<SupabaseProfile>> = flow {
        try {
            val myUuid = resolveUuid(currentUsername) ?: return@flow emit(emptyList())
            // Friendships where I am user_id
            val f1 = postgrest.from("friendships").select {
                filter { eq("user_id", myUuid) }
            }.decodeList<FriendshipRow>()
            // Friendships where I am friend_id
            val f2 = postgrest.from("friendships").select {
                filter { eq("friend_id", myUuid) }
            }.decodeList<FriendshipRow>()
            val friendIds = (f1.map { it.friend_id } + f2.map { it.user_id }).distinct()
            if (friendIds.isEmpty()) return@flow emit(emptyList())
            emit(fetchProfiles(friendIds))
        } catch (_: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    // ===================== BLOCKED USERS LIST =====================

    fun getBlockedUsersFlow(currentUsername: String): Flow<List<SupabaseProfile>> = flow {
        try {
            val myUuid = resolveUuid(currentUsername) ?: return@flow emit(emptyList())
            val blocked = postgrest.from("blocked_users").select {
                filter { eq("blocker_id", myUuid) }
            }.decodeList<BlockedUserRow>()
            val blockedIds = blocked.map { it.blocked_id }.distinct()
            if (blockedIds.isEmpty()) return@flow emit(emptyList())
            emit(fetchProfiles(blockedIds))
        } catch (_: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    // ===================== RELATIONSHIP STATUS =====================

    suspend fun getRelationshipStatus(
        currentUsername: String,
        targetUsername: String
    ): UserRelationshipStatus = withContext(Dispatchers.IO) {
        val cu = currentUsername.trim().lowercase()
        val tu = targetUsername.trim().lowercase()
        if (cu == tu) return@withContext UserRelationshipStatus.SELF

        val myUuid = resolveUuid(cu) ?: return@withContext UserRelationshipStatus.NOT_FRIENDS
        val targetUuid = resolveUuid(tu) ?: return@withContext UserRelationshipStatus.NOT_FRIENDS

        // Block checks (both directions)
        try {
            val blockedByMe = postgrest.from("blocked_users").select {
                filter { eq("blocker_id", myUuid); eq("blocked_id", targetUuid) }
            }.decodeList<BlockedUserRow>().isNotEmpty()
            if (blockedByMe) return@withContext UserRelationshipStatus.BLOCKED_BY_ME

            val blockedByThem = postgrest.from("blocked_users").select {
                filter { eq("blocker_id", targetUuid); eq("blocked_id", myUuid) }
            }.decodeList<BlockedUserRow>().isNotEmpty()
            if (blockedByThem) return@withContext UserRelationshipStatus.BLOCKED_BY_THEM
        } catch (_: Exception) {}

        // Friendship check (both directions)
        try {
            val isFriend = postgrest.from("friendships").select {
                filter { eq("user_id", myUuid); eq("friend_id", targetUuid) }
            }.decodeList<FriendshipRow>().isNotEmpty() ||
                postgrest.from("friendships").select {
                    filter { eq("user_id", targetUuid); eq("friend_id", myUuid) }
                }.decodeList<FriendshipRow>().isNotEmpty()
            if (isFriend) return@withContext UserRelationshipStatus.FRIENDS
        } catch (_: Exception) {}

        // Pending friend request checks
        try {
            // I sent a request to target
            val sent = postgrest.from("friend_requests").select {
                filter {
                    eq("sender_id", myUuid)
                    eq("receiver_id", targetUuid)
                    eq("status", "pending")
                }
            }.decodeList<FriendRequestRow>().isNotEmpty()
            if (sent) return@withContext UserRelationshipStatus.REQUEST_SENT

            // Target sent a request to me
            val received = postgrest.from("friend_requests").select {
                filter {
                    eq("sender_id", targetUuid)
                    eq("receiver_id", myUuid)
                    eq("status", "pending")
                }
            }.decodeList<FriendRequestRow>().isNotEmpty()
            if (received) return@withContext UserRelationshipStatus.REQUEST_RECEIVED
        } catch (_: Exception) {}

        return@withContext UserRelationshipStatus.NOT_FRIENDS
    }

    suspend fun areFriends(userA: String, userB: String): Boolean = withContext(Dispatchers.IO) {
        val u1 = userA.trim().lowercase()
        val u2 = userB.trim().lowercase()
        val uuidA = resolveUuid(u1) ?: return@withContext false
        val uuidB = resolveUuid(u2) ?: return@withContext false
        try {
            postgrest.from("friendships").select {
                filter { eq("user_id", uuidA); eq("friend_id", uuidB) }
            }.decodeList<FriendshipRow>().isNotEmpty() ||
                postgrest.from("friendships").select {
                    filter { eq("user_id", uuidB); eq("friend_id", uuidA) }
                }.decodeList<FriendshipRow>().isNotEmpty()
        } catch (_: Exception) { false }
    }

    // ===================== SEARCH =====================

    suspend fun searchByExactUsername(
        currentUsername: String,
        query: String
    ): UserSearchResult? = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase().removePrefix("@")
        if (cleanQuery.isEmpty()) return@withContext null
        try {
            val user = postgrest.from("profiles").select {
                filter { eq("normalized_username", cleanQuery) }
            }.decodeSingleOrNull<SupabaseProfile>() ?: return@withContext null
            val status = getRelationshipStatus(currentUsername, user.username)
            UserSearchResult(user = user, relationshipStatus = status)
        } catch (_: Exception) { null }
    }

    // ===================== FRIEND REQUEST ACTIONS =====================

    suspend fun sendFriendRequest(
        senderUsername: String,
        targetUsername: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val sender = senderUsername.trim().lowercase()
        val target = targetUsername.trim().lowercase()
        if (sender == target) {
            return@withContext Pair(false, "You cannot send a friend request to yourself.")
        }
        val senderUuid = resolveUuid(sender) ?: return@withContext Pair(false, "Sender not found.")
        val targetUuid = resolveUuid(target) ?: return@withContext Pair(false, "User not found.")
        try {
            // Block check
            val isBlocked = postgrest.from("blocked_users").select {
                filter { eq("blocker_id", senderUuid); eq("blocked_id", targetUuid) }
            }.decodeList<BlockedUserRow>().isNotEmpty() ||
                postgrest.from("blocked_users").select {
                    filter { eq("blocker_id", targetUuid); eq("blocked_id", senderUuid) }
                }.decodeList<BlockedUserRow>().isNotEmpty()
            if (isBlocked) {
                return@withContext Pair(false, "Cannot send request due to user privacy settings.")
            }

            // Check existing friendship
            val alreadyFriends = postgrest.from("friendships").select {
                filter { eq("user_id", senderUuid); eq("friend_id", targetUuid) }
            }.decodeList<FriendshipRow>().isNotEmpty() ||
                postgrest.from("friendships").select {
                    filter { eq("user_id", targetUuid); eq("friend_id", senderUuid) }
                }.decodeList<FriendshipRow>().isNotEmpty()
            if (alreadyFriends) {
                return@withContext Pair(false, "You are already friends.")
            }

            // Check if I already sent a request
            val alreadySent = postgrest.from("friend_requests").select {
                filter {
                    eq("sender_id", senderUuid)
                    eq("receiver_id", targetUuid)
                    eq("status", "pending")
                }
            }.decodeList<FriendRequestRow>().isNotEmpty()
            if (alreadySent) {
                return@withContext Pair(false, "Friend request already sent.")
            }

            // Check if target already sent me a request → auto-accept
            val incomingRequest = postgrest.from("friend_requests").select {
                filter {
                    eq("sender_id", targetUuid)
                    eq("receiver_id", senderUuid)
                    eq("status", "pending")
                }
            }.decodeSingleOrNull<FriendRequestRow>()
            if (incomingRequest != null) {
                acceptFriendRequest(target, sender)
                return@withContext Pair(true, "Request accepted! You are now friends.")
            }

            // Call RPC to send friend request (creates friend_request row + notification)
            postgrest.rpc("send_friend_request_fn") {
                set("sender_id", senderUuid)
                set("receiver_id", targetUuid)
            }

            PushNotificationManager.showCamouflagedNotification(appContext)
            return@withContext Pair(true, "Friend request sent!")
        } catch (e: Exception) {
            return@withContext Pair(false, "Failed to send request: ${e.localizedMessage}")
        }
    }

    suspend fun acceptFriendRequest(
        requesterUsername: String,
        recipientUsername: String
    ): Boolean = withContext(Dispatchers.IO) {
        val req = requesterUsername.trim().lowercase()
        val rec = recipientUsername.trim().lowercase()
        val requesterUuid = resolveUuid(req) ?: return@withContext false
        val recipientUuid = resolveUuid(rec) ?: return@withContext false
        try {
            // Find the pending friend request
            val request = postgrest.from("friend_requests").select {
                filter {
                    eq("sender_id", requesterUuid)
                    eq("receiver_id", recipientUuid)
                    eq("status", "pending")
                }
            }.decodeSingleOrNull<FriendRequestRow>() ?: return@withContext false

            // Call RPC to accept
            postgrest.rpc("accept_friend_request") {
                set("p_request_id", request.id)
                set("p_acceptor_id", recipientUuid)
            }

            PushNotificationManager.showCamouflagedNotification(appContext)
            return@withContext true
        } catch (_: Exception) { false }
    }

    suspend fun declineFriendRequest(
        requesterUsername: String,
        recipientUsername: String
    ): Boolean = withContext(Dispatchers.IO) {
        val req = requesterUsername.trim().lowercase()
        val rec = recipientUsername.trim().lowercase()
        val requesterUuid = resolveUuid(req) ?: return@withContext false
        val recipientUuid = resolveUuid(rec) ?: return@withContext false
        try {
            val request = postgrest.from("friend_requests").select {
                filter {
                    eq("sender_id", requesterUuid)
                    eq("receiver_id", recipientUuid)
                    eq("status", "pending")
                }
            }.decodeSingleOrNull<FriendRequestRow>() ?: return@withContext false

            postgrest.rpc("reject_friend_request") {
                set("p_request_id", request.id)
                set("p_rejector_id", recipientUuid)
            }

            PushNotificationManager.showCamouflagedNotification(appContext)
            return@withContext true
        } catch (_: Exception) { false }
    }

    suspend fun unfriend(userA: String, userB: String): Boolean = withContext(Dispatchers.IO) {
        val uuidA = resolveUuid(userA.trim().lowercase()) ?: return@withContext false
        val uuidB = resolveUuid(userB.trim().lowercase()) ?: return@withContext false
        try {
            // Delete in either direction
            postgrest.from("friendships").delete {
                filter { eq("user_id", uuidA); eq("friend_id", uuidB) }
            }
            postgrest.from("friendships").delete {
                filter { eq("user_id", uuidB); eq("friend_id", uuidA) }
            }
            // Invalidate caches
            uuidByUsernameCache.clear()
            usernameByUuidCache.clear()
            true
        } catch (_: Exception) { false }
    }

    // ===================== BLOCK & UNBLOCK =====================

    suspend fun blockUser(
        blockerUsername: String,
        blockedUsername: String
    ): Boolean = withContext(Dispatchers.IO) {
        val blockerUuid = resolveUuid(blockerUsername.trim().lowercase()) ?: return@withContext false
        val blockedUuid = resolveUuid(blockedUsername.trim().lowercase()) ?: return@withContext false
        try {
            // Remove friendship if exists
            postgrest.from("friendships").delete {
                filter { eq("user_id", blockerUuid); eq("friend_id", blockedUuid) }
            }
            postgrest.from("friendships").delete {
                filter { eq("user_id", blockedUuid); eq("friend_id", blockerUuid) }
            }
            // Insert block
            postgrest.from("blocked_users").insert(
                BlockedUserInsert(blocker_id = blockerUuid, blocked_id = blockedUuid)
            )
            true
        } catch (_: Exception) { false }
    }

    suspend fun unblockUser(
        blockerUsername: String,
        blockedUsername: String
    ): Boolean = withContext(Dispatchers.IO) {
        val blockerUuid = resolveUuid(blockerUsername.trim().lowercase()) ?: return@withContext false
        val blockedUuid = resolveUuid(blockedUsername.trim().lowercase()) ?: return@withContext false
        try {
            postgrest.from("blocked_users").delete {
                filter { eq("blocker_id", blockerUuid); eq("blocked_id", blockedUuid) }
            }
            true
        } catch (_: Exception) { false }
    }

    suspend fun reportUser(
        reporterUsername: String,
        targetUsername: String,
        reason: String
    ): Boolean = withContext(Dispatchers.IO) {
        // Placeholder — logging can be added server-side later
        true
    }

    // ===================== NOTIFICATIONS =====================

    fun getNotificationsFlow(username: String): Flow<List<SupabaseNotification>> = flow {
        try {
            val myUuid = resolveUuid(username) ?: return@flow emit(emptyList())
            val raw = postgrest.from("notifications").select {
                filter { eq("user_id", myUuid) }
            }.decodeList<SupabaseNotification>()
                .sortedByDescending { it.created_at }
            // Enrich each notification with sender info
            val relatedUserIds = raw.mapNotNull { it.related_user_id }.distinct()
            val profiles = fetchProfiles(relatedUserIds).associateBy { it.id }
            emit(raw.map { notif ->
                notif.enrich(profiles[notif.related_user_id])
            })
        } catch (_: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getNotificationsWithSenderFlow(
        username: String
    ): Flow<List<NotificationItemWithUser>> = flow {
        try {
            val myUuid = resolveUuid(username) ?: return@flow emit(emptyList())
            val raw = postgrest.from("notifications").select {
                filter { eq("user_id", myUuid) }
            }.decodeList<SupabaseNotification>()
                .sortedByDescending { it.created_at }
            val relatedUserIds = raw.mapNotNull { it.related_user_id }.distinct()
            val profiles = fetchProfiles(relatedUserIds).associateBy { it.id }
            emit(raw.map { notif ->
                val senderProfile = profiles[notif.related_user_id]
                NotificationItemWithUser(
                    notification = notif.enrich(senderProfile),
                    senderUser = senderProfile
                )
            })
        } catch (_: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getUnreadNotificationsCountFlow(username: String): Flow<Int> = flow {
        try {
            val myUuid = resolveUuid(username) ?: return@flow emit(0)
            val unread = postgrest.from("notifications").select {
                filter { eq("user_id", myUuid); eq("is_read", false) }
            }.decodeList<SupabaseNotification>()
            emit(unread.size)
        } catch (_: Exception) {
            emit(0)
        }
    }.flowOn(Dispatchers.IO)

    /** Note: id is now a String (UUID) instead of Long. */
    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        try {
            postgrest.from("notifications").update({
                set("is_read", true)
            }) {
                filter { eq("id", id) }
            }
        } catch (_: Exception) {}
    }

    suspend fun markAllNotificationsAsRead(username: String) = withContext(Dispatchers.IO) {
        try {
            val myUuid = resolveUuid(username) ?: return@withContext
            postgrest.from("notifications").update({
                set("is_read", true)
            }) {
                filter { eq("user_id", myUuid); eq("is_read", false) }
            }
        } catch (_: Exception) {}
    }

    /** Note: id is now a String (UUID) instead of Long. */
    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        try {
            postgrest.from("notifications").delete {
                filter { eq("id", id) }
            }
        } catch (_: Exception) {}
    }

    suspend fun clearAllNotifications(username: String) = withContext(Dispatchers.IO) {
        try {
            val myUuid = resolveUuid(username) ?: return@withContext
            postgrest.from("notifications").delete {
                filter { eq("user_id", myUuid) }
            }
        } catch (_: Exception) {}
    }
}
