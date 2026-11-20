package com.example.myapplication

import com.example.myapplication.data.dto.QueryResponseDto
import com.example.myapplication.data.dto.RankedResultDto
import com.example.myapplication.data.local.entity.QueryResultEntity
import com.example.myapplication.data.repository.QueryRepository
import com.example.myapplication.ui.results.ResultsUiEvent
import com.example.myapplication.ui.results.ResultsViewModel
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
class ResultsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeResultsQueryRepository
    private lateinit var viewModel: ResultsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeResultsQueryRepository()
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
    fun `initial state loads cached results ordered by corroboration hospital count and score`() = runTest(timeout = 5.seconds) {
        val initialEntities = listOf(
            QueryResultEntity(
                id = 1,
                condition = "Autoimmune Ataxia",
                symptom = "Gait Unsteadiness",
                score = 0.80,
                contributingHospitals = "Hospital Node A, Hospital Node B",
                totalCases = 10
            ),
            QueryResultEntity(
                id = 2,
                condition = "Dilated Cardiomyopathy",
                symptom = "Fatigue",
                score = 0.95,
                contributingHospitals = "Hospital Node A, Hospital Node B, Hospital Node C",
                totalCases = 25
            ),
            QueryResultEntity(
                id = 3,
                condition = "Encephalopathy",
                symptom = "Seizures",
                score = 0.70,
                contributingHospitals = "Hospital Node A",
                totalCases = 5
            )
        )
        fakeRepository.setCachedEntities(initialEntities)

        viewModel = ResultsViewModel(queryRepository = fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.results.size)
        // Corroborated by 3 hospitals (id 2) comes first
        assertEquals("2", state.results[0].id)
        assertEquals("Dilated Cardiomyopathy", state.results[0].condition)
        assertEquals("2", state.selectedResultId)
    }

    @Test
    fun `select result updates selectedResultId and selectedResult`() = runTest(timeout = 5.seconds) {
        val initialEntities = listOf(
            QueryResultEntity(
                id = 1,
                condition = "Autoimmune Ataxia",
                symptom = "Gait Unsteadiness",
                score = 0.80,
                contributingHospitals = "Hospital Node A, Hospital Node B",
                totalCases = 10
            ),
            QueryResultEntity(
                id = 2,
                condition = "Dilated Cardiomyopathy",
                symptom = "Fatigue",
                score = 0.95,
                contributingHospitals = "Hospital Node A, Hospital Node B, Hospital Node C",
                totalCases = 25
            )
        )
        fakeRepository.setCachedEntities(initialEntities)

        viewModel = ResultsViewModel(queryRepository = fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ResultsUiEvent.SelectResult("1"))

        val state = viewModel.uiState.value
        assertEquals("1", state.selectedResultId)
        assertNotNull(state.selectedResult)
        assertEquals("Autoimmune Ataxia", state.selectedResult?.condition)
    }

    @Test
    fun `update search query filters results by condition or symptom`() = runTest(timeout = 5.seconds) {
        val initialEntities = listOf(
            QueryResultEntity(
                id = 1,
                condition = "Autoimmune Ataxia",
                symptom = "Gait Unsteadiness",
                score = 0.80,
                contributingHospitals = "Hospital Node A, Hospital Node B",
                totalCases = 10
            ),
            QueryResultEntity(
                id = 2,
                condition = "Dilated Cardiomyopathy",
                symptom = "Fatigue",
                score = 0.95,
                contributingHospitals = "Hospital Node A, Hospital Node B, Hospital Node C",
                totalCases = 25
            )
        )
        fakeRepository.setCachedEntities(initialEntities)

        viewModel = ResultsViewModel(queryRepository = fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ResultsUiEvent.UpdateSearchQuery("Cardio"))

        val state = viewModel.uiState.value
        assertEquals("Cardio", state.searchQuery)
        assertEquals(1, state.filteredResults.size)
        assertEquals("Dilated Cardiomyopathy", state.filteredResults[0].condition)
    }

    @Test
    fun `clear cache empties cached results and resets selection`() = runTest(timeout = 5.seconds) {
        val initialEntities = listOf(
            QueryResultEntity(
                id = 1,
                condition = "Autoimmune Ataxia",
                symptom = "Gait Unsteadiness",
                score = 0.80,
                contributingHospitals = "Hospital Node A",
                totalCases = 10
            )
        )
        fakeRepository.setCachedEntities(initialEntities)

        viewModel = ResultsViewModel(queryRepository = fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ResultsUiEvent.ClearCache)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.results.isEmpty())
        assertNull(state.selectedResultId)
    }

    @Test
    fun `dismiss error clears error message in ui state`() = runTest(timeout = 5.seconds) {
        fakeRepository.setCachedEntities(emptyList())

        viewModel = ResultsViewModel(queryRepository = fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ResultsUiEvent.DismissError)

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
    }
}

class FakeResultsQueryRepository : QueryRepository {
    private val cachedFlow = MutableStateFlow<List<QueryResultEntity>>(emptyList())

    fun setCachedEntities(entities: List<QueryResultEntity>) {
        cachedFlow.value = entities
    }

    override suspend fun executeFederatedQuery(query: String, maxResults: Int): Result<QueryResponseDto> {
        return Result.success(
            QueryResponseDto(
                queryId = "q1",
                timestamp = System.currentTimeMillis(),
                executionTimeMs = 100,
                totalResults = 0,
                results = emptyList(),
                peerStatuses = emptyList(),
                recentLogs = emptyList()
            )
        )
    }

    override fun getCachedResults(): Flow<List<QueryResultEntity>> = cachedFlow

    override suspend fun clearCache() {
        cachedFlow.value = emptyList()
    }
}
