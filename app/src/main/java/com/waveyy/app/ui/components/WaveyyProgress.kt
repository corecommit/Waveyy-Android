package com.waveyy.app.ui.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Determinate linear progress bar. Ports `components/ui/progress.jsx`.
 *
 * @param progress 0..1 fraction, null renders an indeterminate bar.
 * @param modifier layout modifier.
 */
@Composable
fun WaveyyProgress(
    progress: Float?,
    modifier: Modifier = Modifier
) {
    if (progress == null) {
        LinearProgressIndicator(modifier = modifier)
    } else {
        LinearProgressIndicator(progress = { progress }, modifier = modifier)
    }
}

/**
 * Indeterminate circular spinner for loading states.
 *
 * @param modifier layout modifier.
 */
@Composable
fun WaveyySpinner(
    modifier: Modifier = Modifier
) {
    CircularProgressIndicator(modifier = modifier)
}
