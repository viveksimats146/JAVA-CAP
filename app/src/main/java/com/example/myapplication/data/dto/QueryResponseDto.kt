package com.example.myapplication.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class QueryResponseDto(
    val queryId: String,
    val timestamp: Long,
    val executionTimeMs: Long,
    val totalResults: Int,
    val results: List<RankedResultDto>,
    val peerStatuses: List<NodeHealthDto>,
    val recentLogs: List<String>
)
