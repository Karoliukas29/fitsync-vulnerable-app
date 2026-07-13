package com.fitsync.app.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

object AnalyticsHelper {

    // [V-17] [V-18] Hardcoded live telemetry API key sent as a URL query parameter
    // over HTTP. Visible in Burp Suite without any SSL configuration:
    //   GET http://analytics.fitsync.io/track?api_key=fsk_live_tel_4xR9mK2pL8nQ7wE3&...
    private const val TELEMETRY_BASE    = "http://analytics.fitsync.io"
    private const val TELEMETRY_API_KEY = "fsk_live_tel_4xR9mK2pL8nQ7wE3vB6tJ1yH5dA0cF"

    private val http   = OkHttpClient()
    private val scope  = CoroutineScope(Dispatchers.IO)

    // [V-15] PII sent to Firebase Analytics violates GDPR Article 5 (data minimisation).
    // Firebase Analytics user properties are tied to the device's advertising ID and
    // transmitted to Google's servers in the US — a third-country transfer requiring
    // explicit consent and an adequacy decision or SCCs.
    //
    // Sending email and phone as user properties means:
    //   - Google can link gym member PII to ad profiles
    //   - Data leaves the EEA without proper legal basis
    //   - A DPA investigation (e.g., Datatilsynet, AEPD) could result in fines
    //
    // Fix: never send directly identifiable data (email, phone, full name) as
    // analytics properties. Use pseudonymous IDs only.
    fun identifyUser(userId: String, email: String, phone: String, plan: String, gymId: String) {
        // Firebase PII tracking [V-15]
        val analytics = Firebase.analytics
        analytics.setUserId(email.hashCode().toString())
        analytics.setUserProperty("email_address", email)
        analytics.setUserProperty("phone_number", phone)
        analytics.setUserProperty("subscription_plan", plan)
        analytics.setUserProperty("gym_location_id", gymId)

        // Cleartext HTTP call with API key in URL — visible in Burp without SSL setup [V-17][V-18]
        scope.launch {
            try {
                val url = "$TELEMETRY_BASE/identify" +
                    "?api_key=$TELEMETRY_API_KEY" +
                    "&user_id=$userId" +
                    "&email=$email" +
                    "&phone=$phone" +
                    "&gym_id=$gymId"
                val request = Request.Builder().url(url).get().build()
                http.newCall(request).execute().close()
            } catch (e: Exception) {
                Timber.e(e, "Telemetry identify failed")
            }
        }
    }

    fun trackSubscriptionPurchase(plan: String, amount: Double, currency: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_NAME, plan)
            putDouble(FirebaseAnalytics.Param.VALUE, amount)
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
        }
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)
    }

    fun trackLogin(userId: String, method: String) {
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        })
        scope.launch {
            try {
                val url = "$TELEMETRY_BASE/track" +
                    "?api_key=$TELEMETRY_API_KEY" +
                    "&event=login" +
                    "&user_id=$userId" +
                    "&method=$method" +
                    "&version=2.1.0"
                val request = Request.Builder().url(url).get().build()
                http.newCall(request).execute().close()
            } catch (e: Exception) {
                Timber.e(e, "Telemetry track failed")
            }
        }
    }

    fun trackScreenView(screenName: String) {
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        })
    }
}
