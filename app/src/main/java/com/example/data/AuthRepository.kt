package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.data.local.CleanShieldDatabase
import com.example.data.local.OtpEntity
import com.example.data.local.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.random.Random

data class ActiveUserSession(
    val id: Long = 0,
    val username: String = "",
    val name: String = "",
    val bio: String = "",
    val profilePhotoUri: String? = null,
    val email: String = "",
    val isVerified: Boolean = false,
    val avatarColorHex: Long = 0xFF00E5FF,
    val statusMessage: String = "Active & Encrypted",
    val partnerUsername: String? = null
)

sealed class UsernameValidationResult {
    data object Valid : UsernameValidationResult()
    data class Invalid(val reason: String) : UsernameValidationResult()
}

sealed class AuthResult {
    data class Success(val user: ActiveUserSession) : AuthResult()
    data class RequiresVerification(val email: String, val username: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class OtpVerificationResult {
    data class Success(val user: ActiveUserSession) : OtpVerificationResult()
    data class Error(val message: String) : OtpVerificationResult()
}

class AuthRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("clean_shield_auth_secure_prefs", Context.MODE_PRIVATE)

    val database: CleanShieldDatabase = Room.databaseBuilder(
        appContext,
        CleanShieldDatabase::class.java,
        "clean_shield_secure.db"
    ).fallbackToDestructiveMigration().build()

    private val userDao = database.userDao()
    private val otpDao = database.otpDao()

    private val _currentSession = MutableStateFlow<ActiveUserSession?>(loadSavedSession())
    val currentSession: StateFlow<ActiveUserSession?> = _currentSession.asStateFlow()

    // Last generated OTP for direct in-app simulation preview / notification banner
    private val _latestDispatchedOtp = MutableStateFlow<Pair<String, String>?>(null) // (email, otp)
    val latestDispatchedOtp: StateFlow<Pair<String, String>?> = _latestDispatchedOtp.asStateFlow()

    // Cooldown tracker for resending OTP (stores timestamp of last send per email)
    private val lastOtpSendTimes = mutableMapOf<String, Long>()

    private fun loadSavedSession(): ActiveUserSession? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null
        val id = prefs.getLong(KEY_SESSION_USER_ID, -1L)
        val username = prefs.getString(KEY_SESSION_USERNAME, null) ?: return null
        val name = prefs.getString(KEY_SESSION_NAME, "") ?: ""
        val bio = prefs.getString(KEY_SESSION_BIO, "") ?: ""
        val photoUri = prefs.getString(KEY_SESSION_PHOTO_URI, null)
        val email = prefs.getString(KEY_SESSION_EMAIL, null) ?: return null
        val isVerified = prefs.getBoolean(KEY_SESSION_VERIFIED, false)
        val avatarColor = prefs.getLong(KEY_SESSION_AVATAR_COLOR, 0xFF00E5FF)
        val status = prefs.getString(KEY_SESSION_STATUS, "Active & Encrypted") ?: "Active & Encrypted"
        val partner = prefs.getString(KEY_SESSION_PARTNER, null)

        return ActiveUserSession(
            id = id,
            username = username,
            name = name,
            bio = bio,
            profilePhotoUri = photoUri,
            email = email,
            isVerified = isVerified,
            avatarColorHex = avatarColor,
            statusMessage = status,
            partnerUsername = partner
        )
    }

    // --- PIN MANAGEMENT ---

    fun hasAccessPin(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.getString(KEY_PIN_HASH, null) != null
    }

