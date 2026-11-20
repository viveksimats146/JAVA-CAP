package com.example.myapplication.data.repository

import com.example.myapplication.data.dto.FederatedQueryRequest
import com.example.myapplication.data.dto.QueryResponseDto
import com.example.myapplication.data.local.dao.QueryResultDao
import com.example.myapplication.data.local.entity.QueryResultEntity
import com.example.myapplication.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueryRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val queryResultDao: QueryResultDao
) : QueryRepository {

    override suspend fun executeFederatedQuery(query: String, maxResults: Int): Result<QueryResponseDto> {
        return try {
            val response = apiService.executeFederatedQuery(
                FederatedQueryRequest(query = query, maxResults = maxResults)
            )
            if (response.isSuccessful && response.body() != null) {
                val queryResponse = response.body()!!
                val entities = queryResponse.results.map { dto ->
                    QueryResultEntity(
                        condition = dto.condition,
                        symptom = dto.symptom,
                        score = dto.score,
                        contributingHospitals = dto.contributingHospitals.joinToString(","),
                        totalCases = dto.totalCases
                    )
                }
                queryResultDao.insertResults(entities)
                Result.success(queryResponse)
            } else {
                Result.failure(Exception("Query failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCachedResults(): Flow<List<QueryResultEntity>> {
        return queryResultDao.getAllResults()
    }

    override suspend fun clearCache() {
        queryResultDao.clearResults()
    }
}
