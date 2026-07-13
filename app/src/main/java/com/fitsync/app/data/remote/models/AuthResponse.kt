package com.fitsync.app.data.remote.models

data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val role: String,
    val isPremium: Boolean,
    // [V-13 context] exp is returned but never read by the client — see AuthRepository
    val expiresAt: Long,
    // [V-16] Excessive data exposure — none of these fields are needed by the mobile client.
    // Visible in plain text in Burp Suite when intercepting the /auth/login response.
    // stripeCustomerId can be used to enumerate Stripe customers.
    // passwordHash should never leave the server under any circumstances.
    // memberCount leaks internal business metrics to any intercepting party.
    val stripeCustomerId: String?,
    val internalUserId: String?,
    val gymChainId: String?,
    val passwordHash: String?,
    val memberCount: Int?,
    val adminNotes: String?
)
