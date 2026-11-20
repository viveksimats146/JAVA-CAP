package com.example.myapplication.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.TokenManager
import com.example.myapplication.data.local.entity.NodeHealthEntity
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.NodeHealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val nodeHealthRepository: NodeHealthRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        observeUsername()
        observeCachedNodeHealth()
        startHealthPolling()
    }

    private fun observeUsername() {
        viewModelScope.launch {
            tokenManager.usernameFlow.collect { username ->
                _uiState.update { it.copy(username = username) }
            }
        }
    }

    private fun observeCachedNodeHealth() {
        viewModelScope.launch {
            nodeHealthRepository.getCachedNodeHealth().collect { entities ->
                val updatedNodes = mapEntitiesToHospitalNodes(entities)
                _uiState.update { it.copy(nodes = updatedNodes) }
            }
        }
    }

    private fun startHealthPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                pollNodeHealth()
                delay(5000) // Poll health every 5 seconds
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun cancelJobs() {
        stopPolling()
    }

    private suspend fun pollNodeHealth() {
        val result = nodeHealthRepository.fetchNodeHealth()
        result.fold(
            onSuccess = { dto ->
                // Successfully fetched backend health endpoint
            },
            onFailure = {
                // Ignore network error in background polling, keeping cached data
            }
        )
    }

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            is DashboardUiEvent.PollHealthNow -> {
                viewModelScope.launch { pollNodeHealth() }
            }
            is DashboardUiEvent.ReloadDatasetClicked -> {
                reloadDataset()
            }
            is DashboardUiEvent.DismissSnackbar -> {
                _uiState.update { it.copy(snackbarMessage = null) }
            }
        }
    }

    private fun reloadDataset() {
        if (_uiState.value.isReloadingDataset) return

        _uiState.update { it.copy(isReloadingDataset = true) }

        viewModelScope.launch {
            val result = nodeHealthRepository.reloadDataset()
            result.fold(
                onSuccess = { msg ->
                    _uiState.update {
                        it.copy(
                            isReloadingDataset = false,
                            snackbarMessage = msg
                        )
                    }
                    pollNodeHealth()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isReloadingDataset = false,
                            snackbarMessage = "Reload failed: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLogoutComplete()
        }
    }

    private fun mapEntitiesToHospitalNodes(entities: List<NodeHealthEntity>): List<HospitalNodeStatus> {
        val entityMap = entities.associateBy { it.nodeId.lowercase() }

        val defaultNodes = listOf(
            Triple("hospital-node-a", "Hospital Node A", 1250000L),
            Triple("hospital-node-b", "Hospital Node B", 980000L),
            Triple("hospital-node-c", "Hospital Node C", 1100000L)
        )

        return defaultNodes.map { (id, name, defaultTriples) ->
            val entity = entityMap[id] ?: entityMap[name.lowercase()]
            HospitalNodeStatus(
                nodeId = id,
                displayName = name,
                status = entity?.status ?: "PENDING",
                isConnected = entity?.status != "DOWN" && entity?.status != "OFFLINE",
                responseTimeMs = entity?.responseTimeMs ?: 0L,
                totalTriples = if (entity != null && entity.totalTriples > 0) entity.totalTriples else defaultTriples,
                uptimeSeconds = entity?.uptimeSeconds ?: 0L,
                lastUpdated = entity?.lastUpdated ?: System.currentTimeMillis()
            )
        }
    }
}
