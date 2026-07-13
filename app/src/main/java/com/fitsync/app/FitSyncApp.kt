package com.fitsync.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import timber.log.Timber

class FitSyncApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        createNotificationChannels()

        // [V-12] Timber.DebugTree planted unconditionally — runs in release builds too.
        // A proper release setup would use a custom tree that filters sensitive data
        // and forwards to Crashlytics. As-is, all Timber.d() / Timber.e() calls
        // (including those that log auth tokens in ApiInterceptor) appear in adb logcat
        // even in the production APK.
        Timber.plant(Timber.DebugTree())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel("payments", getString(R.string.channel_payments),
                    NotificationManager.IMPORTANCE_DEFAULT))
            manager.createNotificationChannel(
                NotificationChannel("alerts", getString(R.string.channel_alerts),
                    NotificationManager.IMPORTANCE_HIGH))
        }
    }
}
