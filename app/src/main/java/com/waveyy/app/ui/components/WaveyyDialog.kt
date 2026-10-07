package com.waveyy.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Confirmation dialog. Ports `components/ui/dialog.jsx`.
 *
 * @param title dialog title.
 * @param text dialog body.
 * @param confirmLabel confirm button label.
 * @param onConfirm confirm handler (also dismisses).
 * @param onDismiss dismiss handler.
 * @param dismissLabel dismiss button label, null hides the button.
 */
@Composable
fun WaveyyDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = dismissLabel?.let { label ->
            { TextButton(onClick = onDismiss) { Text(label) } }
        }
    )
}
