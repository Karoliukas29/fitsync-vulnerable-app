package com.fitsync.app.data.remote

import com.fitsync.app.util.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class ApiInterceptor(private val session: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.getAuthToken()

        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .addHeader("X-App-Version", "2.1.0")
            .addHeader("X-Platform", "android")
            .build()

        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            // [V-12] Bearer token included in the error log.
            // Timber.DebugTree forwards to android.util.Log which is visible via
            // adb logcat to any app holding READ_LOGS (granted to ADB sessions and
            // some system apps). In the release build, Timber.DebugTree is still
            // planted (see FitSyncApp.kt), so this leaks in production.
            //
            // An attacker with brief physical access (or a malicious app that acquired
            // READ_LOGS via adb during setup) can capture the Bearer token and
            // replay authenticated API requests.
            //
            // Fix: log only the status code and a sanitised request URL, never headers.
            Timber.e(
                "API error ${response.code} on ${request.url} " +
                "| auth: ${request.header("Authorization")}"
            )
        }

        return response
    }
}
