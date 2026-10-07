package com.waveyy.app.ui.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.ui.components.WaveyyButton
import com.waveyy.app.ui.components.WaveyyInput
import com.waveyy.app.ui.components.WaveyyTextButton

/**
 * Login screen: email-or-username password sign-in plus Google OAuth
 * (opened in a Custom Tab). Mirrors the login mode of `AuthForm`.
 *
 * @param viewModel screen logic.
 * @param onSignedIn callback invoked with the user id after sign-in.
 * @param onSignup callback navigating to signup.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSignedIn: (String) -> Unit,
    onSignup: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val googleUrl by viewModel.googleUrl.collectAsState()
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Log in", style = MaterialTheme.typography.headlineMedium)
        WaveyyInput(
            value = identifier,
            onValueChange = { identifier = it },
            label = "Email or username",
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
            text = if (state == AuthUiState.Loading) "Signing in…" else "Log in",
            onClick = { viewModel.signIn(identifier, password) },
            modifier = Modifier.fillMaxWidth()
        )
        WaveyyButton(
            text = "Continue with Google",
            onClick = { viewModel.signInWithGoogle() },
            modifier = Modifier.fillMaxWidth()
        )
        WaveyyTextButton(
            text = "No account? Sign up",
            onClick = {
                viewModel.clearError()
                onSignup()
            }
        )
    }
}
