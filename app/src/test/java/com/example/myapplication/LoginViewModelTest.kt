package com.example.myapplication

import com.example.myapplication.data.dto.AuthTokenResponse
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.ui.login.LoginStatus
import com.example.myapplication.ui.login.LoginUiEvent
import com.example.myapplication.ui.login.LoginViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = LoginViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default hospital node and idle status`() {
        val state = viewModel.uiState.value
        assertEquals("Hospital Node A", state.selectedHospitalNode)
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertEquals(LoginStatus.Idle, state.loginStatus)
    }

    @Test
    fun `selecting hospital node updates ui state`() {
        viewModel.onEvent(LoginUiEvent.SelectHospitalNode("Hospital Node B"))
        assertEquals("Hospital Node B", viewModel.uiState.value.selectedHospitalNode)
    }

    @Test
    fun `login with empty credentials shows error`() {
        viewModel.onEvent(LoginUiEvent.OnLoginClicked)
        val state = viewModel.uiState.value
        assertTrue(state.loginStatus is LoginStatus.Error)
        assertEquals("Please enter both username and password.", (state.loginStatus as LoginStatus.Error).message)
    }

    @Test
    fun `login with valid credentials updates status to success`() = runTest(timeout = 5.seconds) {
        viewModel.onEvent(LoginUiEvent.OnUsernameChanged("admin"))
        viewModel.onEvent(LoginUiEvent.OnPasswordChanged("secret"))
        viewModel.onEvent(LoginUiEvent.OnLoginClicked)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.loginStatus is LoginStatus.Success)
        val success = state.loginStatus as LoginStatus.Success
        assertEquals("test-token-123", success.token)
        assertEquals("admin", success.username)
    }

    @Test
    fun `login failure shows error status`() = runTest(timeout = 5.seconds) {
        fakeAuthRepository.shouldSucceed = false
        viewModel.onEvent(LoginUiEvent.OnUsernameChanged("admin"))
        viewModel.onEvent(LoginUiEvent.OnPasswordChanged("wrongpass"))
        viewModel.onEvent(LoginUiEvent.OnLoginClicked)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.loginStatus is LoginStatus.Error)
        assertEquals("Invalid credentials", (state.loginStatus as LoginStatus.Error).message)
    }
}

class FakeAuthRepository : AuthRepository {
    var shouldSucceed = true
    private val tokenFlow = MutableStateFlow<String?>(null)

    override suspend fun login(username: String, password: String): Result<AuthTokenResponse> {
        return if (shouldSucceed) {
            tokenFlow.value = "test-token-123"
            Result.success(AuthTokenResponse(token = "test-token-123", username = username))
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    override suspend fun getToken(): String? = tokenFlow.value
    override fun observeToken(): Flow<String?> = tokenFlow
    override suspend fun logout() {
        tokenFlow.value = null
    }
}
