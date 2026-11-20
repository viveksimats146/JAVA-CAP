package com.example.myapplication.data.repository

import com.example.myapplication.data.dto.AuthTokenResponse
import com.example.myapplication.data.dto.LoginRequest
import com.example.myapplication.data.local.TokenManager
import com.example.myapplication.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<AuthTokenResponse> {
        return try {
            val response = apiService.login(LoginRequest(username = username, password = password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveAuthToken(authResponse.token, authResponse.username)
                Result.success(authResponse)
            } else {
                Result.failure(Exception("Login failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getToken(): String? {
        return tokenManager.getToken()
    }

    override fun observeToken(): Flow<String?> {
        return tokenManager.authTokenFlow
    }

    override suspend fun logout() {
        tokenManager.clearToken()
    }
}