    fun saveAccessPin(pin: String): Pair<Boolean, String?> {
        val validation = validatePin(pin)
        if (validation != null) {
            return Pair(false, validation)
        }
        val salt = generateSalt()
        val hash = hashPasswordWithSalt(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .apply()
        return Pair(true, null)
    }

    fun verifyAccessPin(pin: String): Boolean {
        if (pin.length != 6) return false
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val savedSalt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        val calculatedHash = hashPasswordWithSalt(pin, savedSalt)
        return savedHash == calculatedHash
    }

    fun validatePin(pin: String): String? {
        if (pin.length != 6) return "PIN must be exactly 6 digits."
        if (!pin.all { it.isDigit() }) return "PIN must contain only numbers."

        // Check all identical digits (e.g. 000000, 111111)
        if (pin.all { it == pin[0] }) {
            return "PIN cannot contain 6 identical numbers."
        }

        // Check sequential numbers (e.g. 123456, 654321, 012345)
        val isAscending = pin.zipWithNext().all { (a, b) -> b.code - a.code == 1 }
        val isDescending = pin.zipWithNext().all { (a, b) -> a.code - b.code == 1 }
        if (isAscending || isDescending) {
            return "PIN cannot be an obvious sequence (e.g. 123456)."
        }

        return null
    }

    // --- USERNAME VALIDATION (Instagram Format) ---

    suspend fun validateUsername(username: String): UsernameValidationResult = withContext(Dispatchers.IO) {
        val trimmed = username.trim().lowercase()

        if (trimmed.length < 3) {
            return@withContext UsernameValidationResult.Invalid("Username must be at least 3 characters.")
        }
        if (trimmed.length > 30) {
            return@withContext UsernameValidationResult.Invalid("Username cannot exceed 30 characters.")
        }

        // Instagram rules: only lowercase letters, numbers, periods, and underscores
        val instagramRegex = "^[a-z0-9._]+$".toRegex()
        if (!trimmed.matches(instagramRegex)) {
            return@withContext UsernameValidationResult.Invalid("Only lowercase letters, numbers, underscores, and periods are allowed.")
        }

        if (trimmed.startsWith(".")) {
            return@withContext UsernameValidationResult.Invalid("Username cannot start with a period.")
        }
        if (trimmed.endsWith(".")) {
            return@withContext UsernameValidationResult.Invalid("Username cannot end with a period.")
        }
        if (trimmed.contains("..")) {
            return@withContext UsernameValidationResult.Invalid("Username cannot contain consecutive periods.")
        }

        // Check global uniqueness in database
        val count = userDao.countUsername(trimmed)
        if (count > 0) {
            return@withContext UsernameValidationResult.Invalid("Username is already taken. Please choose another.")
        }

        return@withContext UsernameValidationResult.Valid
    }

    suspend fun validateUsernameForProfile(username: String, currentUserId: Long): UsernameValidationResult = withContext(Dispatchers.IO) {
        val trimmed = username.trim().lowercase()

        if (trimmed.length < 3) {
            return@withContext UsernameValidationResult.Invalid("Username must be at least 3 characters.")
        }
        if (trimmed.length > 30) {
            return@withContext UsernameValidationResult.Invalid("Username cannot exceed 30 characters.")
        }

        // Instagram rules: only lowercase letters, numbers, periods, and underscores
        val instagramRegex = "^[a-z0-9._]+$".toRegex()
        if (!trimmed.matches(instagramRegex)) {
            return@withContext UsernameValidationResult.Invalid("Only lowercase letters, numbers, underscores, and periods are allowed.")
        }

        if (trimmed.startsWith(".")) {
            return@withContext UsernameValidationResult.Invalid("Username cannot start with a period.")
        }
        if (trimmed.endsWith(".")) {
            return@withContext UsernameValidationResult.Invalid("Username cannot end with a period.")
        }
        if (trimmed.contains("..")) {
            return@withContext UsernameValidationResult.Invalid("Username cannot contain consecutive periods.")
        }

        // Check uniqueness excluding current user
        val count = userDao.countUsernameExcludingUser(trimmed, currentUserId)
        if (count > 0) {
            return@withContext UsernameValidationResult.Invalid("Username is already taken. Please choose another.")
        }

        return@withContext UsernameValidationResult.Valid
    }

    suspend fun updateUserProfile(
        userId: Long,
        username: String,
        name: String,
        bio: String,
        photoUri: String?
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val trimmedUsername = username.trim().lowercase()
            val trimmedName = name.trim()
            val trimmedBio = bio.trim()

            val usernameValidation = validateUsernameForProfile(trimmedUsername, userId)
            if (usernameValidation is UsernameValidationResult.Invalid) {
                return@withContext Pair(false, usernameValidation.reason)
            }

            if (trimmedName.isEmpty()) {
                return@withContext Pair(false, "Name cannot be empty.")
            }

            userDao.updateProfile(
                userId = userId,
                username = trimmedUsername,
                name = trimmedName,
                bio = trimmedBio,
                photoUri = photoUri
            )

            // Update active session if currently logged in
            val current = _currentSession.value
            if (current != null && current.id == userId) {
                val updatedSession = current.copy(
                    username = trimmedUsername,
                    name = trimmedName,
                    bio = trimmedBio,
                    profilePhotoUri = photoUri
                )
                saveActiveSession(updatedSession)
            }

            return@withContext Pair(true, null)
        } catch (e: Exception) {
            return@withContext Pair(false, "Failed to save profile changes: ${e.localizedMessage}")
        }
    }

