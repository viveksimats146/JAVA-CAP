package com.example.myapplication

import com.example.myapplication.data.dto.NodeHealthDto
import com.example.myapplication.data.local.TokenManager
import com.example.myapplication.data.local.entity.NodeHealthEntity
import com.example.myapplication.data.repository.NodeHealthRepository
import com.example.myapplication.ui.dashboard.DashboardUiEvent
import com.example.myapplication.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeNodeHealthRepository: FakeNodeHealthRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeTokenManager: FakeTokenManager
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeNodeHealthRepository = FakeNodeHealthRepository()
        fakeAuthRepository = FakeAuthRepository()
        fakeTokenManager = FakeTokenManager()
        viewModel = DashboardViewModel(
            nodeHealthRepository = fakeNodeHealthRepository,
            authRepository = fakeAuthRepository,
            tokenManager = fakeTokenManager
        )
        viewModel.stopPolling()
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.stopPolling()
            viewModel.cancelJobs()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state maps hospital nodes A B and C`() = runTest(timeout = 5.seconds) {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(3, state.nodes.size)
        assertEquals("Hospital Node A", state.nodes[0].displayName)
        assertEquals("Hospital Node B", state.nodes[1].displayName)
        assertEquals("Hospital Node C", state.nodes[2].displayName)
    }

    @Test
    fun `reload dataset triggers reload and updates snackbar message`() = runTest(timeout = 5.seconds) {
        viewModel.onEvent(DashboardUiEvent.ReloadDatasetClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isReloadingDataset)
        assertEquals("Dataset reloaded successfully", state.snackbarMessage)
    }

    @Test
    fun `dismiss snackbar clears snackbar message`() = runTest(timeout = 5.seconds) {
        viewModel.onEvent(DashboardUiEvent.ReloadDatasetClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(DashboardUiEvent.DismissSnackbar)
        assertNull(viewModel.uiState.value.snackbarMessage)
    }
}

class FakeNodeHealthRepository : NodeHealthRepository {
    var reloadSucceeds = true
    private val cachedFlow = MutableStateFlow<List<NodeHealthEntity>>(emptyList())

    override suspend fun fetchNodeHealth(): Result<List<NodeHealthDto>> {
        return Result.success(listOf(NodeHealthDto(nodeId = "hospital-node-a", status = "HEALTHY", totalTriples = 1250000L)))
    }

    override fun getCachedNodeHealth(): Flow<List<NodeHealthEntity>> = cachedFlow

    override suspend fun reloadDataset(): Result<String> {
        return if (reloadSucceeds) {
            Result.success("Dataset reloaded successfully")
        } else {
            Result.failure(Exception("Reload failed"))
        }
    }
}

class FakeTokenManager : TokenManager(null) {
    override val usernameFlow: Flow<String?> = MutableStateFlow("Dr. Test")
    override val authTokenFlow: Flow<String?> = MutableStateFlow("fake-token")
}
