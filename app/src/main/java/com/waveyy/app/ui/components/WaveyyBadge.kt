package com.waveyy.app.ui.components

import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Count/label badge. Ports `components/ui/badge.jsx`.
 *
 * @param text badge text, null renders a dot badge.
 * @param modifier layout modifier.
 */
@Composable
fun WaveyyBadge(
    text: String?,
    modifier: Modifier = Modifier
) {
    Badge(modifier = modifier) {
        if (text != null) Text(text)
    }
}
