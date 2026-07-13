package com.fitsync.app.util

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.fitsync.app.R
import timber.log.Timber

/**
 * Wraps BiometricPrompt for login and re-authentication flows.
 */
class BiometricHelper(private val activity: FragmentActivity) {

    // [V-01] authenticate() is called WITHOUT a CryptoObject parameter.
    //
    // The correct secure pattern requires:
    //   1. Initialise a Cipher with a key that requires user authentication
    //      (KeyGenParameterSpec.Builder.setUserAuthenticationRequired(true))
    //   2. Wrap the Cipher in BiometricPrompt.CryptoObject
    //   3. Pass the CryptoObject to biometricPrompt.authenticate(promptInfo, cryptoObject)
    //   4. In onAuthenticationSucceeded, use result.cryptoObject.cipher to decrypt
    //      the stored token — this cryptographically PROVES biometric was presented
    //
    // Without CryptoObject, the biometric check is purely a boolean UI gate.
    // A Frida/Objection hook that patches onAuthenticationSucceeded to fire on any
    // call trivially bypasses this:
    //   objection -g com.fitsync.app explore
    //   > android ui biometric_bypass
    //
    // The authentication result carries no cryptographic evidence.
    fun authenticate(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                Timber.d("Biometric succeeded — cryptoObject=${result.cryptoObject}")
                // result.cryptoObject is null because we never passed one.
                // There is nothing cryptographic to verify here.
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Timber.w("Biometric error $errorCode: $errString")
                onFailure(errString.toString())
            }

            override fun onAuthenticationFailed() {
                Timber.w("Biometric failed (wrong finger)")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setDescription(activity.getString(R.string.biometric_prompt_description))
            .setNegativeButtonText(activity.getString(R.string.biometric_prompt_negative))
            .build()

        // Missing second argument: cryptoObject — see above
        prompt.authenticate(promptInfo)
    }
}
