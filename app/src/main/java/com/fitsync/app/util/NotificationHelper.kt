package com.fitsync.app.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fitsync.app.ui.payment.PaymentActivity
import com.fitsync.app.R
import timber.log.Timber

object NotificationHelper {

    fun showPaymentNotification(context: Context, memberId: String, amount: Double, plan: String) {
        val detailIntent = Intent(context, PaymentActivity::class.java).apply {
            putExtra("member_id", memberId)
            putExtra("amount", amount)
            putExtra("plan", plan)
        }

        // [V-10] PendingIntent created with FLAG_MUTABLE (explicitly set for Android 12+
        // compatibility instead of using FLAG_IMMUTABLE as required for passive intents).
        //
        // FLAG_MUTABLE allows another app with SEND intent privilege to fill in the
        // component, action, or extras of this PendingIntent before it fires.
        // A malicious app colluding with the notification system could modify
        // member_id or amount extras — e.g., redirecting a payment confirmation
        // to show a different member's details or a lower amount.
        //
        // Fix: replace FLAG_MUTABLE with FLAG_IMMUTABLE — this is a read-only
        // notification tap; the intent does not need to be modified by anyone.
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, memberId.hashCode(),
            detailIntent, pendingFlags)

        val notification = NotificationCompat.Builder(context, "payments")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_payment_title))
            .setContentText("€${"%.2f".format(amount)} — $plan")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(memberId.hashCode(), notification)
        Timber.d("Payment notification sent for member $memberId")
    }
}
