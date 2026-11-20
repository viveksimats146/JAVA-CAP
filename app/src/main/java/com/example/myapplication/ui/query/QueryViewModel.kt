package com.example.myapplication.ui.query

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.QueryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueryViewModel @Inject constructor(
    private val queryRepository: QueryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueryUiState())
    val uiState: StateFlow<QueryUiState> = _uiState.asStateFlow()

    private var queryJob: Job? = null

    fun stopPolling() {
        cancelJobs()
    }

    fun cancelJobs() {
        queryJob?.cancel()
        queryJob = null
    }

    fun onEvent(event: QueryUiEvent) {
        when (event) {
            is QueryUiEvent.UpdateQueryText -> {
                val matchesPreset = QueryPreset.entries.find { it.sparqlQuery.trim() == event.text.trim() }
                _uiState.update {
                    it.copy(
                        queryText = event.text,
                        selectedPreset = matchesPreset
                    )
                }
            }
            is QueryUiEvent.SelectPreset -> {
                _uiState.update {
                    it.copy(
                        selectedPreset = event.preset,
                        queryText = event.preset.sparqlQuery
                    )
                }
            }
            is QueryUiEvent.RunFederatedQuery -> {
                runFederatedQuery()
            }
            is QueryUiEvent.DismissPartialFailureBanner -> {
                _uiState.update { it.copy(partialFailureBanner = null) }
            }
            is QueryUiEvent.OnNavigatedToResults -> {
                _uiState.update { it.copy(navigateToResults = false) }
            }
            is QueryUiEvent.ClearQuery -> {
                _uiState.update {
                    it.copy(
                        queryText = "",
                        selectedPreset = null,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun runFederatedQuery() {
        val currentText = _uiState.value.queryText
        if (currentText.isBlank() || _uiState.value.isExecuting) return

        _uiState.update { state ->
            state.copy(
                isExecuting = true,
                errorMessage = null,
                partialFailureBanner = null,
                navigateToResults = false,
                nodeProgresses = listOf(
                    NodeProgress("hospital-a", "Hospital Node A", isLocal = true, status = NodeExecutionStatus.RUNNING),
                    NodeProgress("hospital-b", "Hospital Node B", isLocal = false, status = NodeExecutionStatus.RUNNING),
                    NodeProgress("hospital-c", "Hospital Node C", isLocal = false, status = NodeExecutionStatus.RUNNING)
                )
            )
        }

        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val result = queryRepository.executeFederatedQuery(currentText, _uiState.value.maxResults)
            val elapsedTime = System.currentTimeMillis() - startTime

            result.fold(
                onSuccess = { queryResponse ->
                    val updatedNodes = queryResponse.peerStatuses.map { peer ->
                        val mappedStatus = when (peer.status.uppercase()) {
                            "UP" -> NodeExecutionStatus.SUCCESS
                            "DEGRADED" -> NodeExecutionStatus.FAILED
                            "DOWN" -> NodeExecutionStatus.TIMED_OUT
                            else -> NodeExecutionStatus.FAILED
                        }
                        NodeProgress(
                            nodeId = peer.nodeId,
                            displayName = peer.nodeId.uppercase(),
                            isLocal = peer.nodeId.equals("hospital-a", ignoreCase = true),
                            status = mappedStatus,
                            executionTimeMs = queryResponse.executionTimeMs, // Use total query time for simplicity
                            resultCount = peer.totalTriples.toInt(), // Orchestrator sets this to results.size()
                            errorMessage = if (mappedStatus != NodeExecutionStatus.SUCCESS) "Peer status: ${peer.status}" else null
                        )
                    }

                    val hasFailures = updatedNodes.any { it.status != NodeExecutionStatus.SUCCESS }
                    val banner = if (hasFailures) {
                        "Partial Failure Warning: One or more peer hospitals did not return results. Displaying partial aggregated results."
                    } else null

                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            nodeProgresses = updatedNodes,
                            partialFailureBanner = banner,
                            totalExecutionTimeMs = queryResponse.executionTimeMs,
                            totalResultsSaved = queryResponse.results.size,
                            navigateToResults = true
                        )
                    }
                },
                onFailure = { error ->
                    val updatedNodes = listOf(
                        NodeProgress("hospital-a", "Hospital Node A", isLocal = true, status = NodeExecutionStatus.SUCCESS, executionTimeMs = 35L),
                        NodeProgress("hospital-b", "Hospital Node B", isLocal = false, status = NodeExecutionStatus.FAILED, errorMessage = error.localizedMessage),
                        NodeProgress("hospital-c", "Hospital Node C", isLocal = false, status = NodeExecutionStatus.TIMED_OUT, errorMessage = "Timeout")
                    )
                    _uiState.update {
                        it.copy(
                            isExecuting = false,
                            nodeProgresses = updatedNodes,
                            partialFailureBanner = "Federated query execution incomplete: ${error.localizedMessage}",
                            errorMessage = error.localizedMessage ?: "Execution failed"
                        )
                    }
                }
            )
        }
    }
}
