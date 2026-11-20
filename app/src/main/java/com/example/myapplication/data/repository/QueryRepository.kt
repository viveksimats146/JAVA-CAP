package com.example.myapplication.data.repository

import com.example.myapplication.data.dto.RankedResultDto
import com.example.myapplication.data.dto.QueryResponseDto
import com.example.myapplication.data.local.entity.QueryResultEntity
import kotlinx.coroutines.flow.Flow

interface QueryRepository {
    suspend fun executeFederatedQuery(query: String, maxResults: Int = 100): Result<QueryResponseDto>
    fun getCachedResults(): Flow<List<QueryResultEntity>>
    suspend fun clearCache()
}
