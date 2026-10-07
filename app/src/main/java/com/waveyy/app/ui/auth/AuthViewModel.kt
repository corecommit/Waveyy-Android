package com.waveyy.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Auth screen state. */
sealed interface AuthUiState {
    /** Idle, no request in flight. */
    data object Idle : AuthUiState

    /** Request in flight. */
    data object Loading : AuthUiState

    /** Signed in. */
    data class SignedIn(val user: AuthUser) : AuthUiState

    /** Request failed with [message]. */
    data class Error(val message: String) : AuthUiState
}

/**
 * Login/signup screen logic: password auth, signup, and Google OAuth URL
 * handoff (the screen opens the URL in a Custom Tab).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    /** Screen state. */
    val state: StateFlow<AuthUiState> = mutableState.asStateFlow()

    private val mutableGoogleUrl = MutableStateFlow<String?>(null)
    /** One-shot Google OAuth URL for the screen to open, then consume. */
    val googleUrl: StateFlow<String?> = mutableGoogleUrl.asStateFlow()

    /** Clears a surfaced error back to idle. */
    fun clearError() {
        if (mutableState.value is AuthUiState.Error) mutableState.value = AuthUiState.Idle
    }

    /** Consumes a delivered Google OAuth URL. */
    fun consumeGoogleUrl() {
        mutableGoogleUrl.value = null
    }

    /** Signs in with an email or username. */
    fun signIn(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            mutableState.value = AuthUiState.Error("Enter your login and password")
            return
        }
        mutableState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signInWithPassword(identifier, password)
                .onSuccess { mutableState.value = AuthUiState.SignedIn(it) }
                .onFailure { mutableState.value = AuthUiState.Error(it.message ?: "Sign-in failed") }
        }
    }

    /** Signs up with a username, email, and password. */
    fun signUp(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            mutableState.value = AuthUiState.Error("Fill in username, email and password")
            return
        }
        mutableState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.signUpWithPassword(username, email, password)
                .onSuccess { mutableState.value = AuthUiState.SignedIn(it) }
                .onFailure { mutableState.value = AuthUiState.Error(it.message ?: "Sign-up failed") }
        }
    }

    /** Starts Google OAuth: emits the URL for a Custom Tab. */
    fun signInWithGoogle() {
        viewModelScope.launch {
            runCatching { repository.connectGoogle() }
                .onSuccess { url -> mutableGoogleUrl.value = url }
                .onFailure { mutableState.value = AuthUiState.Error(it.message ?: "Google sign-in failed") }
        }
    }

    /** Exchanges an OAuth callback code for a session. */
    fun exchangeCode(code: String) {
        mutableState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.exchangeCode(code)
                .onSuccess {
                    val user = repository.currentUser()
                    mutableState.value = user?.let(AuthUiState::SignedIn)
                        ?: AuthUiState.Error("Sign-in produced no session")
                }
                .onFailure { mutableState.value = AuthUiState.Error(it.message ?: "Sign-in failed") }
        }
    }
}
