package com.fitsync.app.ui.premium

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.fitsync.app.databinding.ActivityPremiumBinding
import com.fitsync.app.ui.payment.PaymentActivity
import com.fitsync.app.util.SessionManager
import timber.log.Timber

class PremiumGateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        // [V-07] isPremium() reads a SharedPreferences boolean that can be set to true
        // by broadcasting com.fitsync.app.GRANT_PREMIUM to the exported SessionReceiver.
        // The premium gate is purely a client-side flag check — no server-side
        // subscription verification happens before granting access.
        //
        // Attack chain:
        //   adb shell am broadcast -a com.fitsync.app.GRANT_PREMIUM
        //   → SessionReceiver.onReceive() sets is_premium=true in SharedPreferences
        //   → This activity reads isPremium()=true and shows premium content
        //   → No payment ever occurs, no server API is called
        if (session.isPremium()) {
            Timber.d("User already premium — showing pro content")
            binding.tvPremiumStatus.visibility = View.VISIBLE
            binding.btnSubscribeMonthly.isEnabled = false
            binding.btnSubscribeAnnual.isEnabled  = false
            return
        }

        binding.btnSubscribeMonthly.setOnClickListener {
            startActivity(Intent(this, PaymentActivity::class.java).apply {
                putExtra("plan", "monthly")
            })
        }

        binding.btnSubscribeAnnual.setOnClickListener {
            startActivity(Intent(this, PaymentActivity::class.java).apply {
                putExtra("plan", "annual")
            })
        }
    }
}
