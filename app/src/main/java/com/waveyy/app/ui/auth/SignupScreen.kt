package com.waveyy.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.ui.components.WaveyyBackButton
import com.waveyy.app.ui.components.WaveyyButton
import com.waveyy.app.ui.components.WaveyyInput
import com.waveyy.app.ui.components.WaveyyTextButton

/**
 * Signup screen: username + email + password. Polished layout with back
 * navigation, keyboard flow, and error styling.
 *
 * @param viewModel screen logic.
 * @param onSignedIn callback invoked with the user id after sign-up.
 * @param onLogin callback navigating back to login.
 * @param onBack back navigation handler.
 */
@Composable
fun SignupScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSignedIn: (String) -> Unit,
    onLogin: () -> Unit,
    onBack: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val state by viewModel.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading = state is AuthUiState.Loading
    val errorMessage = (state as? AuthUiState.Error)?.message

    fun submit() {
        focusManager.clearFocus()
        viewModel.signUp(username.trim(), email.trim(), password)
    }

    LaunchedEffect(state) {
        val signedIn = state as? AuthUiState.SignedIn ?: return@LaunchedEffect
        onSignedIn(signedIn.user.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WaveyyBackButton(onClick = onBack)
        }
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Create your account",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Start building your library across devices.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        WaveyyInput(
            value = username,
            onValueChange = { username = it },
            label = "Username",
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        Spacer(Modifier.height(12.dp))
        WaveyyInput(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )
        Spacer(Modifier.height(12.dp))
        WaveyyInput(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            isError = errorMessage != null,
            supportingText = errorMessage,
            isPassword = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )
        Spacer(Modifier.height(20.dp))

        WaveyyButton(
            text = if (isLoading) "Signing up…" else "Sign up",
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && username.isNotBlank() && email.isNotBlank() && password.isNotBlank()
        )
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            WaveyyTextButton(
                text = "Have an account? Log in",
                onClick = {
                    viewModel.clearError()
                    onLogin()
                }
            )
        }
    }
}
