package com.fitsync.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fitsync.app.databinding.ActivityLoginBinding
import com.fitsync.app.ui.dashboard.DashboardActivity
import com.fitsync.app.util.AnalyticsHelper
import com.fitsync.app.util.BiometricHelper
import com.fitsync.app.util.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session         = SessionManager(this)
        biometricHelper = BiometricHelper(this)

        binding.btnSignIn.setOnClickListener { attemptLogin() }
        binding.btnBiometric.setOnClickListener { attemptBiometric() }
    }

    private fun attemptLogin() {
        val email    = binding.etEmail.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""

        if (email.isBlank() || password.isBlank()) {
            Snackbar.make(binding.root, "Please fill in all fields", Snackbar.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            // In the real app this calls AuthRepository.login()
            // For the demo, simulate a successful login
            Timber.d("Login attempt for $email")
            session.saveAuthToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c3IxMjM0IiwiZW1haWwiOiIkZW1haWwiLCJyb2xlIjoibWFuYWdlciIsImV4cCI6MTcwOTI1MTIwMH0.signature")
            session.saveUser(email, "manager", false)
            AnalyticsHelper.trackLogin(email.hashCode().toString(), "password")
            navigateToDashboard()
        }
    }

    private fun attemptBiometric() {
        biometricHelper.authenticate(
            onSuccess = {
                Timber.d("Biometric success — navigating to dashboard")
                AnalyticsHelper.trackLogin(session.getUserEmail().hashCode().toString(), "biometric")
                navigateToDashboard()
            },
            onFailure = { error ->
                Snackbar.make(binding.root, error, Snackbar.LENGTH_SHORT).show()
            }
        )
    }

    private fun navigateToDashboard() {
        setLoading(false)
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSignIn.isEnabled    = !loading
        binding.btnBiometric.isEnabled = !loading
    }
}
