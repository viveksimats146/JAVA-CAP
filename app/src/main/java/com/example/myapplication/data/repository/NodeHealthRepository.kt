package com.example.myapplication.data.repository

import com.example.myapplication.data.dto.NodeHealthDto
import com.example.myapplication.data.local.entity.NodeHealthEntity
import kotlinx.coroutines.flow.Flow

interface NodeHealthRepository {
    suspend fun fetchNodeHealth(): Result<List<NodeHealthDto>>
    fun getCachedNodeHealth(): Flow<List<NodeHealthEntity>>
    suspend fun reloadDataset(): Result<String>
}
