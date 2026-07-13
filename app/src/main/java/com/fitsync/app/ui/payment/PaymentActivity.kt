package com.fitsync.app.ui.payment

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fitsync.app.databinding.ActivityPaymentBinding
import com.fitsync.app.util.NotificationHelper
import com.fitsync.app.util.SessionManager
import com.fitsync.app.worker.PaymentSyncWorker
import timber.log.Timber

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [V-14] FLAG_SECURE is NOT set here.
        // The payment screen — which shows card number, expiry, CVV, and cardholder name
        // as the user types — can be:
        //   • Captured by any screen-recording or screenshot app
        //   • Shown in the recent-apps thumbnail (visible without unlocking)
        //   • Read via MediaProjection API by a malicious app granted screen capture
        //
        // Fix: add before setContentView():
        //   window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        //
        // The layout XML also lacks filterTouchesWhenObscured on the confirm button,
        // making it susceptible to tapjacking overlays.

        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val plan   = intent.getStringExtra("plan") ?: "monthly"
        val amount = if (plan == "annual") 99.99 else 12.99
        binding.tvTotal.text = "Total: €${"%.2f".format(amount)}"

        binding.btnConfirmPayment.setOnClickListener {
            val cardNumber = binding.etCardNumber.text?.toString() ?: ""
            val lastFour   = cardNumber.takeLast(4)

            // Simulate Stripe tokenisation — in a real app this uses the Stripe SDK
            val fakeStripeToken = "tok_${System.currentTimeMillis()}"

            Timber.d("Enqueueing payment: plan=$plan amount=$amount token=$fakeStripeToken")

            // [V-11] Stripe token and card identifier written into WorkManager Data —
            // stored in the work_db SQLite database in plaintext. See PaymentSyncWorker.
            val workData = Data.Builder()
                .putString("member_id",    session.getUserEmail())
                .putString("plan_id",      plan)
                .putDouble("amount",       amount)
                .putString("stripe_token", fakeStripeToken)
                .putString("card_last_four", lastFour)
                .build()

            WorkManager.getInstance(this)
                .enqueue(OneTimeWorkRequestBuilder<PaymentSyncWorker>()
                    .setInputData(workData)
                    .build())

            NotificationHelper.showPaymentNotification(this,
                session.getUserEmail(), amount, plan)

            Toast.makeText(this, "Payment processing…", Toast.LENGTH_SHORT).show()
            finish()
        }

        session = SessionManager(this)
    }
}
