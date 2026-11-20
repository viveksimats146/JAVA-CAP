package com.example.myapplication.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.dto.LogEntryDto
import com.example.myapplication.data.repository.LogsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logsRepository: LogsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    private var logsJob: Job? = null

    init {
        loadLogsAndCacheStats()
    }

    fun stopPolling() {
        cancelJobs()
    }

    fun cancelJobs() {
        logsJob?.cancel()
        logsJob = null
    }

    fun onEvent(event: LogsUiEvent) {
        when (event) {
            is LogsUiEvent.RefreshLogs -> {
                loadLogsAndCacheStats()
            }
            is LogsUiEvent.SetFilter -> {
                _uiState.update { state ->
                    val filtered = applyFilter(state.logs, event.filter)
                    state.copy(
                        selectedFilter = event.filter,
                        filteredLogs = filtered
                    )
                }
            }
            is LogsUiEvent.ClearLogs -> {
                _uiState.update {
                    it.copy(
                        logs = emptyList(),
                        filteredLogs = emptyList()
                    )
                }
            }
        }
    }

    fun loadLogsAndCacheStats() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        logsJob?.cancel()
        logsJob = viewModelScope.launch {
            val logsResult = logsRepository.fetchRecentLogs(50)
            val statsResult = logsRepository.fetchCacheStats()

            val logs = logsResult.getOrDefault(emptyList())
            val stats = statsResult.getOrDefault(_uiState.value.cacheStats)
            val filtered = applyFilter(logs, _uiState.value.selectedFilter)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    logs = logs,
                    filteredLogs = filtered,
                    cacheStats = stats,
                    lastRefreshed = System.currentTimeMillis()
                )
            }
        }
    }

    private fun applyFilter(logs: List<LogEntryDto>, filter: LogFilter): List<LogEntryDto> {
        return when (filter) {
            LogFilter.ALL -> logs
            LogFilter.DECOMPOSITION -> logs.filter {
                it.tag.contains("DECOMPOSITION", ignoreCase = true) ||
                        it.tag.contains("AST", ignoreCase = true) ||
                        it.message.contains("AST", ignoreCase = true) ||
                        it.message.contains("clauses", ignoreCase = true)
            }
            LogFilter.CACHE -> logs.filter {
                it.tag.contains("CACHE", ignoreCase = true) ||
                        it.message.contains("cache", ignoreCase = true)
            }
            LogFilter.RMI -> logs.filter {
                it.tag.contains("RMI", ignoreCase = true) ||
                        it.tag.contains("LOCAL_EXEC", ignoreCase = true) ||
                        it.tag.contains("PEER", ignoreCase = true) ||
                        it.message.contains("RMI", ignoreCase = true) ||
                        it.message.contains("Peer", ignoreCase = true)
            }
            LogFilter.FEDERATION -> logs.filter {
                it.tag.contains("FEDERATION", ignoreCase = true) ||
                        it.tag.contains("ALIGNMENT", ignoreCase = true) ||
                        it.message.contains("federat", ignoreCase = true)
            }
        }
    }
}
