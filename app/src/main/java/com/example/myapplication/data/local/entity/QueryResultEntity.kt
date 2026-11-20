package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "query_results")
data class QueryResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val condition: String,
    val symptom: String,
    val score: Double,
    val contributingHospitals: String,
    val totalCases: Int,
    val timestamp: Long = System.currentTimeMillis()
)
