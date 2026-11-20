package com.example.myapplication.ui.login

data class LoginUiState(
    val selectedHospitalNode: String = "Admin Portal",
    val availableHospitalNodes: List<String> = listOf("Admin Portal", "Hospital A", "Hospital B", "Hospital C"),
    val username: String = "admin",
    val password: String = "password123",
    val isPasswordVisible: Boolean = false,
    val loginStatus: LoginStatus = LoginStatus.Idle
)

sealed interface LoginStatus {
    data object Idle : LoginStatus
    data object Loading : LoginStatus
    data class Success(val token: String, val username: String) : LoginStatus
    data class Error(val message: String) : LoginStatus
}

sealed interface LoginUiEvent {
    data class SelectHospitalNode(val nodeName: String) : LoginUiEvent
    data class OnUsernameChanged(val username: String) : LoginUiEvent
    data class OnPasswordChanged(val password: String) : LoginUiEvent
    data object TogglePasswordVisibility : LoginUiEvent
    data object OnLoginClicked : LoginUiEvent
    data object DismissError : LoginUiEvent
}
