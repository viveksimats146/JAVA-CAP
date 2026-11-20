package com.example.myapplication

import com.example.myapplication.data.dto.CacheStatsDto
import com.example.myapplication.data.dto.LogEntryDto
import com.example.myapplication.data.repository.LogsRepository
import com.example.myapplication.ui.logs.LogFilter
import com.example.myapplication.ui.logs.LogsUiEvent
import com.example.myapplication.ui.logs.LogsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeLogsRepository: FakeLogsRepository
    private lateinit var viewModel: LogsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeLogsRepository = FakeLogsRepository()
        viewModel = LogsViewModel(logsRepository = fakeLogsRepository)
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
    fun `initial state fetches recent logs and cache statistics`() = runTest(timeout = 5.seconds) {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(5, state.logs.size)
        assertEquals(42L, state.cacheStats.hitCount)
        assertEquals(18L, state.cacheStats.missCount)
    }

    @Test
    fun `filter logs by DECOMPOSITION filter`() = runTest(timeout = 5.seconds) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(LogsUiEvent.SetFilter(LogFilter.DECOMPOSITION))

        val state = viewModel.uiState.value
        assertEquals(LogFilter.DECOMPOSITION, state.selectedFilter)
        assertTrue(state.filteredLogs.all {
            it.tag.contains("DECOMPOSITION", ignoreCase = true) ||
                    it.tag.contains("AST", ignoreCase = true) ||
                    it.message.contains("AST", ignoreCase = true) ||
                    it.message.contains("clauses", ignoreCase = true)
        })
        assertEquals(2, state.filteredLogs.size)
    }

    @Test
    fun `filter logs by CACHE filter`() = runTest(timeout = 5.seconds) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(LogsUiEvent.SetFilter(LogFilter.CACHE))

        val state = viewModel.uiState.value
        assertEquals(LogFilter.CACHE, state.selectedFilter)
        assertEquals(1, state.filteredLogs.size)
        assertEquals("CACHE_HIT", state.filteredLogs[0].tag)
    }

    @Test
    fun `filter logs by RMI filter`() = runTest(timeout = 5.seconds) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(LogsUiEvent.SetFilter(LogFilter.RMI))

        val state = viewModel.uiState.value
        assertEquals(LogFilter.RMI, state.selectedFilter)
        assertEquals(1, state.filteredLogs.size)
        assertEquals("LOCAL_EXEC", state.filteredLogs[0].tag)
    }

    @Test
    fun `refresh logs reloads recent logs`() = runTest(timeout = 5.seconds) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(LogsUiEvent.RefreshLogs)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(5, state.logs.size)
    }

    @Test
    fun `clear logs empties logs and filteredLogs`() = runTest(timeout = 5.seconds) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(LogsUiEvent.ClearLogs)

        val state = viewModel.uiState.value
        assertTrue(state.logs.isEmpty())
        assertTrue(state.filteredLogs.isEmpty())
    }
}

class FakeLogsRepository : LogsRepository {
    var mockLogs: List<LogEntryDto> = listOf(
        LogEntryDto(timestamp = 1000L, level = "INFO", tag = "FEDERATION", message = "Starting federated query"),
        LogEntryDto(timestamp = 1100L, level = "INFO", tag = "AST_COST", message = "Query complexity score: 2.5"),
        LogEntryDto(timestamp = 1200L, level = "INFO", tag = "DECOMPOSITION", message = "Local clauses: 2"),
        LogEntryDto(timestamp = 1300L, level = "INFO", tag = "LOCAL_EXEC", message = "Local execution completed in 34ms"),
        LogEntryDto(timestamp = 1400L, level = "INFO", tag = "CACHE_HIT", message = "Sub-query cache hit for peer node")
    )

    var mockCacheStats = CacheStatsDto(
        hitCount = 42L,
        missCount = 18L,
        hitRate = 0.70,
        evictionCount = 3L,
        size = 12L
    )

    override suspend fun fetchRecentLogs(limit: Int): Result<List<LogEntryDto>> {
        return Result.success(mockLogs)
    }

    override suspend fun fetchCacheStats(): Result<CacheStatsDto> {
        return Result.success(mockCacheStats)
    }
}
