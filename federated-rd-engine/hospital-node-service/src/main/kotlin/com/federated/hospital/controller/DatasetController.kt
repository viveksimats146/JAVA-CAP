package com.federated.hospital.controller

import com.federated.hospital.graph.LocalGraphManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/dataset")
class DatasetController(
    private val localGraphManager: LocalGraphManager
) {

    @PostMapping("/reload")
    fun reloadDataset(): ResponseEntity<Map<String, Any>> {
        val success = localGraphManager.reloadDatasetFromProvider()
        val totalTriples = localGraphManager.getTotalTripleCount()
        return ResponseEntity.ok(
            mapOf(
                "success" to success,
                "message" to "Dataset reloaded from dataset-provider-service successfully",
                "totalTriples" to totalTriples
            )
        )
    }
}
