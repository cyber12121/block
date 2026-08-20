package com.example.data.auth

data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isGuest: Boolean = false,
    val isDeveloper: Boolean = false,
    val provider: String = "google.com",
    val signInTimestamp: Long = System.currentTimeMillis(),
    val hasPassword: Boolean = false
)
