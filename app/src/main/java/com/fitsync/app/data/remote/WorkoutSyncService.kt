package com.fitsync.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import timber.log.Timber

// [V-18] Hardcoded third-party API credentials.
// FitCloud is a third-party workout analytics provider. The live secret key is
// hardcoded directly in the source — anyone who decompiles the APK with jadx
// gets immediate, unrestricted access to the FitCloud account.
//
// Visible in Burp Suite as:
//   POST https://api.fitcloud.io/v1/sync
//   Authorization: Bearer fc_live_sk_4xR9mK2pL8nQ7wE3vB6tJ1yH5dA0cF
//   X-FitCloud-Secret: fcs_prod_secret_9Kz2Xm4Wq7Nt1Lp8Rv3Ys6Ub0Jd5Gh
//
// Fix: never embed third-party keys in the APK. Use your own backend as a proxy —
// the mobile app calls your API, your server calls FitCloud with its own credentials.
object WorkoutSyncService {

    private const val FITCLOUD_BASE_URL   = "https://api.fitcloud.io/v1"
    private const val FITCLOUD_API_KEY    = "fc_live_sk_4xR9mK2pL8nQ7wE3vB6tJ1yH5dA0cF"
    private const val FITCLOUD_API_SECRET = "fcs_prod_secret_9Kz2Xm4Wq7Nt1Lp8Rv3Ys6Ub0Jd5Gh"
    private const val FITCLOUD_WEBHOOK_KEY = "fcwh_live_7Hp3Nq9Xk2Mw5Yt8Rb1Ls4Vu6Jd0Ef"

    private val client = OkHttpClient()

    fun syncWorkout(userId: String, workoutJson: String) {
        val body = workoutJson.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$FITCLOUD_BASE_URL/sync")
            .post(body)
            .addHeader("Authorization",      "Bearer $FITCLOUD_API_KEY")
            .addHeader("X-FitCloud-Secret",  FITCLOUD_API_SECRET)
            .addHeader("X-User-Id",          userId)
            .addHeader("Content-Type",       "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Timber.d("FitCloud sync: ${response.code}")
            }
        } catch (e: Exception) {
            Timber.e(e, "FitCloud sync failed")
        }
    }

    fun registerWebhook(callbackUrl: String) {
        val json = """{"url":"$callbackUrl","secret":"$FITCLOUD_WEBHOOK_KEY"}"""
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$FITCLOUD_BASE_URL/webhooks/register")
            .post(body)
            .addHeader("Authorization", "Bearer $FITCLOUD_API_KEY")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Timber.d("Webhook registered: ${response.code}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Webhook registration failed")
        }
    }
}
