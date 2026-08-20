package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.CleanShieldDatabase
import com.example.data.local.VaultMediaEntity
import com.example.data.local.VaultPinEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom

class VaultRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val authRepo = AuthRepository.getInstance(context)
    private val db: CleanShieldDatabase = authRepo.database
    private val vaultDao = db.vaultDao()

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
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    suspend fun hasVaultPin(username: String): Boolean = withContext(Dispatchers.IO) {
        val pin = vaultDao.getVaultPin(username.trim().lowercase())
        return@withContext pin != null
    }

    suspend fun createVaultPin(username: String, pin: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val user = username.trim().lowercase()
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            return@withContext Pair(false, "Vault PIN must be exactly 4 digits.")
        }
        val salt = generateSalt()
        val hash = hashPin(pin, salt)

        vaultDao.setVaultPin(
            VaultPinEntity(
                username = user,
                pinHash = hash,
                salt = salt,
                createdAt = System.currentTimeMillis()
            )
        )
        _isVaultUnlocked.value = true
        return@withContext Pair(true, null)
    }

    suspend fun verifyVaultPin(username: String, pin: String): Boolean = withContext(Dispatchers.IO) {
        val user = username.trim().lowercase()
        if (pin.length != 4) return@withContext false

        val saved = vaultDao.getVaultPin(user) ?: return@withContext false
        val calculated = hashPin(pin, saved.salt)
        val matches = (saved.pinHash == calculated)
        if (matches) {
            _isVaultUnlocked.value = true
        }
        return@withContext matches
    }

    fun getVaultMediaFlow(username: String): Flow<List<VaultMediaEntity>> {
        return vaultDao.getVaultMediaFlow(username.trim().lowercase()).flowOn(Dispatchers.IO)
    }

    suspend fun saveMediaToVault(
        username: String,
        sourceUri: Uri,
        mediaType: String,
        title: String = ""
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val user = username.trim().lowercase()
            val vaultDir = File(appContext.filesDir, "private_vault_$user")
            if (!vaultDir.exists()) {
                vaultDir.mkdirs()
            }

            val extension = if (mediaType == "VIDEO") "mp4" else "jpg"
            val fileName = "vault_${System.currentTimeMillis()}_${(1000..9999).random()}.$extension"
            val destFile = File(vaultDir, fileName)

            var fileSize = 0L
            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        fileSize += read
                    }
                    output.flush()
                }
            } ?: run {
                // If stream is not directly readable, store URI directly
                fileSize = 1024L * 1024L
            }

            val storedUri = if (destFile.exists() && destFile.length() > 0) {
                Uri.fromFile(destFile).toString()
            } else {
                sourceUri.toString()
            }

            vaultDao.insertVaultMedia(
                VaultMediaEntity(
                    ownerUsername = user,
                    mediaUri = storedUri,
                    mediaType = mediaType,
                    title = title.ifBlank { if (mediaType == "VIDEO") "Private Video" else "Private Photo" },
                    fileSizeBytes = if (destFile.exists()) destFile.length() else fileSize,
                    createdAt = System.currentTimeMillis()
                )
            )

            return@withContext Pair(true, null)
        } catch (e: Exception) {
            return@withContext Pair(false, "Failed to import media: ${e.localizedMessage}")
        }
    }

    suspend fun deleteVaultMedia(id: Long, mediaUri: String) = withContext(Dispatchers.IO) {
        try {
            vaultDao.deleteVaultMedia(id)
            if (mediaUri.startsWith("file://")) {
                val path = Uri.parse(mediaUri).path
                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val combined = "$pin:$salt:CLEAN_SHIELD_VAULT_2026"
        val bytes = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
