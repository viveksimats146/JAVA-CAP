package com.example.myapplication.data.repository

import com.example.myapplication.data.dto.AuthTokenResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<AuthTokenResponse>
    suspend fun getToken(): String?
    fun observeToken(): Flow<String?>
    suspend fun logout()
}
