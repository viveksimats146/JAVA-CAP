package com.federated.hospital.controller

import com.federated.common.rmi.NodeHealth
import com.federated.common.rmi.RankedResult
import com.federated.hospital.logging.LogBuffer
import com.federated.hospital.orchestrator.FederationOrchestrator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class QueryRequestDto(
    val query: String,
    val maxResults: Int = 100
)

data class QueryResponseDto(
    val queryId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val executionTimeMs: Long,
    val totalResults: Int,
    val results: List<RankedResult>,
    val peerStatuses: List<NodeHealth>,
    val recentLogs: List<String> = emptyList()
)

@RestController
@RequestMapping("/api/v1")
class QueryController(
    private val federationOrchestrator: FederationOrchestrator,
    private val logBuffer: LogBuffer
) {

    @PostMapping("/query")
    fun executeQuery(@RequestBody request: QueryRequestDto): ResponseEntity<QueryResponseDto> {
        val result = federationOrchestrator.executeFederatedQuery(
            sparqlString = request.query,
            maxResults = request.maxResults
        )

        val recentLogs = logBuffer.getRecentLogs(20).map { "[${it.level}] ${it.tag}: ${it.message}" }

        val response = QueryResponseDto(
            queryId = result.queryId,
            executionTimeMs = result.executionTimeMs,
            totalResults = result.rankedResults.size,
            results = result.rankedResults,
            peerStatuses = result.nodeStatuses,
            recentLogs = recentLogs
        )

        return ResponseEntity.ok(response)
    }
}
