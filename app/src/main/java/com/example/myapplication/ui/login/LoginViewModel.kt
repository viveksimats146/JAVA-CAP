package com.example.myapplication.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.SelectHospitalNode -> {
                val (defaultUser, defaultPass) = when (event.nodeName) {
                    "Admin", "Admin Portal" -> Pair("admin", "password123")
                    "Hospital A", "Hospital Node A" -> Pair("doctor_a", "doctor123")
                    "Hospital B", "Hospital Node B" -> Pair("doctor_b", "doctor123")
                    "Hospital C", "Hospital Node C" -> Pair("doctor_c", "doctor123")
                    else -> Pair("admin", "password123")
                }
                _uiState.update {
                    it.copy(
                        selectedHospitalNode = event.nodeName,
                        username = defaultUser,
                        password = defaultPass,
                        loginStatus = LoginStatus.Idle
                    )
                }
            }
            is LoginUiEvent.OnUsernameChanged -> {
                _uiState.update { it.copy(username = event.username) }
            }
            is LoginUiEvent.OnPasswordChanged -> {
                _uiState.update { it.copy(password = event.password) }
            }
            is LoginUiEvent.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is LoginUiEvent.OnLoginClicked -> {
                login()
            }
            is LoginUiEvent.DismissError -> {
                _uiState.update { it.copy(loginStatus = LoginStatus.Idle) }
            }
        }
    }

    private fun login() {
        val currentState = _uiState.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _uiState.update {
                it.copy(loginStatus = LoginStatus.Error("Please enter both username and password."))
            }
            return
        }

        _uiState.update { it.copy(loginStatus = LoginStatus.Loading) }

        viewModelScope.launch {
            val result = authRepository.login(
                username = currentState.username.trim(),
                password = currentState.password.trim()
            )

            result.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            loginStatus = LoginStatus.Success(
                                token = response.token,
                                username = response.username
                            )
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            loginStatus = LoginStatus.Error(
                                message = error.localizedMessage ?: "Authentication failed."
                            )
                        )
                    }
                }
            )
        }
    }
}
