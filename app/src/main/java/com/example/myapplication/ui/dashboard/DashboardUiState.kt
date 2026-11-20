package com.example.myapplication.ui.dashboard

data class HospitalNodeStatus(
    val nodeId: String,
    val displayName: String,
    val status: String,
    val isConnected: Boolean,
    val responseTimeMs: Long,
    val totalTriples: Long,
    val uptimeSeconds: Long,
    val lastUpdated: Long
)

data class DashboardUiState(
    val nodes: List<HospitalNodeStatus> = emptyList(),
    val isPolling: Boolean = true,
    val isReloadingDataset: Boolean = false,
    val snackbarMessage: String? = null,
    val username: String? = null
)

sealed interface DashboardUiEvent {
    data object PollHealthNow : DashboardUiEvent
    data object ReloadDatasetClicked : DashboardUiEvent
    data object DismissSnackbar : DashboardUiEvent
}
