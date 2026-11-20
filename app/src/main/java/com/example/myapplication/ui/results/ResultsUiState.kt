package com.example.myapplication.ui.results

data class RankedResult(
    val id: String,
    val condition: String,
    val symptom: String,
    val score: Double,
    val corroborationText: String,
    val contributingHospitals: List<String>,
    val totalCases: Int,
    val timestampFormatted: String,
    val hospitalBreakdowns: List<HospitalBreakdown>,
    val ageBandSummaries: List<AgeBandSummary>,
    val sparqlTriples: List<SparqlTripleBinding>,
    val localVsPeerDistribution: LocalVsPeerDistribution
)

data class HospitalBreakdown(
    val hospitalName: String,
    val patientCount: Int,
    val isLocal: Boolean,
    val status: String = "ACTIVE",
    val responseTimeMs: Long = 45L
)

data class AgeBandSummary(
    val ageBand: String,
    val patientCount: Int,
    val percentage: Float
)

data class SparqlTripleBinding(
    val subject: String,
    val predicate: String,
    val objectValue: String
)

data class LocalVsPeerDistribution(
    val localCount: Int,
    val peerCount: Int,
    val localPercentage: Float,
    val peerPercentage: Float
)

data class ResultsUiState(
    val isLoading: Boolean = false,
    val results: List<RankedResult> = emptyList(),
    val selectedResultId: String? = null,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val isOfflineMode: Boolean = true
) {
    val selectedResult: RankedResult?
        get() = results.find { it.id == selectedResultId } ?: results.firstOrNull()

    val filteredResults: List<RankedResult>
        get() = if (searchQuery.isBlank()) {
            results
        } else {
            results.filter {
                it.condition.contains(searchQuery, ignoreCase = true) ||
                it.symptom.contains(searchQuery, ignoreCase = true) ||
                it.contributingHospitals.any { h -> h.contains(searchQuery, ignoreCase = true) }
            }
        }
}

sealed interface ResultsUiEvent {
    data class SelectResult(val resultId: String?) : ResultsUiEvent
    data class UpdateSearchQuery(val query: String) : ResultsUiEvent
    object ClearCache : ResultsUiEvent
    object Refresh : ResultsUiEvent
    object DismissError : ResultsUiEvent
}
