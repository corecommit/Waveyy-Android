package com.waveyy.app.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Modal bottom sheet. Ports `components/ui/drawer.jsx` + `sheet.jsx`
 * (mobile drawer behavior).
 *
 * @param onDismiss dismiss handler.
 * @param modifier sheet modifier.
 * @param content sheet content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveyyBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        content = { content() }
    )
}
