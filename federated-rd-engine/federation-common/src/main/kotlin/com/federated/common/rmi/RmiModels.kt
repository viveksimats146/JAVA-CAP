package com.federated.common.rmi

import java.io.Serializable

data class FederatedQueryRequest(
    val queryId: String,
    val sparqlSubQuery: String,
    val originatingNodeId: String,
    val maxResults: Int = 100
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1001L
    }
}

data class FederatedQueryResponse(
    val queryId: String,
    val respondingNodeId: String,
    val success: Boolean,
    val errorMessage: String? = null,
    val executionTimeMs: Long,
    val results: List<HospitalResult>
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1002L
    }
}

data class HospitalResult(
    val patientId: String? = null,
    val condition: String,
    val symptom: String,
    val hospitalId: String,
    val ageBand: String? = null,
    val reportedDate: String? = null,
    val rawBindings: Map<String, String> = emptyMap()
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1003L
    }
}

data class NodeHealth(
    val nodeId: String,
    val status: String, // "UP", "DEGRADED", "DOWN"
    val totalTriples: Long,
    val uptimeSeconds: Long
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1004L
    }
}

data class RankedResult(
    val condition: String,
    val symptom: String,
    val score: Double,
    val contributingHospitals: List<String>,
    val totalCases: Int,
    val hospitalDetails: List<HospitalResult> = emptyList()
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1005L
    }
}
