package com.federated.hospital.controller

import com.federated.hospital.logging.LogBuffer
import com.federated.hospital.logging.LogEntry
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/logs")
class LogsController(
    private val logBuffer: LogBuffer
) {

    @GetMapping("/recent")
    fun getRecentLogs(@RequestParam(defaultValue = "50") limit: Int): ResponseEntity<List<LogEntry>> {
        val logs = logBuffer.getRecentLogs(limit)
        return ResponseEntity.ok(logs)
    }
}
