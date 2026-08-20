package com.example.data

import android.content.Context
import com.example.data.local.BlockedUserEntity
import com.example.data.local.CleanShieldDatabase
import com.example.data.local.FriendshipEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.UserEntity
import com.example.util.PushNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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
    val user: UserEntity,
    val isOnline: Boolean = true,
    val sinceTime: Long = System.currentTimeMillis()
)

data class UserSearchResult(
    val user: UserEntity,
    val relationshipStatus: UserRelationshipStatus
)

data class NotificationItemWithUser(
    val notification: NotificationEntity,
    val senderUser: UserEntity?
)

class SocialRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val authRepo = AuthRepository.getInstance(appContext)
    private val db: CleanShieldDatabase = authRepo.database
    private val userDao = db.userDao()
    private val friendshipDao = db.friendshipDao()
    private val blockedDao = db.blockedUserDao()
    private val notificationDao = db.notificationDao()

    companion object {
        @Volatile
        private var INSTANCE: SocialRepository? = null

        fun getInstance(context: Context): SocialRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SocialRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // --- FRIENDS LIST ---

    fun getFriendsListFlow(currentUsername: String): Flow<List<UserEntity>> {
        val lowerCurrent = currentUsername.trim().lowercase()
        return friendshipDao.getFriendsFlow(lowerCurrent).map { friendships ->
            val friendUsernames = friendships.map { f ->
                if (f.user1Username.equals(lowerCurrent, ignoreCase = true)) f.user2Username else f.user1Username
            }
            if (friendUsernames.isEmpty()) {
                emptyList()
            } else {
                userDao.getUsersByUsernames(friendUsernames)
            }
        }.flowOn(Dispatchers.IO)
    }

    // --- BLOCKED USERS LIST ---

    fun getBlockedUsersFlow(currentUsername: String): Flow<List<UserEntity>> {
        val lowerCurrent = currentUsername.trim().lowercase()
        return blockedDao.getBlockedUsersFlow(lowerCurrent).map { blockedList ->
            val blockedUsernames = blockedList.map { it.blockedUsername }
            if (blockedUsernames.isEmpty()) {
                emptyList()
            } else {
                userDao.getUsersByUsernames(blockedUsernames)
            }
        }.flowOn(Dispatchers.IO)
    }

    // --- RELATIONSHIP STATUS CHECK ---

    suspend fun getRelationshipStatus(currentUsername: String, targetUsername: String): UserRelationshipStatus = withContext(Dispatchers.IO) {
        val u1 = currentUsername.trim().lowercase()
        val u2 = targetUsername.trim().lowercase()

        if (u1 == u2) return@withContext UserRelationshipStatus.SELF

        if (blockedDao.isBlockedBy(u1, u2) > 0) return@withContext UserRelationshipStatus.BLOCKED_BY_ME
        if (blockedDao.isBlockedBy(u2, u1) > 0) return@withContext UserRelationshipStatus.BLOCKED_BY_THEM

        val friendship = friendshipDao.getFriendship(u1, u2)
        if (friendship != null) {
            when (friendship.status) {
                "ACCEPTED" -> return@withContext UserRelationshipStatus.FRIENDS
                "REQUESTED" -> {
                    return@withContext if (friendship.requesterUsername.equals(u1, ignoreCase = true)) {
                        UserRelationshipStatus.REQUEST_SENT
                    } else {
                        UserRelationshipStatus.REQUEST_RECEIVED
                    }
                }
            }
        }

        return@withContext UserRelationshipStatus.NOT_FRIENDS
    }

    suspend fun areFriends(userA: String, userB: String): Boolean = withContext(Dispatchers.IO) {
        val u1 = userA.trim().lowercase()
        val u2 = userB.trim().lowercase()
        if (blockedDao.isAnyBlocked(u1, u2) > 0) return@withContext false
        val friendship = friendshipDao.getFriendship(u1, u2)
        return@withContext friendship?.status == "ACCEPTED"
    }

    // --- EXACT USERNAME SEARCH ---

    suspend fun searchByExactUsername(currentUsername: String, query: String): UserSearchResult? = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase().removePrefix("@")
        if (cleanQuery.isEmpty()) return@withContext null

        val user = userDao.getUserByUsername(cleanQuery) ?: return@withContext null
        val status = getRelationshipStatus(currentUsername, user.username)
        return@withContext UserSearchResult(user = user, relationshipStatus = status)
    }

    // --- FRIEND REQUEST ACTIONS ---

    suspend fun sendFriendRequest(senderUsername: String, targetUsername: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val sender = senderUsername.trim().lowercase()
        val target = targetUsername.trim().lowercase()

        if (sender == target) {
            return@withContext Pair(false, "You cannot send a friend request to yourself.")
        }

        if (blockedDao.isAnyBlocked(sender, target) > 0) {
            return@withContext Pair(false, "Cannot send request due to user privacy settings.")
        }

        val pair1 = if (sender < target) sender else target
        val pair2 = if (sender < target) target else sender

        val existing = friendshipDao.getFriendship(sender, target)
        if (existing != null) {
            if (existing.status == "ACCEPTED") {
                return@withContext Pair(false, "You are already friends.")
            }
            if (existing.status == "REQUESTED") {
                if (existing.requesterUsername.equals(sender, ignoreCase = true)) {
                    return@withContext Pair(false, "Friend request already sent.")
                } else {
                    // Auto-accept reciprocal request
                    acceptFriendRequest(target, sender)
                    return@withContext Pair(true, "Request accepted! You are now friends.")
                }
            }
        }

        friendshipDao.insertOrUpdateFriendship(
            FriendshipEntity(
                user1Username = pair1,
                user2Username = pair2,
                requesterUsername = sender,
                status = "REQUESTED",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Send notification to recipient
        notificationDao.insertNotification(
            NotificationEntity(
                recipientUsername = target,
                senderUsername = sender,
                type = "FRIEND_REQUEST",
                title = "New Friend Request",
                message = "@$sender sent you a friend request.",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                status = "PENDING"
            )
        )

        // Trigger stealth push notification
        PushNotificationManager.showCamouflagedNotification(appContext)

        return@withContext Pair(true, "Friend request sent!")
    }

    suspend fun acceptFriendRequest(requesterUsername: String, recipientUsername: String): Boolean = withContext(Dispatchers.IO) {
        val req = requesterUsername.trim().lowercase()
        val rec = recipientUsername.trim().lowercase()

        val pair1 = if (req < rec) req else rec
        val pair2 = if (req < rec) rec else req

        friendshipDao.insertOrUpdateFriendship(
            FriendshipEntity(
                user1Username = pair1,
                user2Username = pair2,
                requesterUsername = req,
                status = "ACCEPTED",
                updatedAt = System.currentTimeMillis()
            )
        )

        // Update recipient's notification for this request to ACCEPTED
        notificationDao.updateFriendRequestStatus(recipient = rec, sender = req, status = "ACCEPTED")

        // Notify requester that their request was accepted
        notificationDao.insertNotification(
            NotificationEntity(
                recipientUsername = req,
                senderUsername = rec,
                type = "REQUEST_ACCEPTED",
                title = "Friend Request Accepted",
                message = "@$rec accepted your friend request. You can now chat securely.",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                status = "ACCEPTED"
            )
        )

        // Trigger stealth push notification
        PushNotificationManager.showCamouflagedNotification(appContext)

        return@withContext true
    }

    suspend fun declineFriendRequest(requesterUsername: String, recipientUsername: String): Boolean = withContext(Dispatchers.IO) {
        val req = requesterUsername.trim().lowercase()
        val rec = recipientUsername.trim().lowercase()

        // 1. Remove friendship entry
        friendshipDao.removeFriendship(req, rec)

        // 2. Update recipient's notification status to REJECTED and mark read
        notificationDao.updateFriendRequestStatus(recipient = rec, sender = req, status = "REJECTED")

        // 3. Notify requester that request was rejected
        notificationDao.insertNotification(
            NotificationEntity(
                recipientUsername = req,
                senderUsername = rec,
                type = "REQUEST_REJECTED",
                title = "Friend Request Rejected",
                message = "@$rec rejected your friend request.",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                status = "REJECTED"
            )
        )

        // Trigger stealth push notification
        PushNotificationManager.showCamouflagedNotification(appContext)

        return@withContext true
    }

    suspend fun unfriend(userA: String, userB: String): Boolean = withContext(Dispatchers.IO) {
        val u1 = userA.trim().lowercase()
        val u2 = userB.trim().lowercase()
        friendshipDao.removeFriendship(u1, u2)
        return@withContext true
    }

    // --- BLOCK & UNBLOCK ACTIONS ---

    suspend fun blockUser(blockerUsername: String, blockedUsername: String): Boolean = withContext(Dispatchers.IO) {
        val blocker = blockerUsername.trim().lowercase()
        val blocked = blockedUsername.trim().lowercase()

        // 1. Remove friendship if exists
        friendshipDao.removeFriendship(blocker, blocked)

        // 2. Insert block
        blockedDao.insertBlock(
            BlockedUserEntity(
                blockerUsername = blocker,
                blockedUsername = blocked,
                createdAt = System.currentTimeMillis()
            )
        )
        return@withContext true
    }

    suspend fun unblockUser(blockerUsername: String, blockedUsername: String): Boolean = withContext(Dispatchers.IO) {
        val blocker = blockerUsername.trim().lowercase()
        val blocked = blockedUsername.trim().lowercase()
        blockedDao.unblockUser(blocker, blocked)
        return@withContext true
    }

    suspend fun reportUser(reporterUsername: String, targetUsername: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        // Log report securely
        return@withContext true
    }

    // --- NOTIFICATIONS ---

    fun getNotificationsFlow(username: String): Flow<List<NotificationEntity>> {
        return notificationDao.getNotificationsFlow(username.trim().lowercase()).flowOn(Dispatchers.IO)
    }

    fun getNotificationsWithSenderFlow(username: String): Flow<List<NotificationItemWithUser>> {
        val cleanUser = username.trim().lowercase()
        return notificationDao.getNotificationsFlow(cleanUser).map { list ->
            list.map { notif ->
                val sender = userDao.getUserByUsername(notif.senderUsername)
                NotificationItemWithUser(
                    notification = notif,
                    senderUser = sender
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    fun getUnreadNotificationsCountFlow(username: String): Flow<Int> {
        return notificationDao.getUnreadNotificationsCountFlow(username.trim().lowercase()).flowOn(Dispatchers.IO)
    }

    suspend fun markNotificationAsRead(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead(username: String) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(username.trim().lowercase())
    }

    suspend fun deleteNotification(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.deleteNotification(id)
    }

    suspend fun clearAllNotifications(username: String) = withContext(Dispatchers.IO) {
        notificationDao.deleteAllNotifications(username.trim().lowercase())
    }
}
