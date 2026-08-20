package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String, // lowercase, Instagram format
    val email: String,
    val passwordHash: String,
    val salt: String,
    val name: String = "",
    val bio: String = "",
    val profilePhotoUri: String? = null,
    val isEmailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val avatarColorHex: Long = 0xFF00E5FF,
    val statusMessage: String = "Secure & Protected",
    val partnerUsername: String? = null
)

@Entity(tableName = "email_otps")
data class OtpEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val otpCode: String,
    val type: String, // "SIGNUP" or "RESET"
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val attemptsCount: Int = 0,
    val isUsed: Boolean = false
)

@Entity(tableName = "friendships")
data class FriendshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val user1Username: String, // alphabetically lower username for unique pairing
    val user2Username: String, // alphabetically higher username
    val requesterUsername: String,
    val status: String = "REQUESTED", // "REQUESTED", "ACCEPTED", "DECLINED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_users")
data class BlockedUserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val blockerUsername: String,
    val blockedUsername: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipientUsername: String,
    val senderUsername: String,
    val type: String, // "FRIEND_REQUEST", "REQUEST_ACCEPTED", "SECURITY_ALERT"
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val status: String = "PENDING" // "PENDING", "ACCEPTED", "DECLINED"
)

@Entity(tableName = "vault_pins")
data class VaultPinEntity(
    @PrimaryKey
    val username: String,
    val pinHash: String,
    val salt: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerUsername: String,
    val mediaUri: String,
    val mediaType: String, // "IMAGE" or "VIDEO"
    val title: String = "",
    val fileSizeBytes: Long = 0L,
    val durationSeconds: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderUsername: String,
    val receiverUsername: String,
    val content: String = "",
    val mediaUri: String? = null,
    val mediaType: String = "TEXT", // "TEXT", "IMAGE", "VIDEO", "ONE_SHOT_IMAGE", "ONE_SHOT_VIDEO"
    val status: String = "SENT", // "SENDING", "SENT", "SEEN", "FAILED"
    val isOneShotOpened: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val deletedForSender: Boolean = false,
    val deletedForReceiver: Boolean = false
)

@Entity(tableName = "chat_settings")
data class ChatSettingsEntity(
    @PrimaryKey
    val conversationKey: String, // "userA_userB"
    val isSecureChatEnabled: Boolean = false
)

// Legacy entity kept for backward compat if needed
@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerUsername: String,
    val title: String,
    val content: String,
    val category: String = "NOTE",
    val createdAt: Long = System.currentTimeMillis()
)

// DAOs

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:identifier) OR LOWER(email) = LOWER(:identifier) LIMIT 1")
    suspend fun getUserByIdentifier(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) LIKE '%' || LOWER(:query) || '%' LIMIT 30")
    suspend fun searchUsersByUsername(query: String): List<UserEntity>

    @Query("SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(:username)")
    suspend fun countUsername(username: String): Int

    @Query("SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(:username) AND id != :userId")
    suspend fun countUsernameExcludingUser(username: String, userId: Long): Int

    @Query("SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email)")
    suspend fun countEmail(email: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET username = :username, name = :name, bio = :bio, profilePhotoUri = :photoUri WHERE id = :userId")
    suspend fun updateProfile(userId: Long, username: String, name: String, bio: String, photoUri: String?)

    @Query("UPDATE users SET isEmailVerified = 1 WHERE LOWER(email) = LOWER(:email)")
    suspend fun markEmailVerified(email: String)

    @Query("UPDATE users SET passwordHash = :passwordHash, salt = :salt WHERE LOWER(email) = LOWER(:email)")
    suspend fun updatePassword(email: String, passwordHash: String, salt: String)

    @Query("SELECT * FROM users WHERE username IN (:usernames)")
    suspend fun getUsersByUsernames(usernames: List<String>): List<UserEntity>
}

