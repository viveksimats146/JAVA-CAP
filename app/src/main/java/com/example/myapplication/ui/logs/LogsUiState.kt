package com.example.myapplication.ui.logs

import com.example.myapplication.data.dto.CacheStatsDto
import com.example.myapplication.data.dto.LogEntryDto

enum class LogFilter(val displayName: String) {
    ALL("All Logs"),
    DECOMPOSITION("AST Decomposition"),
    CACHE("Sub-Query Cache"),
    RMI("RMI Invocation"),
    FEDERATION("Federation Engine")
}

data class LogsUiState(
    val logs: List<LogEntryDto> = emptyList(),
    val filteredLogs: List<LogEntryDto> = emptyList(),
    val cacheStats: CacheStatsDto = CacheStatsDto(),
    val selectedFilter: LogFilter = LogFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshed: Long = System.currentTimeMillis()
)

sealed interface LogsUiEvent {
    data object RefreshLogs : LogsUiEvent
    data class SetFilter(val filter: LogFilter) : LogsUiEvent
    data object ClearLogs : LogsUiEvent
}
