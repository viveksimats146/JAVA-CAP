package com.federated.dataset.controller

import com.federated.dataset.service.DatasetService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/datasets")
class DatasetController(
    private val datasetService: DatasetService
) {

    @GetMapping("/hospital-a")
    fun getHospitalA(): ResponseEntity<String> = serveTurtle("hospital-a")

    @GetMapping("/hospital-b")
    fun getHospitalB(): ResponseEntity<String> = serveTurtle("hospital-b")

    @GetMapping("/hospital-c")
    fun getHospitalC(): ResponseEntity<String> = serveTurtle("hospital-c")

    @GetMapping("/{hospitalId}")
    fun getHospitalById(@PathVariable hospitalId: String): ResponseEntity<String> = serveTurtle(hospitalId)

    @GetMapping("/health")
    fun getHealth(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "service" to "dataset-provider-service",
                "datasetsAvailable" to listOf("hospital-a", "hospital-b", "hospital-c")
            )
        )
    }

    private fun serveTurtle(hospitalId: String): ResponseEntity<String> {
        return try {
            val turtleContent = datasetService.getDatasetTurtle(hospitalId)
            val headers = HttpHeaders().apply {
                add(HttpHeaders.CONTENT_TYPE, "text/turtle;charset=UTF-8")
            }
            ResponseEntity(turtleContent, headers, HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: ${e.message}")
        }
    }
}
