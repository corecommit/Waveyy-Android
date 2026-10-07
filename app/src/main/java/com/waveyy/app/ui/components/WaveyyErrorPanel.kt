package com.waveyy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.waveyy.app.R

/**
 * Centered error with retry. Every screen renders this (never a spinner)
 * on load failure, so errors are always visible and recoverable.
 *
 * @param onRetry retry handler.
 * @param modifier layout modifier.
 * @param message override message (defaults to the generic error string).
 */
@Composable
fun WaveyyErrorPanel(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.state_error)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium
        )
        WaveyyButton(
            text = stringResource(R.string.action_retry),
            onClick = onRetry,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
