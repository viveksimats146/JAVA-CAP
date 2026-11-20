package com.example.myapplication.ui.query

enum class QueryPreset(
    val title: String,
    val subtitle: String,
    val sparqlQuery: String
) {
    AUTOIMMUNE_ATAXIA(
        title = "Cross-Hospital Correlation: Autoimmune & Ataxia",
        subtitle = "Autoimmune & Ataxia overlap across Node A and Node B",
        sparqlQuery = """
            PREFIX ex: <http://example.org/health#>
            
            SELECT ?patient ?condition ?symptom WHERE {
              ?patient ex:diagnosedWith ?condition .
              ?patient ex:hasSymptom ?symptom .
              FILTER(?condition = "Autoimmune" && ?symptom = "Ataxia")
            }
            LIMIT 100
        """.trimIndent()
    ),
    CARDIOMYOPATHY_FATIGUE(
        title = "Multi-Node Corroboration: Cardiomyopathy & Fatigue",
        subtitle = "Cardiomyopathy & Fatigue overlap across Node A, B, and C",
        sparqlQuery = """
            PREFIX ex: <http://example.org/health#>
            
            SELECT ?patient ?condition ?symptom WHERE {
              ?patient ex:diagnosedWith ?condition .
              ?patient ex:hasSymptom ?symptom .
              FILTER(?condition = "Cardiomyopathy" && ?symptom = "Fatigue")
            }
            LIMIT 100
        """.trimIndent()
    ),
    ENCEPHALOPATHY_SEIZURES(
        title = "All-Node Overlap: Encephalopathy & Seizures",
        subtitle = "Rare disease overlap across all federated hospital nodes",
        sparqlQuery = """
            PREFIX ex: <http://example.org/health#>
            
            SELECT ?patient ?condition ?symptom WHERE {
              ?patient ex:diagnosedWith ?condition .
              ?patient ex:hasSymptom ?symptom .
              FILTER(?condition = "Encephalopathy" && ?symptom = "Seizures")
            }
            LIMIT 100
        """.trimIndent()
    )
}

enum class NodeExecutionStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    TIMED_OUT,
    FAILED
}

data class NodeProgress(
    val nodeId: String,
    val displayName: String,
    val isLocal: Boolean,
    val status: NodeExecutionStatus = NodeExecutionStatus.IDLE,
    val executionTimeMs: Long? = null,
    val resultCount: Int? = null,
    val errorMessage: String? = null
)

data class QueryUiState(
    val queryText: String = QueryPreset.AUTOIMMUNE_ATAXIA.sparqlQuery,
    val selectedPreset: QueryPreset? = QueryPreset.AUTOIMMUNE_ATAXIA,
    val maxResults: Int = 100,
    val isExecuting: Boolean = false,
    val nodeProgresses: List<NodeProgress> = listOf(
        NodeProgress("hospital-a", "Hospital Node A", isLocal = true),
        NodeProgress("hospital-b", "Hospital Node B", isLocal = false),
        NodeProgress("hospital-c", "Hospital Node C", isLocal = false)
    ),
    val partialFailureBanner: String? = null,
    val totalExecutionTimeMs: Long? = null,
    val totalResultsSaved: Int? = null,
    val errorMessage: String? = null,
    val navigateToResults: Boolean = false
)

sealed interface QueryUiEvent {
    data class UpdateQueryText(val text: String) : QueryUiEvent
    data class SelectPreset(val preset: QueryPreset) : QueryUiEvent
    data object RunFederatedQuery : QueryUiEvent
    data object DismissPartialFailureBanner : QueryUiEvent
    data object OnNavigatedToResults : QueryUiEvent
    data object ClearQuery : QueryUiEvent
}
