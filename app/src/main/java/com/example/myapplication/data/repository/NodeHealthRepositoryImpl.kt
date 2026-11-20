package com.example.myapplication.data.repository

import com.example.myapplication.data.dto.NodeHealthDto
import com.example.myapplication.data.local.dao.NodeHealthDao
import com.example.myapplication.data.local.entity.NodeHealthEntity
import com.example.myapplication.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NodeHealthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val nodeHealthDao: NodeHealthDao
) : NodeHealthRepository {

    override suspend fun fetchNodeHealth(): Result<List<NodeHealthDto>> {
        val startTime = System.currentTimeMillis()
        return try {
            val response = apiService.getNodeHealth()
            val responseTimeMs = System.currentTimeMillis() - startTime
            if (response.isSuccessful && response.body() != null) {
                val nodeHealthList = response.body()!!.map { it.copy(responseTimeMs = responseTimeMs) }
                val entities = nodeHealthList.map { nodeHealth ->
                    NodeHealthEntity(
                        nodeId = nodeHealth.nodeId,
                        status = nodeHealth.status,
                        totalTriples = nodeHealth.totalTriples,
                        uptimeSeconds = nodeHealth.uptimeSeconds,
                        responseTimeMs = responseTimeMs,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                // Clear old and insert new, or just insert (assuming insert uses REPLACE strategy)
                entities.forEach { nodeHealthDao.insertNodeHealth(it) }
                Result.success(nodeHealthList)
            } else {
                Result.failure(Exception("Node health check failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCachedNodeHealth(): Flow<List<NodeHealthEntity>> {
        return nodeHealthDao.getAllNodeHealth()
    }

    override suspend fun reloadDataset(): Result<String> {
        return try {
            val response = apiService.reloadDataset()
            if (response.isSuccessful) {
                val body = response.body()
                val message = body?.get("message")?.toString()
                    ?: "Dataset reloaded from dataset-provider-service successfully"
                Result.success(message)
            } else {
                Result.failure(Exception("Dataset reload failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
