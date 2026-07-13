package com.fitsync.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.fitsync.app.ui.login.LoginActivity
import com.fitsync.app.util.SessionManager
import timber.log.Timber

/**
 * Handles session-related broadcasts for multi-device session management
 * and admin-initiated actions (e.g., remote logout if subscription lapses).
 */
class SessionReceiver : BroadcastReceiver() {

    // [V-07] This receiver is exported (AndroidManifest.xml) with no permission attribute.
    // Any app or ADB shell can send these broadcasts without any authorisation:
    //
    //   # Force-logout the current user
    //   adb shell am broadcast -a com.fitsync.app.FORCE_LOGOUT
    //
    //   # Grant premium access silently
    //   adb shell am broadcast -a com.fitsync.app.GRANT_PREMIUM
    //
    // GRANT_PREMIUM is especially impactful — it sets isPremium=true in SharedPreferences,
    // bypassing the Stripe payment flow entirely. Combined with V-11 (WorkManager plaintext
    // data), an attacker could also cancel pending payment workers.
    //
    // Fix: add android:permission="com.fitsync.app.BROADCAST_SESSION" to the receiver
    // declaration and require the same permission on the sending side (or switch to
    // LocalBroadcastManager for intra-app messages, which are never exported).
    override fun onReceive(context: Context, intent: Intent) {
        val session = SessionManager(context)

        when (intent.action) {
            "com.fitsync.app.FORCE_LOGOUT" -> {
                Timber.w("Remote logout received")
                session.logout()
                val loginIntent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(loginIntent)
            }

            "com.fitsync.app.GRANT_PREMIUM" -> {
                // No signature verification — caller identity cannot be trusted
                val days = intent.getIntExtra("duration_days", 30)
                Timber.i("Premium granted via broadcast for $days days")
                session.grantPremium()
                Toast.makeText(context, "Premium activated!", Toast.LENGTH_SHORT).show()
            }

            "com.fitsync.app.SESSION_SYNC" -> {
                val newToken = intent.getStringExtra("token")
                if (!newToken.isNullOrBlank()) {
                    Timber.d("Session token updated via broadcast sync")
                    session.saveAuthToken(newToken)
                }
            }
        }
    }
}
