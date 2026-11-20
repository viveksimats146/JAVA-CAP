package com.example.myapplication.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val expiresInMs: Long = 86400000,
    val username: String = ""
)
