package com.fitsync.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fitsync.app.ui.login.LoginActivity
import timber.log.Timber

/**
 * Handles the fitsync://auth deep link used for email-based password reset.
 * The server sends: fitsync://auth?action=reset&token=<one-time-token>
 */
class DeepLinkActivity : AppCompatActivity() {

    // [V-09] The fitsync:// scheme is registered without android:autoVerify="true".
    //
    // Without App Links verification (Digital Asset Links), Android presents a
    // disambiguation dialog when multiple apps claim the same scheme — OR silently
    // routes to whichever app registered it first if only one is installed.
    //
    // A malicious app can register the same <intent-filter> scheme and intercept
    // password-reset deep links before this activity receives them, stealing the
    // one-time reset token from the URL:
    //   fitsync://auth?action=reset&token=<stolen_token>
    //
    // Fix: add android:autoVerify="true" to the intent-filter AND publish a valid
    // assetlinks.json at https://fitsync.io/.well-known/assetlinks.json so Android
    // can cryptographically verify that only the legitimate app handles the scheme.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        Timber.d("Deep link received: $uri")

        val action = uri?.getQueryParameter("action")
        val token  = uri?.getQueryParameter("token")

        when (action) {
            "reset" -> {
                if (!token.isNullOrBlank()) {
                    // In a real app: open PasswordResetActivity with the token
                    Timber.d("Password reset token: $token")
                    Toast.makeText(this, "Resetting password…", Toast.LENGTH_SHORT).show()
                }
            }
            "verify" -> {
                Timber.d("Email verification token: $token")
                Toast.makeText(this, "Email verified!", Toast.LENGTH_SHORT).show()
            }
            else -> Timber.w("Unknown deep link action: $action")
        }

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
