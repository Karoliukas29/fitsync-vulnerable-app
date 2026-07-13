package com.fitsync.app.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import timber.log.Timber
import java.security.MessageDigest
import javax.crypto.SecretKey

class SessionManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("fitsync_session", Context.MODE_PRIVATE)

    // [V-03] Key material derived from ANDROID_ID — a persistent but non-secret device
    // identifier that can be read by any app holding READ_PRIVILEGED_PHONE_STATE or
    // obtained via ADB. The SHA-256 hash of a known value provides no security.
    //
    // The correct approach: generate a random key once, store it in the Android Keystore
    // (hardware-backed on supported devices, never leaves secure hardware).
    //
    // Secondary issue: deriving from Build.MODEL + ANDROID_ID means the key can be
    // pre-computed for any known device model once ANDROID_ID is compromised.
    fun deriveLocalKey(): SecretKey {
        val androidId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID) ?: "fallback"
        val seed = "fitsync_local_${androidId}_${Build.MODEL}"
        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(seed.toByteArray(Charsets.UTF_8))
        return CryptoManager.generateKey(keyBytes)
    }

    fun saveAuthToken(token: String) {
        val key = deriveLocalKey()
        val encrypted = CryptoManager.encrypt(token, key)
        prefs.edit().putString("auth_token_enc", encrypted).apply()
    }

    fun getAuthToken(): String {
        val encrypted = prefs.getString("auth_token_enc", null) ?: return ""
        return try {
            val key = deriveLocalKey()
            CryptoManager.decrypt(encrypted, key)
        } catch (e: Exception) {
            Timber.e("Token decryption failed: ${e.message}")
            ""
        }
    }

    fun saveUser(email: String, role: String, isPremium: Boolean) {
        prefs.edit()
            .putString("user_email", email)
            .putString("user_role", role)
            .putBoolean("is_premium", isPremium)  // [V-07] writeable via broadcast
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getString("auth_token_enc", null) != null
    fun isPremium(): Boolean  = prefs.getBoolean("is_premium", false)
    fun getUserEmail(): String = prefs.getString("user_email", "") ?: ""
    fun getUserRole(): String  = prefs.getString("user_role", "member") ?: "member"

    fun grantPremium() {
        prefs.edit().putBoolean("is_premium", true).apply()
        Timber.d("Premium granted for ${getUserEmail()}")
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
