package com.fitsync.app.data.remote.models

data class SubscriptionResponse(
    val isPremium: Boolean,
    val planName: String,
    val expiryDate: String,
    val stripeSubscriptionId: String?
)
