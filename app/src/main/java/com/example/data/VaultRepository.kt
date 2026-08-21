package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.data.supabase.SupabaseClient
import io.github.jan-tennert.supabase.postgrest.from
import io.github.jan-tennert.supabase.postgrest.filter.eq
import io.github.jan-tennert.supabase.postgrest.filter.isNull
import io.github.jan-tennert.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom

// ==================== Data Classes ====================

/**
 * Mirrors the `vault_media` table in Supabase.
 * All nullable columns default to null so decoding never crashes on partial rows.
 */
@kotlinx.serialization.Serializable
data class SupabaseVaultMedia(
    val id: String = "",
    val user_id: String = "",
    val media_type: String = "IMAGE",
    val secure_storage_reference: String? = null,
    val title: String? = null,
    val file_size_bytes: Long? = null,
    val created_at: String? = null,
    val deleted_at: String? = null,
    val permanently_deleted_at: String? = null
) {
    /** Epoch millis derived from ISO-8601 created_at, for local sorting. */
    val createdAtMillis: Long
        get() = try {
            created_at?.let { java.time.Instant.parse(it).toEpochMilli() } ?: 0L
        } catch (_: Exception) { 0L }
}

/** Payload for inserting a new row (DB generates id, created_at, timestamps). */
@kotlinx.serialization.Serializable
private data class VaultMediaInsert(
    val user_id: String,
    val media_type: String,
    val secure_storage_reference: String,
    val title: String,
    val file_size_bytes: Long
)

// ==================== Repository ====================

class VaultRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val postgrest get() = SupabaseClient.postgrest
    private val storage get() = SupabaseClient.storage

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("clean_shield_vault_prefs", Context.MODE_PRIVATE)

    // Transient in-memory lock state per session
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: VaultRepository? = null

        fun getInstance(context: Context): VaultRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VaultRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        private const val KEY_VAULT_PIN_HASH = "vault_pin_hash"
        private const val KEY_VAULT_PIN_SALT = "vault_pin_salt"
    }

    // ==================== Vault Lock ====================

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    // ==================== Vault PIN ====================

    /**
     * Check whether the user has a vault PIN by querying the profiles table.
     */
    suspend fun hasVaultPin(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val row = postgrest.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<VaultPinRow>()
            !row?.vault_pin_hash.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Create a vault PIN: hash locally, persist in SharedPreferences for offline use,
     * and sync the hash/salt to the profiles table for cross-device support.
     */
    suspend fun createVaultPin(userId: String, pin: String): Pair<Boolean, String?> =
        withContext(Dispatchers.IO) {
            if (pin.length != 4 || !pin.all { it.isDigit() }) {
                return@withContext Pair(false, "Vault PIN must be exactly 4 digits.")
            }
            val salt = generateSalt()
            val hash = hashPin(pin, salt)

            // Save locally for fast offline verification
            prefs.edit()
                .putString(KEY_VAULT_PIN_HASH, hash)
                .putString(KEY_VAULT_PIN_SALT, salt)
                .apply()

            // Sync to profiles table for cross-device use
            try {
                postgrest.from("profiles").update({
                    set("vault_pin_hash", hash)
                    set("vault_pin_salt", salt)
                }) {
                    filter { eq("id", userId) }
                }
            } catch (_: Exception) {
                // Local save succeeded; remote sync is best-effort
            }

            _isVaultUnlocked.value = true
            return@withContext Pair(true, null)
        }

    /**
     * Verify a vault PIN against the locally stored hash.
     * Falls back to fetching from the profiles table if not cached locally.
     */
    suspend fun verifyVaultPin(userId: String, pin: String): Boolean =
        withContext(Dispatchers.IO) {
            if (pin.length != 4) return@withContext false

            val localHash = prefs.getString(KEY_VAULT_PIN_HASH, null)
            val localSalt = prefs.getString(KEY_VAULT_PIN_SALT, null)

            if (localHash != null && localSalt != null) {
                val matches = localHash == hashPin(pin, localSalt)
                if (matches) {
                    _isVaultUnlocked.value = true
                }
                return@withContext matches
            }

            // Fallback: fetch from profiles table and cache locally
            try {
                val row = postgrest.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<VaultPinRow>()

                val remoteHash = row?.vault_pin_hash ?: return@withContext false
                val remoteSalt = row.vault_pin_salt ?: return@withContext false

                // Cache for future fast verification
                prefs.edit()
                    .putString(KEY_VAULT_PIN_HASH, remoteHash)
                    .putString(KEY_VAULT_PIN_SALT, remoteSalt)
                    .apply()

                val matches = remoteHash == hashPin(pin, remoteSalt)
                if (matches) {
                    _isVaultUnlocked.value = true
                }
                return@withContext matches
            } catch (_: Exception) {
                return@withContext false
            }
        }

    // ==================== Vault Media ====================

    /**
     * Fetch all non-deleted vault media for the given user as a one-shot Flow.
     */
    fun getVaultMediaFlow(userId: String): Flow<List<SupabaseVaultMedia>> = flow {
        try {
            val media = postgrest.from("vault_media").select {
                filter {
                    eq("user_id", userId)
                    isNull("deleted_at")
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<SupabaseVaultMedia>()
            emit(media)
        } catch (_: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Upload media to Supabase Storage and insert a vault_media DB record.
     */
    suspend fun saveMediaToVault(
        userId: String,
        sourceUri: Uri,
        mediaType: String,
        title: String = ""
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            // 1. Read bytes from the source URI
            val bytes = appContext.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                return@withContext Pair(false, "Could not read the selected media file.")
            }

            // 2. Determine filename and storage path
            val extension = if (mediaType == "VIDEO") "mp4" else "jpg"
            val filename = "vault_${System.currentTimeMillis()}_${(1000..9999).random()}.$extension"
            val storagePath = "$userId/$filename"

            // 3. Upload to Supabase Storage
            storage.from("vault-media").upload(storagePath, bytes)

            // 4. Insert DB record
            val displayTitle = title.ifBlank {
                if (mediaType == "VIDEO") "Private Video" else "Private Photo"
            }
            postgrest.from("vault_media").insert(
                VaultMediaInsert(
                    user_id = userId,
                    media_type = mediaType,
                    secure_storage_reference = storagePath,
                    title = displayTitle,
                    file_size_bytes = bytes.size.toLong()
                )
            )

            return@withContext Pair(true, null)
        } catch (e: Exception) {
            return@withContext Pair(false, "Failed to import media: ${e.localizedMessage}")
        }
    }

    /**
     * Soft-delete vault media via RPC. The media file stays in Supabase Storage
     * and the DB row is kept for 30 days before permanent cleanup.
     */
    suspend fun deleteVaultMedia(
        mediaId: String,
        userId: String,
        storageReference: String
    ) = withContext(Dispatchers.IO) {
        try {
            postgrest.rpc(
                "soft_delete_vault_media",
                mapOf(
                    "p_vault_media_id" to mediaId,
                    "p_user_id" to userId
                )
            )
        } catch (_: Exception) {
            // Silently fail – the RPC handles idempotency
        }
    }

    /**
     * Generate a temporary signed URL for viewing vault media from Supabase Storage.
     * @param storagePath The storage path (e.g. "userId/vault_123.jpg") from [SupabaseVaultMedia.secure_storage_reference].
     * @param expirySeconds How long the signed URL is valid (default 1 hour).
     */
    suspend fun getVaultMediaSignedUrl(
        storagePath: String,
        expirySeconds: Long = 3600
    ): String? = withContext(Dispatchers.IO) {
        try {
            storage.from("vault-media").createSignedUrl(storagePath, expirySeconds)
        } catch (_: Exception) { null }
    }

    // ==================== Crypto Helpers ====================
    // Same SHA-256 + salt + pepper approach as AuthRepository.

    private fun generateSalt(): String =
        SecureRandom().generateSeed(16).joinToString("") { "%02x".format(it) }

    private fun hashPin(pin: String, salt: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest("$pin:$salt:CLEAN_SHIELD_VAULT_2026".toByteArray())
            .joinToString("") { "%02x".format(it) }

    // ==================== Internal: profiles row for vault PIN ====================

    @kotlinx.serialization.Serializable
    private data class VaultPinRow(
        val vault_pin_hash: String? = null,
        val vault_pin_salt: String? = null
    )

}
