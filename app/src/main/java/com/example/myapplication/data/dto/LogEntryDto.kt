package com.example.myapplication.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LogEntryDto(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO",
    val tag: String = "SYSTEM",
    val message: String = ""
)
