package com.fitsync.app.data.remote.models

data class MemberResponse(
    val id: String,
    val displayName: String,
    val email: String,
    val phone: String,
    val plan: String,
    val status: String,
    val expiryDate: String,
    val gymLocation: String
)
