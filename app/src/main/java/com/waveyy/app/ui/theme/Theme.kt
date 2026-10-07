package com.waveyy.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Minimal Phase 1 theme wrapper. Phase 2 ports the full Waveyy design tokens
 * (CSS vars -> darkColorScheme, Bricolage Grotesque, shapes, motion).
 */
@Composable
fun WaveyyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}
