package com.fitsync.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fitsync.app.data.remote.ApiClient
import com.fitsync.app.data.remote.ApiService
import com.fitsync.app.data.remote.WorkoutSyncService
import com.fitsync.app.databinding.ActivityDashboardBinding
import com.fitsync.app.ui.premium.PremiumGateActivity
import com.fitsync.app.util.AnalyticsHelper
import com.fitsync.app.util.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import timber.log.Timber

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        session = SessionManager(this)
        val api = ApiClient.create(session)

        binding.tvGreeting.text = "Hello, ${session.getUserEmail().substringBefore('@')}"
        binding.tvPlanBadge.text = if (session.isPremium()) "PRO MEMBER" else "FREE PLAN"
        binding.tvActiveMembers.text = "124"
        binding.tvMonthlyRevenue.text = "€3,847"

        binding.btnUpgrade.setOnClickListener {
            startActivity(Intent(this, PremiumGateActivity::class.java))
        }

        // [V-11] Loads the full member roster (PII) over the API. The request carries
        // the session Bearer token; the response returns names, emails and phone
        // numbers — all visible in an intercepting proxy.
        binding.btnViewMembers.setOnClickListener { loadMembers(api) }

        // [V-15] Email and phone sent to Firebase Analytics + cleartext telemetry
        AnalyticsHelper.identifyUser(
            userId  = session.getUserEmail().hashCode().toString(),
            email   = session.getUserEmail(),
            phone   = "+37061234567",       // retrieved from user profile in real app
            plan    = if (session.isPremium()) "pro" else "free",
            gymId   = "gym_vilnius_01"
        )

        AnalyticsHelper.trackScreenView("Dashboard")

        // [V-18] Background workout sync to the third-party FitCloud provider. The live
        // FitCloud key/secret are hardcoded (see WorkoutSyncService) and travel in the
        // Authorization / X-FitCloud-Secret headers on every sync — recoverable by
        // anyone proxying the app's traffic.
        WorkoutSyncService.syncWorkout(
            userId      = session.getUserEmail().hashCode().toString(),
            workoutJson = """{"type":"run","durationMin":32,"distanceKm":5.1,"calories":410}"""
        )

        // [V-12] Silent session-token refresh on dashboard entry. When the backend
        // rejects the refresh (401), ApiInterceptor logs the full Authorization header
        // — Bearer token included — to logcat.
        lifecycleScope.launch {
            try {
                val resp = api.refreshToken()
                Timber.d("Token refresh: ${resp.code()}")
            } catch (e: Exception) {
                Timber.e(e, "Token refresh failed")
            }
        }
    }

    private fun loadMembers(api: ApiService) {
        lifecycleScope.launch {
            try {
                val resp = api.getMembers()
                val count = resp.body()?.size ?: 0
                Snackbar.make(binding.root, "Loaded $count members", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load members")
                Snackbar.make(binding.root, "Couldn't reach backend", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
