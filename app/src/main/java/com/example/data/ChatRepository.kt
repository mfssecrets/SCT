package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.supabase.SupabaseClient
import io.github.jan-tennert.supabase.postgrest.from
import io.github.jan-tennert.supabase.postgrest.filter.*
import io.github.jan-tennert.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

// ==================== Data Classes ====================

@kotlinx.serialization.Serializable
data class SupabaseMessage(
    val id: String = "",
    val conversation_id: String = "",
    val sender_id: String = "",
    val message_type: String = "TEXT",
    val content: String? = null,
    val media_reference: String? = null,
    val one_shot: Boolean = false,
    val one_shot_opened: Boolean = false,
    val one_shot_opened_at: String? = null,
    val sent_at: String? = null,
    val seen_at: String? = null,
    val deleted_for_sender: Boolean = false,
    val deleted_for_receiver: Boolean = false,
    val deleted_at: String? = null,
    val permanently_deleted_at: String? = null,
    val status: String = "SENDING"
) {
    /** Epoch millis derived from ISO-8601 sent_at. */
    val sentAtMillis: Long
        get() = try {
            sent_at?.let { java.time.Instant.parse(it).toEpochMilli() } ?: 0L
        } catch (_: Exception) { 0L }

    /** Epoch millis derived from ISO-8601 seen_at. */
    val seenAtMillis: Long
        get() = try {
            seen_at?.let { java.time.Instant.parse(it).toEpochMilli() } ?: 0L
        } catch (_: Exception) { 0L }
}

data class InboxConversation(
    val friend: SupabaseProfile,
    val lastMessage: SupabaseMessage? = null,
    val unreadCount: Int = 0
)

// ---- Internal Supabase row types ----

@kotlinx.serialization.Serializable
private data class ConversationMemberRow(
    val conversation_id: String = "",
    val user_id: String = ""
)

@kotlinx.serialization.Serializable
private data class ChatSettingsRow(
    val id: String = "",
    val conversation_id: String = "",
    val is_secure_chat_enabled: Boolean = false
)

@kotlinx.serialization.Serializable
private data class MessageInsert(
    val conversation_id: String,
    val sender_id: String,
    val message_type: String,
    val content: String? = null,
    val media_reference: String? = null,
    val one_shot: Boolean = false,
    val status: String,
    val sent_at: String
)

@kotlinx.serialization.Serializable
private data class ChatSettingsInsert(
    val conversation_id: String,
    val is_secure_chat_enabled: Boolean
)

// ==================== Repository ====================

class ChatRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val postgrest get() = SupabaseClient.postgrest
    private val storage get() = SupabaseClient.storage

    companion object {
        @Volatile
        private var INSTANCE: ChatRepository? = null

        fun getInstance(context: Context): ChatRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ==================== HELPERS ====================

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

    /** Returns true if the message should be visible to the given user (not soft-deleted for them). */
    private fun isVisibleToUser(message: SupabaseMessage, currentUserId: String): Boolean {
        return if (message.sender_id == currentUserId) {
            !message.deleted_for_sender
        } else {
            !message.deleted_for_receiver
        }
    }

    // ==================== INBOX ====================

    fun getInboxFlow(currentUserId: String): Flow<List<InboxConversation>> = flow {
        try {
            // 1. Get all conversation_ids where current user is a member
            val memberships = postgrest.from("conversation_members").select {
                filter { eq("user_id", currentUserId) }
            }.decodeList<ConversationMemberRow>()

            if (memberships.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val conversationIds = memberships.map { it.conversation_id }

            // 2. Get all members across these conversations to find the "friend" in each
            val allMembers = postgrest.from("conversation_members").select {
                filter { `in`("conversation_id", conversationIds) }
            }.decodeList<ConversationMemberRow>()

            // Map each conversation_id to the other member's user_id
            val conversationFriendMap = mutableMapOf<String, String>()
            for (convId in conversationIds) {
                val friendId = allMembers
                    .filter { it.conversation_id == convId && it.user_id != currentUserId }
                    .firstOrNull()?.user_id
                if (friendId != null) conversationFriendMap[convId] = friendId
            }

            // 3. Batch-fetch friend profiles
            val friendIds = conversationFriendMap.values.distinct()
            val profiles = fetchProfiles(friendIds).associateBy { it.id }

            // 4. Fetch all messages for all conversations (most-recent first)
            val allMessages = postgrest.from("messages").select {
                filter { `in`("conversation_id", conversationIds) }
                order("sent_at", Order.DESCENDING)
            }.decodeList<SupabaseMessage>()

            val messagesByConversation = allMessages.groupBy { it.conversation_id }

            // 5. Build inbox entries
            val inbox = conversationIds.mapNotNull { convId ->
                val friendId = conversationFriendMap[convId] ?: return@mapNotNull null
                val friend = profiles[friendId] ?: return@mapNotNull null
                val messages = messagesByConversation[convId] ?: emptyList()

                // Last visible message (already sorted desc)
                val lastMessage = messages.firstOrNull { isVisibleToUser(it, currentUserId) }

                // Unread count: visible messages from others that haven't been seen
                val unreadCount = messages.count { msg ->
                    msg.sender_id != currentUserId &&
                        isVisibleToUser(msg, currentUserId) &&
                        msg.seen_at == null
                }

                InboxConversation(
                    friend = friend,
                    lastMessage = lastMessage,
                    unreadCount = unreadCount
                )
            }

            // Sort by last message time; conversations with no messages go to the end
            emit(inbox.sortedByDescending { it.lastMessage?.sentAtMillis ?: 0L })
        } catch (_: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    // ==================== CONVERSATION MESSAGES ====================

    fun getConversationMessagesFlow(
        conversationId: String,
        currentUserId: String
    ): Flow<List<SupabaseMessage>> = flow {
        try {
            val messages = postgrest.from("messages").select {
                filter { eq("conversation_id", conversationId) }
                order("sent_at", Order.ASCENDING)
            }.decodeList<SupabaseMessage>()

            // Filter out soft-deleted messages client-side
            emit(messages.filter { isVisibleToUser(it, currentUserId) })
        } catch (_: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    // ==================== SEND MESSAGE ====================

    suspend fun sendMessage(
        senderId: String,
        receiverId: String,
        content: String,
        mediaUri: Uri?,
        mediaType: String // TEXT, IMAGE, VIDEO, ONE_SHOT_IMAGE, ONE_SHOT_VIDEO
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            // 1. Get or create conversation via RPC
            val rpcResult = postgrest.rpc(
                "get_or_create_conversation",
                mapOf("user_a" to senderId, "user_b" to receiverId)
            )
            val conversationId = rpcResult.decodeSingleOrNull<Map<String, String>>()
                ?.values?.firstOrNull()
                ?: return@withContext Pair(false, "Failed to get or create conversation.")

            // 2. Upload media to Supabase Storage if present
            var mediaReference: String? = null
            if (mediaUri != null) {
                try {
                    val bytes = appContext.contentResolver.openInputStream(mediaUri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        val ext = if (mediaType.contains("VIDEO")) "mp4" else "jpg"
                        val filename = "msg_${System.currentTimeMillis()}_${(100..999).random()}.$ext"
                        val storagePath = "$senderId/$filename"
                        storage.from("chat-media").upload(storagePath, bytes)
                        mediaReference = storagePath
                    }
                } catch (e: Exception) {
                    return@withContext Pair(false, "Failed to upload media: ${e.localizedMessage}")
                }
            }

            // 3. Insert the message
            val insertRow = MessageInsert(
                conversation_id = conversationId,
                sender_id = senderId,
                message_type = mediaType,
                content = if (content.isEmpty()) null else content,
                media_reference = mediaReference,
                one_shot = mediaType.startsWith("ONE_SHOT"),
                status = "SENT",
                sent_at = java.time.Instant.now().toString()
            )
            postgrest.from("messages").insert(insertRow)

            return@withContext Pair(true, null)
        } catch (e: Exception) {
            return@withContext Pair(false, "Failed to send message: ${e.localizedMessage}")
        }
    }

    // ==================== MARK SEEN ====================

    suspend fun markConversationSeen(conversationId: String, userId: String) =
        withContext(Dispatchers.IO) {
            try {
                postgrest.from("messages").update({
                    set("seen_at", java.time.Instant.now().toString())
                    set("status", "SEEN")
                }) {
                    filter {
                        eq("conversation_id", conversationId)
                        neq("sender_id", userId)
                        isNull("seen_at")
                    }
                }
            } catch (_: Exception) {}
        }

    // ==================== ONE SHOT ====================

    suspend fun markOneShotOpened(messageId: String) = withContext(Dispatchers.IO) {
        try {
            postgrest.from("messages").update({
                set("one_shot_opened", true)
                set("one_shot_opened_at", java.time.Instant.now().toString())
            }) {
                filter { eq("id", messageId) }
            }
        } catch (_: Exception) {}
    }

    // ==================== CLEAR CHAT ====================

    suspend fun clearChatForMe(conversationId: String, userId: String) =
        withContext(Dispatchers.IO) {
            try {
                postgrest.rpc(
                    "soft_clear_conversation_for_user",
                    mapOf("conversation_id" to conversationId, "user_id" to userId)
                )
            } catch (_: Exception) {}
        }

    suspend fun clearChatForBoth(
        conversationId: String,
        userId: String,
        partnerId: String
    ) = withContext(Dispatchers.IO) {
        try {
            // Clear for the current user
            postgrest.rpc(
                "soft_clear_conversation_for_user",
                mapOf("conversation_id" to conversationId, "user_id" to userId)
            )
            // Clear for the partner
            postgrest.rpc(
                "soft_clear_conversation_for_user",
                mapOf("conversation_id" to conversationId, "user_id" to partnerId)
            )
        } catch (_: Exception) {}
    }

    // ==================== SECURE CHAT SETTINGS ====================

    fun getSecureChatModeFlow(conversationId: String): Flow<Boolean> = flow {
        try {
            val settings = postgrest.from("chat_settings").select {
                filter { eq("conversation_id", conversationId) }
            }.decodeList<ChatSettingsRow>()
            emit(settings.firstOrNull()?.is_secure_chat_enabled ?: false)
        } catch (_: Exception) {
            emit(false)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun setSecureChatMode(conversationId: String, enabled: Boolean) =
        withContext(Dispatchers.IO) {
            try {
                // Check if settings row already exists
                val existing = postgrest.from("chat_settings").select {
                    filter { eq("conversation_id", conversationId) }
                }.decodeList<ChatSettingsRow>()

                if (existing.isNotEmpty()) {
                    postgrest.from("chat_settings").update({
                        set("is_secure_chat_enabled", enabled)
                    }) {
                        filter { eq("conversation_id", conversationId) }
                    }
                } else {
                    postgrest.from("chat_settings").insert(
                        ChatSettingsInsert(
                            conversation_id = conversationId,
                            is_secure_chat_enabled = enabled
                        )
                    )
                }
            } catch (_: Exception) {}
        }

    // ==================== MEDIA URLS ====================

    /**
     * Generates a signed URL for a chat media file stored in the 'chat-media' bucket.
     * @param storagePath The storage path (e.g. "userId/msg_123.jpg") stored in [SupabaseMessage.media_reference].
     * @param expirySeconds How long the signed URL is valid for (default 1 hour).
     */
    suspend fun getMediaSignedUrl(storagePath: String, expirySeconds: Long = 3600): String? =
        withContext(Dispatchers.IO) {
            try {
                storage.from("chat-media").createSignedUrl(storagePath, expirySeconds)
            } catch (_: Exception) { null }
        }
}
