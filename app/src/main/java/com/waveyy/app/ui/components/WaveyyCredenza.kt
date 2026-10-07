package com.waveyy.app.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * Adaptive dialog: centered [Dialog] on expanded widths, [WaveyyBottomSheet]
 * on compact/medium widths. Ports `components/ui/credenza.jsx` (dialog on
 * desktop, drawer on mobile via media query).
 *
 * @param onDismiss dismiss handler.
 * @param modifier content modifier.
 * @param content dialog/sheet content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveyyCredenza(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val expanded = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass ==
        WindowWidthSizeClass.EXPANDED
    if (expanded) {
        Dialog(onDismissRequest = onDismiss) {
            content()
        }
    } else {
        WaveyyBottomSheet(onDismiss = onDismiss, modifier = modifier) {
            content()
        }
    }
}
