package com.example.myapplication.data.repository

import com.example.myapplication.data.dto.CacheStatsDto
import com.example.myapplication.data.dto.LogEntryDto

interface LogsRepository {
    suspend fun fetchRecentLogs(limit: Int = 50): Result<List<LogEntryDto>>
    suspend fun fetchCacheStats(): Result<CacheStatsDto>
}
