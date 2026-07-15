package com.fitsync.app.data.remote

import com.fitsync.app.util.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Builds the Retrofit-backed [ApiService] the app uses to talk to the FitSync backend.
 *
 * The client trusts the system/user CA store (no certificate pinning — see
 * res/xml/network_security_config.xml), so traffic is interceptable by any proxy
 * with a user-installed CA.
 *
 * For local testing there is no public backend. Point the [BASE_URL] host at the
 * bundled mock server (mock-server/README.md) using Burp's hostname resolution,
 * and the leaky /auth/login response becomes visible in the proxy.
 */
object ApiClient {

    private const val BASE_URL = "https://api.fitsync.io/"

    fun create(session: SessionManager): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(ApiInterceptor(session))
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
