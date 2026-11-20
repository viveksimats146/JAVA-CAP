package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "node_health")
data class NodeHealthEntity(
    @PrimaryKey val nodeId: String,
    val status: String,
    val totalTriples: Long,
    val uptimeSeconds: Long,
    val responseTimeMs: Long = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
