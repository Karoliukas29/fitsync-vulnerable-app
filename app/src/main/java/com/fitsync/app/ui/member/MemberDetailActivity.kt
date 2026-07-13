package com.fitsync.app.ui.member

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fitsync.app.databinding.ActivityMemberDetailBinding
import timber.log.Timber

class MemberDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val memberId = intent.getStringExtra("member_id") ?: "1"
        Timber.d("Viewing member: $memberId")

        // Demo data — real app fetches from API
        val email = "marcus.johnson@example.com"
        binding.tvName.text   = "Marcus Johnson"
        binding.tvEmail.text  = email
        binding.tvPlan.text   = "Annual Plan — Active"
        binding.tvExpiry.text = "Renews 2025-01-15"

        // [V-19-style] Long-press on email auto-copies to clipboard.
        // On Android 12+ a toast appears notifying OTHER APPS that clipboard content
        // changed. More critically, any app running in the foreground that polls
        // ClipboardManager receives the member's email without permission.
        // GDPR consideration: silently copying PII to an uncontrolled shared buffer.
        binding.tvEmail.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("member_email", email))
            Toast.makeText(this, "Email copied", Toast.LENGTH_SHORT).show()
            true
        }
    }
}
