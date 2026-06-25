package com.example.teamb.ui.onboarding

import com.example.teamb.data.datastore.CredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** UI state for the password unlock screen. The stored password is never exposed. */
data class LoginUiState(
    val password: String = "",
    val error: String? = null,
    val unlocked: Boolean = false,
)

/** Verifies the account password without ever revealing the stored value. */
class LoginViewModel(private val credentialStore: CredentialStore) {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    /** Verifies the entered password; sets [LoginUiState.unlocked] or an inline error. */
    fun submit() {
        val pw = _state.value.password
        if (pw.isBlank()) {
            _state.update { it.copy(error = "Enter your password.") }
            return
        }
        if (credentialStore.verify(pw)) {
            _state.update { it.copy(error = null, unlocked = true) }
        } else {
            _state.update { it.copy(error = "Incorrect password.") }
        }
    }
}
