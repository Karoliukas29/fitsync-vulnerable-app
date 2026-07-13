package com.fitsync.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

// [V-17] All calls go to http:// (cleartext) — see network_security_config.xml.
// [V-18] API key hardcoded as a constant and appended as a query parameter on every request.
//
// Both issues are visible in Burp Suite without any SSL unpinning:
//   GET http://analytics.fitsync.io/track?api_key=fsk_live_tel_4xR9...&event=login&user_id=...
//
// The api_key will appear in:
//   - Burp HTTP history in plaintext
//   - Server access logs at the analytics provider
//   - Any network intermediary or proxy
//   - Browser history if deep-linked via a WebView
//
// Fix: move API credentials to server-side. The mobile client should call your
// own backend, which then forwards to the analytics provider using server-held keys.
interface TelemetryService {

    @GET("track")
    suspend fun trackEvent(
        @Query("api_key") apiKey: String,
        @Query("event")   event: String,
        @Query("user_id") userId: String,
        @Query("plan")    plan: String,
        @Query("version") appVersion: String
    )

    @GET("identify")
    suspend fun identifyUser(
        @Query("api_key") apiKey: String,
        @Query("user_id") userId: String,
        @Query("email")   email: String,
        @Query("phone")   phone: String,
        @Query("gym_id")  gymId: String
    )
}
