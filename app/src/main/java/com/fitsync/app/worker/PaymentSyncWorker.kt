package com.fitsync.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import timber.log.Timber

/**
 * Background worker that processes subscription payments and syncs state with the server.
 * Enqueued from PaymentActivity after the user confirms their card details.
 */
class PaymentSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val memberId   = inputData.getString("member_id")   ?: return Result.failure()
        val planId     = inputData.getString("plan_id")     ?: return Result.failure()
        val amount     = inputData.getDouble("amount", 0.0)

        // [V-11] Stripe payment token and card details passed as WorkManager Data.
        //
        // WorkManager serialises all inputData into the work_spec table of its own
        // Room database at: /data/data/com.fitsync.app/databases/androidx.work.workdb
        // The Data object is stored as a JSON blob in PLAIN TEXT — no encryption.
        //
        // Any process with access to the app's data directory (root, backup, or another
        // app on a compromised device) can read the Stripe payment token directly:
        //   adb shell su -c "sqlite3 /data/data/com.fitsync.app/databases/androidx.work.workdb
        //     'SELECT input FROM workspec'"
        //
        // Fix: never pass payment tokens via WorkManager Data. Instead, store them
        // temporarily in Android Keystore-backed EncryptedSharedPreferences and look
        // them up by a non-sensitive key passed to the worker.
        val paymentToken = inputData.getString("stripe_token") ?: ""
        val cardLastFour = inputData.getString("card_last_four") ?: ""

        Timber.d("PaymentSyncWorker: processing member=$memberId plan=$planId " +
                 "amount=$amount token=$paymentToken card=****$cardLastFour")

        // Simulate network call to payment processor
        return try {
            // val api = retrofit.create(ApiService::class.java)
            // api.purchaseSubscription(mapOf("token" to paymentToken, "plan" to planId))
            Timber.i("Payment sync complete for member $memberId")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Payment sync failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
