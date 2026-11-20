package com.example.myapplication.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CacheStatsDto(
    val hitCount: Long = 0,
    val missCount: Long = 0,
    val hitRate: Double = 0.0,
    val evictionCount: Long = 0,
    val size: Long = 0
)
