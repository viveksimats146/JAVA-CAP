package com.example.myapplication

import com.example.myapplication.data.dto.QueryResponseDto
import com.example.myapplication.data.dto.RankedResultDto
import com.example.myapplication.data.local.entity.QueryResultEntity
import com.example.myapplication.data.repository.QueryRepository
import com.example.myapplication.ui.query.NodeExecutionStatus
import com.example.myapplication.ui.query.QueryPreset
import com.example.myapplication.ui.query.QueryUiEvent
import com.example.myapplication.ui.query.QueryViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QueryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeQueryRepository: FakeQueryRepository
    private lateinit var viewModel: QueryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeQueryRepository = FakeQueryRepository()
        viewModel = QueryViewModel(queryRepository = fakeQueryRepository)
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
    fun `initial state loads default AUTOIMMUNE_ATAXIA preset query`() = runTest(timeout = 5.seconds) {
        val state = viewModel.uiState.value
        assertEquals(QueryPreset.AUTOIMMUNE_ATAXIA, state.selectedPreset)
        assertTrue(state.queryText.contains("Autoimmune"))
        assertFalse(state.isExecuting)
        assertEquals(3, state.nodeProgresses.size)
    }

    @Test
    fun `select preset updates selectedPreset and queryText`() = runTest(timeout = 5.seconds) {
        viewModel.onEvent(QueryUiEvent.SelectPreset(QueryPreset.CARDIOMYOPATHY_FATIGUE))

        val state = viewModel.uiState.value
        assertEquals(QueryPreset.CARDIOMYOPATHY_FATIGUE, state.selectedPreset)
        assertTrue(state.queryText.contains("Cardiomyopathy"))
    }

    @Test
    fun `update query text updates queryText and matches preset or clears preset if custom`() = runTest(timeout = 5.seconds) {
        viewModel.onEvent(QueryUiEvent.UpdateQueryText("SELECT ?patient WHERE { ?patient ?p ?o }"))

        val state = viewModel.uiState.value
        assertEquals("SELECT ?patient WHERE { ?patient ?p ?o }", state.queryText)
        assertNull(state.selectedPreset)
    }

    @Test
    fun `run federated query executes query and updates results state`() = runTest(timeout = 5.seconds) {
        fakeQueryRepository.mockResults = listOf(
            RankedResultDto(condition = "Autoimmune", symptom = "Ataxia", score = 0.95, totalCases = 15)
        )

        viewModel.onEvent(QueryUiEvent.RunFederatedQuery)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isExecuting)
        assertTrue(state.navigateToResults)
        assertEquals(1, state.totalResultsSaved)
        assertEquals(NodeExecutionStatus.SUCCESS, state.nodeProgresses[0].status)
        assertEquals(NodeExecutionStatus.SUCCESS, state.nodeProgresses[1].status)
    }

    @Test
    fun `run federated query with encephalopathy query triggers partial failure banner for node C timeout`() = runTest(timeout = 5.seconds) {
        viewModel.onEvent(QueryUiEvent.SelectPreset(QueryPreset.ENCEPHALOPATHY_SEIZURES))
        viewModel.onEvent(QueryUiEvent.RunFederatedQuery)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isExecuting)
        assertNotNull(state.partialFailureBanner)
        assertTrue(state.partialFailureBanner!!.contains("Hospital Node C timed out"))
        assertEquals(NodeExecutionStatus.TIMED_OUT, state.nodeProgresses[2].status)
    }

    @Test
    fun `dismiss partial failure banner clears banner`() = runTest(timeout = 5.seconds) {
        viewModel.onEvent(QueryUiEvent.SelectPreset(QueryPreset.ENCEPHALOPATHY_SEIZURES))
        viewModel.onEvent(QueryUiEvent.RunFederatedQuery)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(QueryUiEvent.DismissPartialFailureBanner)

        val state = viewModel.uiState.value
        assertNull(state.partialFailureBanner)
    }

    @Test
    fun `clear query resets queryText and selectedPreset`() = runTest(timeout = 5.seconds) {
        viewModel.onEvent(QueryUiEvent.ClearQuery)

        val state = viewModel.uiState.value
        assertEquals("", state.queryText)
        assertNull(state.selectedPreset)
    }
}

class FakeQueryRepository : QueryRepository {
    var mockResults: List<RankedResultDto> = emptyList()
    var shouldFail = false
    private val cachedFlow = MutableStateFlow<List<QueryResultEntity>>(emptyList())

    override suspend fun executeFederatedQuery(query: String, maxResults: Int): Result<QueryResponseDto> {
        return if (shouldFail) {
            Result.failure(Exception("Network Timeout"))
        } else {
            Result.success(
                QueryResponseDto(
                    queryId = "q1",
                    timestamp = System.currentTimeMillis(),
                    executionTimeMs = 120,
                    totalResults = mockResults.size,
                    results = mockResults,
                    peerStatuses = emptyList(),
                    recentLogs = emptyList()
                )
            )
        }
    }

    override fun getCachedResults(): Flow<List<QueryResultEntity>> = cachedFlow

    override suspend fun clearCache() {
        cachedFlow.value = emptyList()
    }
}
