package com.example.myapplication.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RankedResultDto(
    val condition: String,
    val symptom: String,
    val score: Double,
    val contributingHospitals: List<String> = emptyList(),
    val totalCases: Int = 0,
    val hospitalDetails: List<HospitalResultDto> = emptyList()
)
