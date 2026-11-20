package com.example.myapplication.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class FederatedQueryRequest(
    val query: String,
    val maxResults: Int = 100
)
