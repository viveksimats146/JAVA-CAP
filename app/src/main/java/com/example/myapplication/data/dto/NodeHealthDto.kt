package com.example.myapplication.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class NodeHealthDto(
    val nodeId: String,
    val status: String,
    val totalTriples: Long = 0,
    val uptimeSeconds: Long = 0,
    val responseTimeMs: Long = 0
)
