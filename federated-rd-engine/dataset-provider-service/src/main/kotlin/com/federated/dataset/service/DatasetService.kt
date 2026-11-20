package com.federated.dataset.service

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Service
class DatasetService {

    fun getDatasetTurtle(hospitalId: String): String {
        val normalized = hospitalId.lowercase().replace("hospital-", "")
        val filename = "datasets/hospital-$normalized.ttl"
        val resource = ClassPathResource(filename)
        if (!resource.exists()) {
            throw IllegalArgumentException("Dataset not found for hospital ID: $hospitalId")
        }
        return resource.inputStream.use { stream ->
            InputStreamReader(stream, StandardCharsets.UTF_8).readText()
        }
    }
}
