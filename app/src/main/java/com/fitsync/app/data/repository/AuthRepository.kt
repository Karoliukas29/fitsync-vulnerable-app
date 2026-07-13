package com.fitsync.app.data.repository

import android.util.Base64
import com.fitsync.app.data.remote.ApiService
import com.fitsync.app.data.remote.models.AuthRequest
import com.fitsync.app.util.SessionManager
import org.json.JSONObject
import timber.log.Timber

class AuthRepository(
    private val apiService: ApiService,
    private val session: SessionManager
) {

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = apiService.login(AuthRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                session.saveAuthToken(body.token)
                session.saveUser(body.email, body.role, body.isPremium)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Login error")
            Result.failure(e)
        }
    }

    // [V-13] Token validity is checked by decoding the JWT payload and reading the
    // sub claim, but the exp (expiration) field is never compared against the current
    // time. A token that has expired on the server continues to pass this check locally.
    //
    // Practical impact: a stolen JWT remains "valid" in the app indefinitely — the
    // app will not prompt for re-login or token refresh. Combined with V-12 (token in
    // logs), an attacker capturing an old token from logcat can still use it
    // client-side even after the server has expired it, delaying detection.
    //
    // Fix: deserialize expiresAt from AuthResponse, persist it alongside the token,
    // and check System.currentTimeMillis() / 1000 < expiresAt before each API call.
    fun isTokenValid(): Boolean {
        val token = session.getAuthToken()
        if (token.isBlank()) return false

        return try {
            val parts = token.split(".")
            if (parts.size != 3) return false

            val payloadJson = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING),
                Charsets.UTF_8
            )
            val payload = JSONObject(payloadJson)
            val sub = payload.optString("sub", "")
            // Decodes successfully — returns true for ANY non-empty sub, including expired tokens
            sub.isNotBlank()
        } catch (e: Exception) {
            Timber.w("Token parse error: ${e.message}")
            false
        }
    }

    fun logout() = session.logout()
}
