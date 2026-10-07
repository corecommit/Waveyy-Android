package com.waveyy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.waveyy.app.ui.theme.WaveyyShapes

/**
 * Loading placeholder block. Ports `components/ui/skeleton.jsx`.
 *
 * @param modifier sized layout modifier (caller sets width/height).
 */
@Composable
fun WaveyySkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(WaveyyShapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
