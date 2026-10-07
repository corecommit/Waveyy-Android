package com.waveyy.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.ui.components.WaveyyButton
import com.waveyy.app.ui.components.WaveyyInput
import com.waveyy.app.ui.components.WaveyyTextButton

/**
 * Signup screen: username + email + password, mirroring the signup mode of
 * `AuthForm` (3-30 letters/digits/underscores enforced by the repository).
 *
 * @param viewModel screen logic.
 * @param onSignedIn callback invoked with the user id after sign-up.
 * @param onLogin callback navigating back to login.
 */
@Composable
fun SignupScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSignedIn: (String) -> Unit,
    onLogin: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        val signedIn = state as? AuthUiState.SignedIn ?: return@LaunchedEffect
        onSignedIn(signedIn.user.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Sign up", style = MaterialTheme.typography.headlineMedium)
        WaveyyInput(
            value = username,
            onValueChange = { username = it },
            label = "Username",
            modifier = Modifier.fillMaxWidth()
        )
        WaveyyInput(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            modifier = Modifier.fillMaxWidth()
        )
        WaveyyInput(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            modifier = Modifier.fillMaxWidth()
        )
        if (state is AuthUiState.Error) {
            Text(
                text = (state as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
        WaveyyButton(
            text = if (state == AuthUiState.Loading) "Signing up…" else "Sign up",
            onClick = { viewModel.signUp(username, email, password) },
            modifier = Modifier.fillMaxWidth()
        )
        WaveyyTextButton(
            text = "Have an account? Log in",
            onClick = {
                viewModel.clearError()
                onLogin()
            }
        )
    }
}
