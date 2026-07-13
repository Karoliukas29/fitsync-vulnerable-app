package com.fitsync.app.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles AES-GCM encryption for sensitive fields stored locally.
 * Key is derived from the device-bound secret (see [SessionManager.deriveLocalKey]).
 */
object CryptoManager {

    private const val ALGORITHM  = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128

    // [V-02] Static IV — the single biggest AES-GCM implementation mistake.
    // AES-GCM is an authenticated cipher that provides both confidentiality and
    // integrity, BUT only when a unique IV is used for every encryption with the
    // same key. Reusing the same IV+key pair:
    //   - Reveals XOR of any two plaintexts encrypted with the same key+IV
    //   - Completely destroys the GCM authentication guarantee
    //   - Allows an attacker who captures two ciphertexts to recover both plaintexts
    //
    // The correct fix: generate a random 12-byte IV per encryption and prepend it
    // to the ciphertext (IV does not need to be secret, only unique).
    private val STATIC_IV = "FitSyncIV240601".toByteArray(Charsets.UTF_8).copyOf(12)

    fun encrypt(plaintext: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, STATIC_IV))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, STATIC_IV))
        val decrypted = cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP))
        return String(decrypted, Charsets.UTF_8)
    }

    fun generateKey(keyBytes: ByteArray): SecretKey = SecretKeySpec(keyBytes, "AES")
}
