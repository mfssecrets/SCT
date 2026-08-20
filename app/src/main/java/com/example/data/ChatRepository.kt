package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSettingsEntity
import com.example.data.local.CleanShieldDatabase
import com.example.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class InboxConversation(
    val friend: UserEntity,
    val lastMessage: ChatMessageEntity?,
    val unreadCount: Int = 0
)

class ChatRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val authRepo = AuthRepository.getInstance(context)
    private val socialRepo = SocialRepository.getInstance(context)
    private val db: CleanShieldDatabase = authRepo.database
    private val chatMessageDao = db.chatMessageDao()
    private val chatSettingsDao = db.chatSettingsDao()
    private val userDao = db.userDao()

    companion object {
        @Volatile
        private var INSTANCE: ChatRepository? = null

        fun getInstance(context: Context): ChatRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun getConversationKey(user1: String, user2: String): String {
        val u1 = user1.trim().lowercase()
        val u2 = user2.trim().lowercase()
        return if (u1 < u2) "${u1}_${u2}" else "${u2}_${u1}"
    }

    // --- INBOX FLOW (Friends only) ---

    fun getInboxFlow(currentUsername: String): Flow<List<InboxConversation>> {
        val user = currentUsername.trim().lowercase()
        return socialRepo.getFriendsListFlow(user).map { friends ->
            friends.map { friend ->
                val lastMsg = chatMessageDao.getLastMessage(user, friend.username.lowercase())
                // Only consider valid if not deleted for current user
                val validLastMsg = if (lastMsg != null) {
                    val isSender = lastMsg.senderUsername.equals(user, ignoreCase = true)
                    if (isSender && !lastMsg.deletedForSender) lastMsg
                    else if (!isSender && !lastMsg.deletedForReceiver) lastMsg
                    else null
                } else null

                InboxConversation(
                    friend = friend,
                    lastMessage = validLastMsg,
                    unreadCount = if (validLastMsg != null && !validLastMsg.senderUsername.equals(user, ignoreCase = true) && validLastMsg.status != "SEEN") 1 else 0
                )
            }.sortedByDescending { it.lastMessage?.timestamp ?: 0L }
        }.flowOn(Dispatchers.IO)
    }

    // --- CHAT MESSAGES ---

    fun getConversationMessagesFlow(userA: String, userB: String): Flow<List<ChatMessageEntity>> {
        return chatMessageDao.getConversationMessagesFlow(userA.trim().lowercase(), userB.trim().lowercase())
            .flowOn(Dispatchers.IO)
    }

    suspend fun sendMessage(
        sender: String,
        receiver: String,
        content: String,
        mediaUri: Uri?,
        mediaType: String // "TEXT", "IMAGE", "VIDEO", "ONE_SHOT_IMAGE", "ONE_SHOT_VIDEO"
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val u1 = sender.trim().lowercase()
        val u2 = receiver.trim().lowercase()

        // Server-side friendship & block verification
        val areFriends = socialRepo.areFriends(u1, u2)
        if (!areFriends) {
            return@withContext Pair(false, "You can only message accepted friends.")
        }

        var savedMediaUriStr: String? = null
        if (mediaUri != null) {
            try {
                val chatMediaDir = File(appContext.filesDir, "chat_media")
                if (!chatMediaDir.exists()) chatMediaDir.mkdirs()
                val ext = if (mediaType.contains("VIDEO")) "mp4" else "jpg"
                val destFile = File(chatMediaDir, "msg_${System.currentTimeMillis()}_${(100..999).random()}.$ext")

                appContext.contentResolver.openInputStream(mediaUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                savedMediaUriStr = if (destFile.exists() && destFile.length() > 0) {
                    Uri.fromFile(destFile).toString()
                } else {
                    mediaUri.toString()
                }
            } catch (e: Exception) {
                savedMediaUriStr = mediaUri.toString()
            }
        }

        val message = ChatMessageEntity(
            senderUsername = u1,
            receiverUsername = u2,
            content = content,
            mediaUri = savedMediaUriStr,
            mediaType = mediaType,
            status = "SENDING",
            isOneShotOpened = false,
            timestamp = System.currentTimeMillis()
        )

        val id = chatMessageDao.insertMessage(message)

        // Real-time simulated delivery transitions
        delay(120)
        chatMessageDao.updateMessageStatus(id, "SENT")

        return@withContext Pair(true, null)
    }

    suspend fun markConversationSeen(partner: String, current: String) = withContext(Dispatchers.IO) {
        chatMessageDao.markAllAsSeen(partner.trim().lowercase(), current.trim().lowercase())
    }

    suspend fun markOneShotOpened(messageId: Long) = withContext(Dispatchers.IO) {
        chatMessageDao.markOneShotOpened(messageId)
    }

    suspend fun clearChatForMe(current: String, partner: String) = withContext(Dispatchers.IO) {
        chatMessageDao.clearChatForMe(current.trim().lowercase(), partner.trim().lowercase())
    }

    suspend fun clearChatForBoth(current: String, partner: String) = withContext(Dispatchers.IO) {
        chatMessageDao.clearChatForBoth(current.trim().lowercase(), partner.trim().lowercase())
    }

    // --- SECURE MY CHAT SETTINGS ---

    fun getSecureChatModeFlow(userA: String, userB: String): Flow<Boolean> {
        val key = getConversationKey(userA, userB)
        return chatSettingsDao.getSettingsFlow(key).map { it?.isSecureChatEnabled ?: false }
            .flowOn(Dispatchers.IO)
    }

    suspend fun setSecureChatMode(userA: String, userB: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val key = getConversationKey(userA, userB)
        chatSettingsDao.saveSettings(
            ChatSettingsEntity(
                conversationKey = key,
                isSecureChatEnabled = enabled
            )
        )
    }
}
