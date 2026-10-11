package com.waveyy.app.ui.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.ui.components.WaveyyBackButton
import com.waveyy.app.ui.components.WaveyyButton
import com.waveyy.app.ui.components.WaveyyInput
import com.waveyy.app.ui.components.WaveyyOutlineButton
import com.waveyy.app.ui.components.WaveyyTextButton

/**
 * Login screen: email-or-username password sign-in plus Google OAuth.
 * Polished layout with back navigation, keyboard flow, and error styling.
 *
 * @param viewModel screen logic.
 * @param onSignedIn callback invoked with the user id after sign-in.
 * @param onSignup callback navigating to signup.
 * @param onBack back navigation handler.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSignedIn: (String) -> Unit,
    onSignup: () -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val state by viewModel.state.collectAsState()
    val googleUrl by viewModel.googleUrl.collectAsState()
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading = state is AuthUiState.Loading
    val errorMessage = (state as? AuthUiState.Error)?.message

    fun submit() {
        focusManager.clearFocus()
        viewModel.signIn(identifier.trim(), password)
    }

    LaunchedEffect(googleUrl) {
        val url = googleUrl ?: return@LaunchedEffect
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        viewModel.consumeGoogleUrl()
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
            text = "Welcome back",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Log in to sync your playlists and likes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))

        WaveyyInput(
            value = identifier,
            onValueChange = { identifier = it },
            label = "Email or username",
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
            text = if (isLoading) "Signing in…" else "Log in",
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && identifier.isNotBlank() && password.isNotBlank()
        )
        Spacer(Modifier.height(12.dp))
        WaveyyOutlineButton(
            text = "Continue with Google",
            onClick = { viewModel.signInWithGoogle() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            WaveyyTextButton(
                text = "No account? Sign up",
                onClick = {
                    viewModel.clearError()
                    onSignup()
                }
            )
        }
    }
}