@Dao
interface OtpDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtp(otp: OtpEntity): Long

    @Query("SELECT * FROM email_otps WHERE LOWER(email) = LOWER(:email) AND type = :type AND isUsed = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestActiveOtp(email: String, type: String): OtpEntity?

    @Query("UPDATE email_otps SET isUsed = 1 WHERE id = :id")
    suspend fun markOtpUsed(id: Long)

    @Query("UPDATE email_otps SET attemptsCount = attemptsCount + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)
}

@Dao
interface FriendshipDao {
    @Query("""
        SELECT * FROM friendships 
        WHERE (LOWER(user1Username) = LOWER(:username) OR LOWER(user2Username) = LOWER(:username)) 
          AND status = 'ACCEPTED' 
        ORDER BY updatedAt DESC
    """)
    fun getFriendsFlow(username: String): Flow<List<FriendshipEntity>>

    @Query("""
        SELECT * FROM friendships 
        WHERE (LOWER(user1Username) = LOWER(:username) OR LOWER(user2Username) = LOWER(:username)) 
          AND status = 'ACCEPTED' 
        ORDER BY updatedAt DESC
    """)
    suspend fun getFriends(username: String): List<FriendshipEntity>

    @Query("""
        SELECT * FROM friendships 
        WHERE ((LOWER(user1Username) = LOWER(:userA) AND LOWER(user2Username) = LOWER(:userB)) 
           OR (LOWER(user1Username) = LOWER(:userB) AND LOWER(user2Username) = LOWER(:userA))) 
        LIMIT 1
    """)
    suspend fun getFriendship(userA: String, userB: String): FriendshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFriendship(friendship: FriendshipEntity): Long

    @Query("""
        DELETE FROM friendships 
        WHERE ((LOWER(user1Username) = LOWER(:userA) AND LOWER(user2Username) = LOWER(:userB)) 
           OR (LOWER(user1Username) = LOWER(:userB) AND LOWER(user2Username) = LOWER(:userA)))
    """)
    suspend fun removeFriendship(userA: String, userB: String)

    @Query("""
        UPDATE friendships 
        SET status = :status, updatedAt = :updatedAt 
        WHERE ((LOWER(user1Username) = LOWER(:userA) AND LOWER(user2Username) = LOWER(:userB)) 
           OR (LOWER(user1Username) = LOWER(:userB) AND LOWER(user2Username) = LOWER(:userA)))
    """)
    suspend fun updateFriendshipStatus(userA: String, userB: String, status: String, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface BlockedUserDao {
    @Query("SELECT * FROM blocked_users WHERE LOWER(blockerUsername) = LOWER(:blocker) ORDER BY createdAt DESC")
    fun getBlockedUsersFlow(blocker: String): Flow<List<BlockedUserEntity>>

    @Query("SELECT * FROM blocked_users WHERE LOWER(blockerUsername) = LOWER(:blocker) ORDER BY createdAt DESC")
    suspend fun getBlockedUsers(blocker: String): List<BlockedUserEntity>

    @Query("SELECT COUNT(*) FROM blocked_users WHERE LOWER(blockerUsername) = LOWER(:blocker) AND LOWER(blockedUsername) = LOWER(:blocked)")
    suspend fun isBlockedBy(blocker: String, blocked: String): Int

    @Query("""
        SELECT COUNT(*) FROM blocked_users 
        WHERE (LOWER(blockerUsername) = LOWER(:userA) AND LOWER(blockedUsername) = LOWER(:userB))
           OR (LOWER(blockerUsername) = LOWER(:userB) AND LOWER(blockedUsername) = LOWER(:userA))
    """)
    suspend fun isAnyBlocked(userA: String, userB: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: BlockedUserEntity): Long

    @Query("DELETE FROM blocked_users WHERE LOWER(blockerUsername) = LOWER(:blocker) AND LOWER(blockedUsername) = LOWER(:blocked)")
    suspend fun unblockUser(blocker: String, blocked: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE LOWER(recipientUsername) = LOWER(:recipient) ORDER BY timestamp DESC")
    fun getNotificationsFlow(recipient: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE LOWER(recipientUsername) = LOWER(:recipient) AND isRead = 0")
    fun getUnreadNotificationsCountFlow(recipient: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE LOWER(recipientUsername) = LOWER(:recipient)")
    suspend fun markAllAsRead(recipient: String)

    @Query("UPDATE notifications SET status = :status, isRead = 1 WHERE id = :id")
    suspend fun updateStatusAndRead(id: Long, status: String)

    @Query("UPDATE notifications SET status = :status, isRead = 1 WHERE LOWER(recipientUsername) = LOWER(:recipient) AND LOWER(senderUsername) = LOWER(:sender) AND type = 'FRIEND_REQUEST'")
    suspend fun updateFriendRequestStatus(recipient: String, sender: String, status: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM notifications WHERE LOWER(recipientUsername) = LOWER(:recipient)")
    suspend fun deleteAllNotifications(recipient: String)
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_pins WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getVaultPin(username: String): VaultPinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setVaultPin(pin: VaultPinEntity)

    @Query("SELECT * FROM vault_media WHERE LOWER(ownerUsername) = LOWER(:username) ORDER BY createdAt DESC")
    fun getVaultMediaFlow(username: String): Flow<List<VaultMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultMedia(media: VaultMediaEntity): Long

    @Query("DELETE FROM vault_media WHERE id = :id")
    suspend fun deleteVaultMedia(id: Long)
}

@Dao
interface ChatMessageDao {
    @Query("""
        SELECT * FROM chat_messages 
        WHERE ((LOWER(senderUsername) = LOWER(:userA) AND LOWER(receiverUsername) = LOWER(:userB) AND deletedForSender = 0)
           OR (LOWER(senderUsername) = LOWER(:userB) AND LOWER(receiverUsername) = LOWER(:userA) AND deletedForReceiver = 0))
        ORDER BY timestamp ASC
    """)
    fun getConversationMessagesFlow(userA: String, userB: String): Flow<List<ChatMessageEntity>>

    @Query("""
        SELECT * FROM chat_messages 
        WHERE ((LOWER(senderUsername) = LOWER(:userA) AND LOWER(receiverUsername) = LOWER(:userB))
           OR (LOWER(senderUsername) = LOWER(:userB) AND LOWER(receiverUsername) = LOWER(:userA)))
        ORDER BY timestamp DESC LIMIT 1
    """)
    suspend fun getLastMessage(userA: String, userB: String): ChatMessageEntity?

    @Query("""
        SELECT COUNT(*) FROM chat_messages 
        WHERE LOWER(senderUsername) = LOWER(:sender) 
          AND LOWER(receiverUsername) = LOWER(:recipient) 
          AND status != 'SEEN' 
          AND deletedForReceiver = 0
    """)
    fun getUnreadCountFlow(sender: String, recipient: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: String)

    @Query("UPDATE chat_messages SET status = 'SEEN' WHERE LOWER(senderUsername) = LOWER(:partner) AND LOWER(receiverUsername) = LOWER(:current) AND status != 'SEEN'")
    suspend fun markAllAsSeen(partner: String, current: String)

    @Query("UPDATE chat_messages SET isOneShotOpened = 1 WHERE id = :id")
    suspend fun markOneShotOpened(id: Long)

    @Query("""
        UPDATE chat_messages 
        SET deletedForSender = CASE WHEN LOWER(senderUsername) = LOWER(:current) THEN 1 ELSE deletedForSender END,
            deletedForReceiver = CASE WHEN LOWER(receiverUsername) = LOWER(:current) THEN 1 ELSE deletedForReceiver END
        WHERE (LOWER(senderUsername) = LOWER(:current) AND LOWER(receiverUsername) = LOWER(:partner))
           OR (LOWER(senderUsername) = LOWER(:partner) AND LOWER(receiverUsername) = LOWER(:current))
    """)
    suspend fun clearChatForMe(current: String, partner: String)

    @Query("""
        DELETE FROM chat_messages 
        WHERE (LOWER(senderUsername) = LOWER(:current) AND LOWER(receiverUsername) = LOWER(:partner))
           OR (LOWER(senderUsername) = LOWER(:partner) AND LOWER(receiverUsername) = LOWER(:current))
    """)
    suspend fun clearChatForBoth(current: String, partner: String)
}

@Dao
interface ChatSettingsDao {
    @Query("SELECT * FROM chat_settings WHERE conversationKey = :key LIMIT 1")
    suspend fun getSettings(key: String): ChatSettingsEntity?

    @Query("SELECT * FROM chat_settings WHERE conversationKey = :key LIMIT 1")
    fun getSettingsFlow(key: String): Flow<ChatSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: ChatSettingsEntity)
}

@Database(
    entities = [
        UserEntity::class,
        OtpEntity::class,
        FriendshipEntity::class,
        BlockedUserEntity::class,
        NotificationEntity::class,
        VaultPinEntity::class,
        VaultMediaEntity::class,
        ChatMessageEntity::class,
        ChatSettingsEntity::class,
        VaultItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CleanShieldDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun otpDao(): OtpDao
    abstract fun friendshipDao(): FriendshipDao
    abstract fun blockedUserDao(): BlockedUserDao
    abstract fun notificationDao(): NotificationDao
    abstract fun vaultDao(): VaultDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatSettingsDao(): ChatSettingsDao
}
