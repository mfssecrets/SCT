package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val isRegistered: Boolean = false,
    val securityLevel: String = "High",
    val lastOptimizedTime: String = "Just now"
)

data class OptimizationMetrics(
    val junkCleanedMb: Int = 1420,
    val memoryFreedMb: Int = 860,
    val risksDetected: Int = 0,
    val systemItemsOptimized: Int = 48,
    val healthScore: Int = 100
)

class SecurityRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("clean_shield_secure_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _metrics = MutableStateFlow(OptimizationMetrics())
    val metrics: StateFlow<OptimizationMetrics> = _metrics.asStateFlow()

    private fun loadUserProfile(): UserProfile {
        val isReg = prefs.getBoolean(KEY_IS_REGISTERED, false)
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        return UserProfile(
            name = name,
            email = email,
            isRegistered = isReg
        )
    }

    fun hasPin(): Boolean {
        return prefs.contains(KEY_PIN_HASH)
    }

    fun savePin(pin: String): Boolean {
        if (pin.length != 6) return false
        val hash = hashString(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        if (pin.length != 6) return false
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return savedHash == hashString(pin)
    }

    fun registerAccount(name: String, email: String) {
        prefs.edit()
            .putBoolean(KEY_IS_REGISTERED, true)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .apply()
        _userProfile.value = UserProfile(
            name = name,
            email = email,
            isRegistered = true
        )
    }

    fun updateMetrics(junk: Int, mem: Int, risks: Int, sysItems: Int) {
        _metrics.value = OptimizationMetrics(
            junkCleanedMb = junk,
            memoryFreedMb = mem,
            risksDetected = risks,
            systemItemsOptimized = sysItems,
            healthScore = 100
        )
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "key_pin_hash_v1"
        private const val KEY_IS_REGISTERED = "key_is_registered"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_EMAIL = "key_user_email"

        @Volatile
        private var INSTANCE: SecurityRepository? = null

        fun getInstance(context: Context): SecurityRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