    // --- ACCOUNT CREATION & OTP ---

    suspend fun createAccount(
        usernameInput: String,
        emailInput: String,
        passwordInput: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            val username = usernameInput.trim().lowercase()
            val email = emailInput.trim().lowercase()

            // Validate username
            val usernameCheck = validateUsername(username)
            if (usernameCheck is UsernameValidationResult.Invalid) {
                return@withContext AuthResult.Error(usernameCheck.reason)
            }

            // Validate email format
            val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
            if (!email.matches(emailRegex)) {
                return@withContext AuthResult.Error("Please enter a valid email address.")
            }

            // Check if email already registered
            val emailCount = userDao.countEmail(email)
            if (emailCount > 0) {
                val existing = userDao.getUserByEmail(email)
                if (existing != null && existing.isEmailVerified) {
                    return@withContext AuthResult.Error("An account with this email already exists. Please Sign In.")
                }
            }

            // Validate password
            if (passwordInput.length < 6) {
                return@withContext AuthResult.Error("Password must be at least 6 characters.")
            }

            // Hash password
            val salt = generateSalt()
            val passwordHash = hashPasswordWithSalt(passwordInput, salt)

            // Random vibrant avatar color
            val avatarColors = listOf(
                0xFF00E5FF, 0xFF00E676, 0xFF7C4DFF, 0xFFFF4081, 0xFFFFAB00, 0xFF00B0FF
            )
            val chosenColor = avatarColors[Random.nextInt(avatarColors.size)]

            // Check if unverified user already exists with this email, update or insert
            val existingUser = userDao.getUserByEmail(email)
            val userId = if (existingUser != null) {
                userDao.updateUser(
                    existingUser.copy(
                        username = username,
                        passwordHash = passwordHash,
                        salt = salt,
                        avatarColorHex = chosenColor
                    )
                )
                existingUser.id
            } else {
                val newUser = UserEntity(
                    username = username,
                    email = email,
                    passwordHash = passwordHash,
                    salt = salt,
                    isEmailVerified = false,
                    avatarColorHex = chosenColor
                )
                userDao.insertUser(newUser)
            }

            // Generate and dispatch 6-digit OTP
            val otpCode = generateAndSaveOtp(email, "SIGNUP")

            return@withContext AuthResult.RequiresVerification(
                email = email,
                username = username
            )
        } catch (e: Exception) {
            return@withContext AuthResult.Error("Account creation failed. Please check your details and try again.")
        }
    }

    suspend fun sendPasswordResetOtp(emailInput: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        val user = userDao.getUserByEmail(email) ?: return@withContext Pair(false, "No registered account found with this email.")

        val otp = generateAndSaveOtp(email, "RESET")
        return@withContext Pair(true, "A 6-digit reset code has been sent to $email.")
    }

    private suspend fun generateAndSaveOtp(email: String, type: String): String {
        val code = String.format("%06d", Random.nextInt(1000000))
        val expiresAt = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes validity

        val otpEntity = OtpEntity(
            email = email,
            otpCode = code,
            type = type,
            expiresAt = expiresAt,
            attemptsCount = 0,
            isUsed = false
        )
        otpDao.insertOtp(otpEntity)
        lastOtpSendTimes[email] = System.currentTimeMillis()

        // Post to flow for instant testing display/toast
        _latestDispatchedOtp.value = Pair(email, code)
        return code
    }

    fun getResendCooldownSeconds(email: String): Int {
        val lastSend = lastOtpSendTimes[email.trim().lowercase()] ?: return 0
        val elapsed = (System.currentTimeMillis() - lastSend) / 1000
        val remaining = 60 - elapsed
        return if (remaining > 0) remaining.toInt() else 0
    }

    suspend fun resendOtp(emailInput: String, type: String = "SIGNUP"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        val cooldown = getResendCooldownSeconds(email)
        if (cooldown > 0) {
            return@withContext Pair(false, "Please wait $cooldown seconds before requesting a new code.")
        }

        val code = generateAndSaveOtp(email, type)
        return@withContext Pair(true, "A new 6-digit verification code was sent to $email.")
    }

    suspend fun verifySignupOtp(emailInput: String, otpInput: String): OtpVerificationResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        val code = otpInput.trim()

        if (code.length != 6 || !code.all { it.isDigit() }) {
            return@withContext OtpVerificationResult.Error("Please enter a valid 6-digit verification code.")
        }

        val activeOtp = otpDao.getLatestActiveOtp(email, "SIGNUP")
            ?: return@withContext OtpVerificationResult.Error("No active verification code found. Please request a new code.")

        if (System.currentTimeMillis() > activeOtp.expiresAt) {
            return@withContext OtpVerificationResult.Error("Verification code has expired. Please request a new code.")
        }

        if (activeOtp.attemptsCount >= 5) {
            return@withContext OtpVerificationResult.Error("Too many incorrect attempts. Please request a new code.")
        }

        if (activeOtp.otpCode != code) {
            otpDao.incrementAttempts(activeOtp.id)
            val remaining = 4 - activeOtp.attemptsCount
            return@withContext OtpVerificationResult.Error(
                if (remaining > 0) "Incorrect verification code. $remaining attempts remaining."
                else "Incorrect code. Please request a new code."
            )
        }

        // Success! Mark OTP used
        otpDao.markOtpUsed(activeOtp.id)

        // Mark user email verified
        userDao.markEmailVerified(email)
        val user = userDao.getUserByEmail(email)
            ?: return@withContext OtpVerificationResult.Error("User record not found.")

        // Create active session
        val session = ActiveUserSession(
            id = user.id,
            username = user.username,
            name = user.name,
            bio = user.bio,
            profilePhotoUri = user.profilePhotoUri,
            email = user.email,
            isVerified = true,
            avatarColorHex = user.avatarColorHex,
            statusMessage = user.statusMessage,
            partnerUsername = user.partnerUsername
        )
        saveActiveSession(session)

        return@withContext OtpVerificationResult.Success(session)
    }

    suspend fun verifyPasswordResetOtp(emailInput: String, otpInput: String, newPasswordInput: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        val code = otpInput.trim()

        if (code.length != 6 || !code.all { it.isDigit() }) {
            return@withContext Pair(false, "Please enter a valid 6-digit reset code.")
        }
        if (newPasswordInput.length < 6) {
            return@withContext Pair(false, "New password must be at least 6 characters.")
        }

        val activeOtp = otpDao.getLatestActiveOtp(email, "RESET")
            ?: return@withContext Pair(false, "No active reset code found. Please request a new code.")

        if (System.currentTimeMillis() > activeOtp.expiresAt) {
            return@withContext Pair(false, "Reset code has expired. Please request a new code.")
        }

        if (activeOtp.otpCode != code) {
            otpDao.incrementAttempts(activeOtp.id)
            return@withContext Pair(false, "Incorrect reset code.")
        }

        otpDao.markOtpUsed(activeOtp.id)
        val salt = generateSalt()
        val newHash = hashPasswordWithSalt(newPasswordInput, salt)
        userDao.updatePassword(email, newHash, salt)

        return@withContext Pair(true, "Password updated successfully. You may now sign in.")
    }

    // --- SIGN IN ---

    suspend fun signIn(identifierInput: String, passwordInput: String): AuthResult = withContext(Dispatchers.IO) {
        val identifier = identifierInput.trim().lowercase()

        if (identifier.isEmpty() || passwordInput.isEmpty()) {
            return@withContext AuthResult.Error("Please enter your username/email and password.")
        }

        val user = userDao.getUserByIdentifier(identifier)
            ?: return@withContext AuthResult.Error("Invalid username/email or password.")

        val calculatedHash = hashPasswordWithSalt(passwordInput, user.salt)
        if (calculatedHash != user.passwordHash) {
            return@withContext AuthResult.Error("Invalid username/email or password.")
        }

        if (!user.isEmailVerified) {
            // Generate verification OTP and require verification
            generateAndSaveOtp(user.email, "SIGNUP")
            return@withContext AuthResult.RequiresVerification(
                email = user.email,
                username = user.username
            )
        }

        val session = ActiveUserSession(
            id = user.id,
            username = user.username,
            name = user.name,
            bio = user.bio,
            profilePhotoUri = user.profilePhotoUri,
            email = user.email,
            isVerified = true,
            avatarColorHex = user.avatarColorHex,
            statusMessage = user.statusMessage,
            partnerUsername = user.partnerUsername
        )
        saveActiveSession(session)

        return@withContext AuthResult.Success(session)
    }

    fun signOut() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_SESSION_USER_ID)
            .remove(KEY_SESSION_USERNAME)
            .remove(KEY_SESSION_NAME)
            .remove(KEY_SESSION_BIO)
            .remove(KEY_SESSION_PHOTO_URI)
            .remove(KEY_SESSION_EMAIL)
            .remove(KEY_SESSION_VERIFIED)
            .remove(KEY_SESSION_AVATAR_COLOR)
            .remove(KEY_SESSION_STATUS)
            .remove(KEY_SESSION_PARTNER)
            .apply()
        _currentSession.value = null
    }

    private fun saveActiveSession(session: ActiveUserSession) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putLong(KEY_SESSION_USER_ID, session.id)
            .putString(KEY_SESSION_USERNAME, session.username)
            .putString(KEY_SESSION_NAME, session.name)
            .putString(KEY_SESSION_BIO, session.bio)
            .putString(KEY_SESSION_PHOTO_URI, session.profilePhotoUri)
            .putString(KEY_SESSION_EMAIL, session.email)
            .putBoolean(KEY_SESSION_VERIFIED, session.isVerified)
            .putLong(KEY_SESSION_AVATAR_COLOR, session.avatarColorHex)
            .putString(KEY_SESSION_STATUS, session.statusMessage)
            .putString(KEY_SESSION_PARTNER, session.partnerUsername)
            .apply()
        _currentSession.value = session
    }

    // --- CRYPTO UTILS ---

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    private fun hashPasswordWithSalt(password: String, salt: String): String {
        val combined = password + salt + "CLEAN_SHIELD_PEPPER_2026"
        val bytes = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "sec_pin_hash_v2"
        private const val KEY_PIN_SALT = "sec_pin_salt_v2"

        private const val KEY_IS_LOGGED_IN = "sec_is_logged_in"
        private const val KEY_SESSION_USER_ID = "sec_session_user_id"
        private const val KEY_SESSION_USERNAME = "sec_session_username"
        private const val KEY_SESSION_NAME = "sec_session_name"
        private const val KEY_SESSION_BIO = "sec_session_bio"
        private const val KEY_SESSION_PHOTO_URI = "sec_session_photo_uri"
        private const val KEY_SESSION_EMAIL = "sec_session_email"
        private const val KEY_SESSION_VERIFIED = "sec_session_verified"
        private const val KEY_SESSION_AVATAR_COLOR = "sec_session_avatar_color"
        private const val KEY_SESSION_STATUS = "sec_session_status"
        private const val KEY_SESSION_PARTNER = "sec_session_partner"

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
