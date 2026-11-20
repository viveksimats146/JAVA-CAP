package com.example.myapplication.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class HospitalResultDto(
    val patientId: String? = null,
    val condition: String,
    val symptom: String,
    val hospitalId: String,
    val ageBand: String? = null,
    val reportedDate: String? = null
)
