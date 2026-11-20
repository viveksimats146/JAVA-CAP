package com.example.myapplication.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.entity.QueryResultEntity
import com.example.myapplication.data.repository.QueryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val queryRepository: QueryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState(isLoading = true))
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadCachedResults()
    }

    private fun loadCachedResults() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            queryRepository.getCachedResults()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load cached query results: ${error.localizedMessage}"
                        )
                    }
                }
                .collect { entities ->
                    if (entities.isEmpty()) {
                        // Seed initial demo queries into cache if completely empty
                        seedInitialDemoQueries()
                    } else {
                        val mappedResults = entities
                            .map { mapEntityToRankedResult(it) }
                            .sortedWith(
                                compareByDescending<RankedResult> { it.contributingHospitals.size }
                                    .thenByDescending { it.score }
                            )

                        _uiState.update { current ->
                            val selectedId = current.selectedResultId
                                ?: mappedResults.firstOrNull()?.id
                            current.copy(
                                isLoading = false,
                                results = mappedResults,
                                selectedResultId = selectedId,
                                errorMessage = null
                            )
                        }
                    }
                }
        }
    }

    fun stopPolling() {
        cancelJobs()
    }

    fun cancelJobs() {
        loadJob?.cancel()
        loadJob = null
    }

    private suspend fun seedInitialDemoQueries() {
        // Attempt a default query to seed cache via QueryRepository
        queryRepository.executeFederatedQuery(
            query = "PREFIX rd: <http://example.org/rdf/> SELECT ?patient ?condition ?symptom WHERE { ?patient rd:condition ?condition . ?patient rd:symptom ?symptom }",
            maxResults = 50
        )
        // If repository fails or is offline, update state to empty state
        _uiState.update {
            if (it.results.isEmpty()) {
                it.copy(isLoading = false)
            } else {
                it
            }
        }
    }

    fun onEvent(event: ResultsUiEvent) {
        when (event) {
            is ResultsUiEvent.SelectResult -> {
                _uiState.update { it.copy(selectedResultId = event.resultId) }
            }
            is ResultsUiEvent.UpdateSearchQuery -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is ResultsUiEvent.ClearCache -> {
                viewModelScope.launch {
                    try {
                        queryRepository.clearCache()
                        _uiState.update {
                            it.copy(
                                results = emptyList(),
                                selectedResultId = null,
                                isLoading = false
                            )
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(errorMessage = "Failed to clear cache: ${e.localizedMessage}")
                        }
                    }
                }
            }
            is ResultsUiEvent.Refresh -> {
                _uiState.update { it.copy(isLoading = true) }
                loadCachedResults()
            }
            is ResultsUiEvent.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    fun mapEntityToRankedResult(entity: QueryResultEntity): RankedResult {
        // NOTE: The detailed hospital breakdowns, per-hospital response times, and age-band
        // percentages in this method are synthesized display data for illustrative/demo
        // purposes, not actual telemetry derived from the real query response.
        val hospitals = entity.contributingHospitals
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("Hospital Node A") }

        val corroborationText = "Corroborated by ${hospitals.size} Hospital${if (hospitals.size > 1) "s" else ""}"
        val totalCases = if (entity.totalCases > 0) entity.totalCases else 12

        val hospitalBreakdowns = hospitals.mapIndexed { idx, hospName ->
            val isLocal = hospName.contains("Node A", ignoreCase = true) || hospName.contains("Local", ignoreCase = true) || idx == 0
            val count = (totalCases / hospitals.size) + (if (idx == 0) totalCases % hospitals.size else 0)
            HospitalBreakdown(
                hospitalName = hospName,
                patientCount = count,
                isLocal = isLocal,
                status = "ACTIVE",
                responseTimeMs = if (isLocal) 18L else (45L + idx * 30L)
            )
        }

        val ageBands = listOf(
            AgeBandSummary("35-44 yrs", (totalCases * 0.25).toInt().coerceAtLeast(1), 25f),
            AgeBandSummary("45-54 yrs", (totalCases * 0.40).toInt().coerceAtLeast(1), 40f),
            AgeBandSummary("55-64 yrs", (totalCases * 0.20).toInt().coerceAtLeast(1), 20f),
            AgeBandSummary("65+ yrs", (totalCases * 0.15).toInt().coerceAtLeast(1), 15f)
        )

        val condSlug = entity.condition.replace(" ", "")
        val sympSlug = entity.symptom.replace(" ", "")
        val sparqlTriples = listOf(
            SparqlTripleBinding("?patient", "rdf:type", "rd:${condSlug}Case"),
            SparqlTripleBinding("?patient", "rd:hasSymptom", "rd:${sympSlug}"),
            SparqlTripleBinding("?patient", "rd:corroboratingHospitals", "\"${hospitals.joinToString(", ")}\""),
            SparqlTripleBinding("?patient", "rd:aggregatedCases", "\"${totalCases}\"^^xsd:integer"),
            SparqlTripleBinding("?patient", "rd:relevanceScore", "\"${entity.score}\"^^xsd:double")
        )

        val localCount = hospitalBreakdowns.filter { it.isLocal }.sumOf { it.patientCount }
        val peerCount = hospitalBreakdowns.filter { !it.isLocal }.sumOf { it.patientCount }
        val total = (localCount + peerCount).coerceAtLeast(1)
        val localPct = (localCount.toFloat() / total) * 100f
        val peerPct = (peerCount.toFloat() / total) * 100f

        val formattedDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            .format(Date(entity.timestamp))

        return RankedResult(
            id = entity.id.toString(),
            condition = entity.condition,
            symptom = entity.symptom,
            score = entity.score,
            corroborationText = corroborationText,
            contributingHospitals = hospitals,
            totalCases = totalCases,
            timestampFormatted = formattedDate,
            hospitalBreakdowns = hospitalBreakdowns,
            ageBandSummaries = ageBands,
            sparqlTriples = sparqlTriples,
            localVsPeerDistribution = LocalVsPeerDistribution(
                localCount = localCount,
                peerCount = peerCount,
                localPercentage = localPct,
                peerPercentage = peerPct
            )
        )
    }
}
