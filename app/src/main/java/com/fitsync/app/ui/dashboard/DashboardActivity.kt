package com.fitsync.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fitsync.app.databinding.ActivityDashboardBinding
import com.fitsync.app.ui.premium.PremiumGateActivity
import com.fitsync.app.util.AnalyticsHelper
import com.fitsync.app.util.SessionManager

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        session = SessionManager(this)

        binding.tvGreeting.text = "Hello, ${session.getUserEmail().substringBefore('@')}"
        binding.tvPlanBadge.text = if (session.isPremium()) "PRO MEMBER" else "FREE PLAN"
        binding.tvActiveMembers.text = "124"
        binding.tvMonthlyRevenue.text = "€3,847"

        binding.btnUpgrade.setOnClickListener {
            startActivity(Intent(this, PremiumGateActivity::class.java))
        }

        // [V-15] Email and phone sent to Firebase Analytics
        AnalyticsHelper.identifyUser(
            userId  = session.getUserEmail().hashCode().toString(),
            email   = session.getUserEmail(),
            phone   = "+37061234567",       // retrieved from user profile in real app
            plan    = if (session.isPremium()) "pro" else "free",
            gymId   = "gym_vilnius_01"
        )

        AnalyticsHelper.trackScreenView("Dashboard")
    }
}
