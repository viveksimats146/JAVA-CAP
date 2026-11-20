package com.example.myapplication.data.remote

import com.example.myapplication.data.dto.AuthTokenResponse
import com.example.myapplication.data.dto.FederatedQueryRequest
import com.example.myapplication.data.dto.LogEntryDto
import com.example.myapplication.data.dto.LoginRequest
import com.example.myapplication.data.dto.NodeHealthDto
import com.example.myapplication.data.dto.QueryResponseDto
import com.example.myapplication.data.dto.RankedResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthTokenResponse>

    @POST("api/v1/query")
    suspend fun executeFederatedQuery(@Body request: FederatedQueryRequest): Response<QueryResponseDto>

    @GET("api/v1/nodes")
    suspend fun getNodeHealth(): Response<List<NodeHealthDto>>

    @POST("api/v1/dataset/reload")
    suspend fun reloadDataset(): Response<Map<String, Any>>

    @GET("api/v1/logs/recent")
    suspend fun getRecentLogs(@Query("limit") limit: Int = 50): Response<List<LogEntryDto>>

    @GET("api/v1/health")
    suspend fun getHealthWithCacheStats(): Response<Map<String, Any>>
}
