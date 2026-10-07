package com.waveyy.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Primary filled button. Ports `components/ui/button.jsx`.
 *
 * @param text button label.
 * @param onClick click handler.
 * @param modifier layout modifier.
 * @param enabled enabled flag.
 */
@Composable
fun WaveyyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Text(text)
    }
}

/**
 * Destructive filled button (delete/remove flows).
 *
 * @param text button label.
 * @param onClick click handler.
 * @param modifier layout modifier.
 */
@Composable
fun WaveyyDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error
        )
    ) {
        Text(text)
    }
}

/**
 * Low-emphasis text button. Ports the ghost/link button variants.
 *
 * @param text button label.
 * @param onClick click handler.
 * @param modifier layout modifier.
 */
@Composable
fun WaveyyTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text)
    }
}

/**
 * Icon slot helper for buttons with leading icons.
 */
@Composable
fun WaveyyButtonIcon(
    content: @Composable RowScope.() -> Unit
) {
    content()
}
