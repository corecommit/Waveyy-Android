package com.waveyy.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.waveyy.app.ui.theme.WaveyyShapes

/**
 * Loading placeholder block. Ports `components/ui/skeleton.jsx` (rounded
 * `bg-muted` rect): surface-variant fill with a shadcn-style pulse.
 * Never hardcodes white/black — always theme roles.
 *
 * @param modifier sized layout modifier (caller sets width/height).
 */
@Composable
fun WaveyySkeleton(
    modifier: Modifier = Modifier
) {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .clip(WaveyyShapes.small)
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
