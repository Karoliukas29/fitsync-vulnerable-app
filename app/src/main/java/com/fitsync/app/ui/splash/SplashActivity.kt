package com.fitsync.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.fitsync.app.R
import com.fitsync.app.ui.dashboard.DashboardActivity
import com.fitsync.app.ui.login.LoginActivity
import com.fitsync.app.util.SessionManager

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val session = SessionManager(this)
        Handler(Looper.getMainLooper()).postDelayed({
            val dest = if (session.isLoggedIn()) DashboardActivity::class.java
                       else LoginActivity::class.java
            startActivity(Intent(this, dest))
            finish()
        }, 1200)
    }
}
