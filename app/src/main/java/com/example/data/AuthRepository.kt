package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.supabase.SupabaseClient
import io.github.jan-tennert.supabase.auth.auth
import io.github.jan-tennert.supabase.auth.providers.builtin.Email
import io.github.jan-tennert.supabase.postgrest.from
import io.github.jan-tennert.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import java.security.SecureRandom

// ---- Data Classes ----

data class ActiveUserSession(
    val id: String = "",
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

@kotlinx.serialization.Serializable
data class SupabaseProfile(
    val id: String = "",
    val username: String = "",
    val normalized_username: String = "",
    val email: String = "",
    val name: String = "",
    val bio: String = "",
    val profile_image: String? = null,
    val avatar_color_hex: Long? = null,
    val status_message: String = "Active & Encrypted",
    val partner_username: String? = null,
    val email_verified: Boolean = false,
    val created_at: String = "",
    val updated_at: String = ""
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

// ---- Repository ----

class AuthRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("clean_shield_auth_secure_prefs", Context.MODE_PRIVATE)

    private val sb get() = SupabaseClient.client
    private val postgrest get() = SupabaseClient.postgrest

    private val ktorClient = HttpClient(OkHttp)

    private val _currentSession = MutableStateFlow<ActiveUserSession?>(loadSavedSession())
    val currentSession: StateFlow<ActiveUserSession?> = _currentSession.asStateFlow()

    // No longer exposing local OTP - real OTPs sent via edge function
    private val _latestDispatchedOtp = MutableStateFlow<Pair<String, String>?>(null)
    val latestDispatchedOtp: StateFlow<Pair<String, String>?> = _latestDispatchedOtp.asStateFlow()

    private val lastOtpSendTimes = mutableMapOf<String, Long>()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            sb.auth.sessionStatus.collect { status ->
                when (status) {
                    is io.github.jan-tennert.supabase.auth.SessionStatus.Authenticated -> {
                        status.session.user?.id?.let { loadSessionFromProfile(it) }
                    }
                    is io.github.jan-tennert.supabase.auth.SessionStatus.NotAuthenticated -> {
                        _currentSession.value = null
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadSavedSession(): ActiveUserSession? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null
        val id = prefs.getString(KEY_SESSION_USER_ID, null) ?: return null
        val username = prefs.getString(KEY_SESSION_USERNAME, null) ?: return null
        return ActiveUserSession(
            id = id, username = username,
            name = prefs.getString(KEY_SESSION_NAME, "") ?: "",
            bio = prefs.getString(KEY_SESSION_BIO, "") ?: "",
            profilePhotoUri = prefs.getString(KEY_SESSION_PHOTO_URI, null),
            email = prefs.getString(KEY_SESSION_EMAIL, null) ?: return null,
            isVerified = prefs.getBoolean(KEY_SESSION_VERIFIED, false),
            avatarColorHex = prefs.getLong(KEY_SESSION_AVATAR_COLOR, 0xFF00E5FF),
            statusMessage = prefs.getString(KEY_SESSION_STATUS, "Active & Encrypted") ?: "Active & Encrypted",
            partnerUsername = prefs.getString(KEY_SESSION_PARTNER, null)
        )
    }

    private suspend fun loadSessionFromProfile(userId: String) = withContext(Dispatchers.IO) {
        try {
            val profile = postgrest.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<SupabaseProfile>()
            if (profile != null) {
                saveActiveSession(ActiveUserSession(
                    id = profile.id, username = profile.username, name = profile.name,
                    bio = profile.bio, profilePhotoUri = profile.profile_image,
                    email = profile.email, isVerified = profile.email_verified,
                    avatarColorHex = profile.avatar_color_hex ?: 0xFF00E5FF,
                    statusMessage = profile.status_message, partnerUsername = profile.partner_username
                ))
            }
        } catch (_: Exception) {}
    }

    // ---- PIN (local only, never sent to server) ----

    fun hasAccessPin(): Boolean = prefs.contains(KEY_PIN_HASH) && prefs.getString(KEY_PIN_HASH, null) != null

    fun saveAccessPin(pin: String): Pair<Boolean, String?> {
        validatePin(pin)?.let { return Pair(false, it) }
        val salt = generateSalt()
        prefs.edit().putString(KEY_PIN_HASH, hashPasswordWithSalt(pin, salt)).putString(KEY_PIN_SALT, salt).apply()
        return Pair(true, null)
    }

    fun verifyAccessPin(pin: String): Boolean {
        if (pin.length != 6) return false
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val savedSalt = prefs.getString(KEY_PIN_SALT, "") ?: ""
        return savedHash == hashPasswordWithSalt(pin, savedSalt)
    }

    fun validatePin(pin: String): String? {
        if (pin.length != 6) return "PIN must be exactly 6 digits."
        if (!pin.all { it.isDigit() }) return "PIN must contain only numbers."
        if (pin.all { it == pin[0] }) return "PIN cannot contain 6 identical numbers."
        if (pin.zipWithNext().all { (a, b) -> b.code - a.code == 1 } || pin.zipWithNext().all { (a, b) -> a.code - b.code == 1 })
            return "PIN cannot be an obvious sequence."
        return null
    }

    // ---- USERNAME VALIDATION (server-side) ----

    suspend fun validateUsername(username: String): UsernameValidationResult = withContext(Dispatchers.IO) {
        val trimmed = username.trim().lowercase()
        if (trimmed.length < 3) return@withContext UsernameValidationResult.Invalid("Username must be at least 3 characters.")
        if (trimmed.length > 30) return@withContext UsernameValidationResult.Invalid("Username cannot exceed 30 characters.")
        if (!"^[a-z0-9._]+$".toRegex().matches(trimmed)) return@withContext UsernameValidationResult.Invalid("Only lowercase letters, numbers, underscores, and periods allowed.")
        if (trimmed.startsWith(".") || trimmed.endsWith(".") || trimmed.contains(".."))
            return@withContext UsernameValidationResult.Invalid("Invalid username format.")
        val count = postgrest.from("profiles").select { filter { eq("normalized_username", trimmed) } }.decodeList<SupabaseProfile>().size
        if (count > 0) return@withContext UsernameValidationResult.Invalid("Username is already taken.")
        return@withContext UsernameValidationResult.Valid
    }

    suspend fun validateUsernameForProfile(username: String, currentUserId: String): UsernameValidationResult = withContext(Dispatchers.IO) {
        val trimmed = username.trim().lowercase()
        if (trimmed.length < 3) return@withContext UsernameValidationResult.Invalid("Username must be at least 3 characters.")
        if (trimmed.length > 30) return@withContext UsernameValidationResult.Invalid("Username cannot exceed 30 characters.")
        if (!"^[a-z0-9._]+$".toRegex().matches(trimmed)) return@withContext UsernameValidationResult.Invalid("Only lowercase letters, numbers, underscores, and periods allowed.")
        if (trimmed.startsWith(".") || trimmed.endsWith(".") || trimmed.contains(".."))
            return@withContext UsernameValidationResult.Invalid("Invalid username format.")
        val count = postgrest.from("profiles").select { filter { eq("normalized_username", trimmed); neq("id", currentUserId) } }.decodeList<SupabaseProfile>().size
        if (count > 0) return@withContext UsernameValidationResult.Invalid("Username is already taken.")
        return@withContext UsernameValidationResult.Valid
    }

    suspend fun updateUserProfile(userId: String, username: String, name: String, bio: String, photoUri: String?): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val uv = validateUsernameForProfile(username.trim().lowercase(), userId)
            if (uv is UsernameValidationResult.Invalid) return@withContext Pair(false, uv.reason)
            if (name.trim().isEmpty()) return@withContext Pair(false, "Name cannot be empty.")
            postgrest.from("profiles").update({
                set("username", username.trim().lowercase())
                set("name", name.trim())
                set("bio", bio.trim())
                set("profile_image", photoUri)
                set("updated_at", java.time.Instant.now().toString())
            }) { filter { eq("id", userId) } }
            _currentSession.value?.let { cur ->
                if (cur.id == userId) saveActiveSession(cur.copy(
                    username = username.trim().lowercase(), name = name.trim(), bio = bio.trim(), profilePhotoUri = photoUri
                ))
            }
            return@withContext Pair(true, null)
        } catch (e: Exception) {
            return@withContext Pair(false, "Failed: ${e.localizedMessage}")
        }
    }

    // ---- ACCOUNT CREATION (Supabase Auth + Edge Function 6-digit OTP) ----

    suspend fun createAccount(usernameInput: String, emailInput: String, passwordInput: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val username = usernameInput.trim().lowercase()
            val email = emailInput.trim().lowercase()
            val usernameCheck = validateUsername(username)
            if (usernameCheck is UsernameValidationResult.Invalid) return@withContext AuthResult.Error(usernameCheck.reason)
            if (!"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex().matches(emailInput))
                return@withContext AuthResult.Error("Please enter a valid email address.")
            if (passwordInput.length < 6) return@withContext AuthResult.Error("Password must be at least 6 characters.")
            val existing = postgrest.from("profiles").select { filter { eq("email", email) } }.decodeList<SupabaseProfile>()
            if (existing.any { it.email_verified }) return@withContext AuthResult.Error("An account with this email already exists. Please Sign In.")
            // Sign up via Supabase Auth
            try { sb.auth.signUpWith(Email) { this.email = email; password = passwordInput; data = buildMap { put("username", username); put("name", "") } } } catch (_: Exception) {}
            // Send 6-digit OTP via Edge Function (NEVER link-based)
            sendOtpViaEdgeFunction(email, "SIGNUP")
            return@withContext AuthResult.RequiresVerification(email = email, username = username)
        } catch (e: Exception) {
            return@withContext AuthResult.Error("Account creation failed: ${e.localizedMessage}")
        }
    }

    private suspend fun sendOtpViaEdgeFunction(email: String, type: String): Boolean {
        return try {
            val response = ktorClient.post("${BuildConfig.SUPABASE_URL}/functions/v1/send-otp") {
                headers { append("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}") }
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "type" to type))
            }
            lastOtpSendTimes[email] = System.currentTimeMillis()
            response.status == HttpStatusCode.OK
        } catch (_: Exception) { false }
    }

    suspend fun sendPasswordResetOtp(emailInput: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        val profiles = postgrest.from("profiles").select { filter { eq("email", email) } }.decodeList<SupabaseProfile>()
        if (profiles.isEmpty()) return@withContext Pair(false, "No registered account found with this email.")
        sendOtpViaEdgeFunction(email, "RESET")
        return@withContext Pair(true, "A 6-digit reset code has been sent to $email.")
    }

    fun getResendCooldownSeconds(email: String): Int {
        val lastSend = lastOtpSendTimes[email.trim().lowercase()] ?: return 0
        return maxOf(0, 60 - ((System.currentTimeMillis() - lastSend) / 1000).toInt())
    }

    suspend fun resendOtp(emailInput: String, type: String = "SIGNUP"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        val cooldown = getResendCooldownSeconds(email)
        if (cooldown > 0) return@withContext Pair(false, "Please wait $cooldown seconds before requesting a new code.")
        sendOtpViaEdgeFunction(email, type)
        return@withContext Pair(true, "A new 6-digit verification code was sent to $email.")
    }

    suspend fun verifySignupOtp(emailInput: String, otpInput: String): OtpVerificationResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        val code = otpInput.trim()
        if (code.length != 6 || !code.all { it.isDigit() }) return@withContext OtpVerificationResult.Error("Please enter a valid 6-digit verification code.")
        try {
            val response = ktorClient.post("${BuildConfig.SUPABASE_URL}/functions/v1/verify-otp") {
                headers { append("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}") }
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "otp_code" to code, "type" to "SIGNUP"))
            }
            val body = response.body<String>()
            val json = Json.parseToJsonElement(body).jsonObject
            if (json["success"]?.jsonPrimitive?.booleanOrNull != true) {
                return@withContext OtpVerificationResult.Error(json["error"]?.jsonPrimitive?.contentOrNull ?: "Verification failed.")
            }
            postgrest.from("profiles").update({ set("email_verified", true); set("updated_at", java.time.Instant.now().toString()) }) {
                filter { eq("email", email) }
            }
            val profile = postgrest.from("profiles").select { filter { eq("email", email) } }.decodeSingleOrNull<SupabaseProfile>()
                ?: return@withContext OtpVerificationResult.Error("User record not found.")
            val session = ActiveUserSession(id = profile.id, username = profile.username, name = profile.name, bio = profile.bio, profilePhotoUri = profile.profile_image, email = profile.email, isVerified = true, avatarColorHex = profile.avatar_color_hex ?: 0xFF00E5FF, statusMessage = profile.status_message, partnerUsername = profile.partner_username)
            saveActiveSession(session)
            return@withContext OtpVerificationResult.Success(session)
        } catch (e: Exception) {
            return@withContext OtpVerificationResult.Error("Verification failed: ${e.localizedMessage}")
        }
    }

    suspend fun verifyPasswordResetOtp(emailInput: String, otpInput: String, newPasswordInput: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        val code = otpInput.trim()
        if (code.length != 6 || !code.all { it.isDigit() }) return@withContext Pair(false, "Please enter a valid 6-digit reset code.")
        if (newPasswordInput.length < 6) return@withContext Pair(false, "New password must be at least 6 characters.")
        try {
            val response = ktorClient.post("${BuildConfig.SUPABASE_URL}/functions/v1/verify-otp") {
                headers { append("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}") }
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "otp_code" to code, "type" to "RESET"))
            }
            val body = response.body<String>()
            val json = Json.parseToJsonElement(body).jsonObject
            if (json["success"]?.jsonPrimitive?.booleanOrNull != true) {
                return@withContext Pair(false, json["error"]?.jsonPrimitive?.contentOrNull ?: "Invalid code.")
            }
            sb.auth.updateUser { password = newPasswordInput }
            return@withContext Pair(true, "Password updated. You may now sign in.")
        } catch (e: Exception) {
            return@withContext Pair(false, "Failed: ${e.localizedMessage}")
        }
    }

    // ---- SIGN IN ----

    suspend fun signIn(identifierInput: String, passwordInput: String): AuthResult = withContext(Dispatchers.IO) {
        val identifier = identifierInput.trim().lowercase()
        if (identifier.isEmpty() || passwordInput.isEmpty()) return@withContext AuthResult.Error("Please enter your username/email and password.")
        try {
            val email = if (identifier.contains("@")) identifier else {
                postgrest.from("profiles").select { filter { eq("normalized_username", identifier) } }.decodeSingleOrNull<SupabaseProfile>()?.email
                    ?: return@withContext AuthResult.Error("Invalid username/email or password.")
            }
            sb.auth.signInWith(Email) { this.email = email; password = passwordInput }
            val profile = postgrest.from("profiles").select { filter { eq("email", email) } }.decodeSingleOrNull<SupabaseProfile>()
                ?: return@withContext AuthResult.Error("Invalid username/email or password.")
            if (!profile.email_verified) {
                sendOtpViaEdgeFunction(email, "SIGNUP")
                return@withContext AuthResult.RequiresVerification(email = email, username = profile.username)
            }
            val session = ActiveUserSession(id = profile.id, username = profile.username, name = profile.name, bio = profile.bio, profilePhotoUri = profile.profile_image, email = profile.email, isVerified = true, avatarColorHex = profile.avatar_color_hex ?: 0xFF00E5FF, statusMessage = profile.status_message, partnerUsername = profile.partner_username)
            saveActiveSession(session)
            return@withContext AuthResult.Success(session)
        } catch (_: Exception) {
            return@withContext AuthResult.Error("Invalid username/email or password.")
        }
    }

    fun signOut() {
        try { CoroutineScope(Dispatchers.IO).launch { sb.auth.signOut() } } catch (_: Exception) {}
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_SESSION_USER_ID).remove(KEY_SESSION_USERNAME).remove(KEY_SESSION_NAME)
            .remove(KEY_SESSION_BIO).remove(KEY_SESSION_PHOTO_URI).remove(KEY_SESSION_EMAIL)
            .remove(KEY_SESSION_VERIFIED).remove(KEY_SESSION_AVATAR_COLOR)
            .remove(KEY_SESSION_STATUS).remove(KEY_SESSION_PARTNER).apply()
        _currentSession.value = null
    }

    private fun saveActiveSession(session: ActiveUserSession) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, true).putString(KEY_SESSION_USER_ID, session.id)
            .putString(KEY_SESSION_USERNAME, session.username).putString(KEY_SESSION_NAME, session.name)
            .putString(KEY_SESSION_BIO, session.bio).putString(KEY_SESSION_PHOTO_URI, session.profilePhotoUri)
            .putString(KEY_SESSION_EMAIL, session.email).putBoolean(KEY_SESSION_VERIFIED, session.isVerified)
            .putLong(KEY_SESSION_AVATAR_COLOR, session.avatarColorHex)
            .putString(KEY_SESSION_STATUS, session.statusMessage)
            .putString(KEY_SESSION_PARTNER, session.partnerUsername).apply()
        _currentSession.value = session
    }

    private fun generateSalt(): String = SecureRandom().generateSeed(16).joinToString("") { "%02x".format(it) }
    private fun hashPasswordWithSalt(password: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((password + salt + "CLEAN_SHIELD_PEPPER_2026").toByteArray()).joinToString("") { "%02x".format(it) }

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

        @Volatile private var INSTANCE: AuthRepository? = null
        fun getInstance(context: Context): AuthRepository = INSTANCE ?: synchronized(this) { INSTANCE ?: AuthRepository(context.applicationContext).also { INSTANCE = it } }
    }
}
