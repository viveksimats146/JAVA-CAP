package com.federated.hospital.controller

import com.federated.common.rmi.NodeHealth
import com.federated.hospital.orchestrator.FederationOrchestrator
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class NodeController(
    @Value("\${hospital.id:hospital-a}") private val hospitalId: String,
    @Value("\${hospital.rmi-port:1099}") private val rmiPort: Int,
    private val federationOrchestrator: FederationOrchestrator
) {

    @GetMapping("/nodes")
    fun getNodeStatuses(): ResponseEntity<List<NodeHealth>> {
        val statuses = federationOrchestrator.getNodeStatuses()
        return ResponseEntity.ok(statuses)
    }

    @GetMapping("/health")
    fun getHealth(): ResponseEntity<Map<String, Any>> {
        val cacheStats = federationOrchestrator.subQueryCache.getStats()
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "hospitalId" to hospitalId,
                "rmiPort" to rmiPort,
                "cacheStats" to cacheStats
            )
        )
    }
}
