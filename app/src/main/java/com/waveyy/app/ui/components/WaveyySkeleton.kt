package com.waveyy.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

/**
 * Horizontal rail of card-sized shimmer tiles, matching real tile
 * dimensions (140x180dp, 16.dp corners) so loading mirrors loaded layout.
 * Shimmer sweeps onSurface at 6% base / 12% highlight. Renders [count]
 * tiles (4-6 like the data rails).
 *
 * @param count tile count.
 * @param cardWidth tile width.
 * @param cardHeight tile height.
 * @param modifier layout modifier.
 */
@Composable
fun ShimmerRail(
    count: Int = 5,
    cardWidth: Dp = 140.dp,
    cardHeight: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val sweep = rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(count) {
            val brush = Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(sweep.value * 300f - 150f, 0f),
                end = Offset(sweep.value * 300f + 150f, 200f)
            )
            Box(
                modifier = Modifier
                    .size(cardWidth, cardHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
            )
        }
    }
}

/**
 * Vertical list of 56.dp shimmer rows for list screens (search results,
 * queue). Same onSurface sweep as [ShimmerRail].
 *
 * @param count row count.
 * @param modifier layout modifier.
 */
@Composable
fun ShimmerRows(
    count: Int = 6,
    modifier: Modifier = Modifier
) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val sweep = rememberInfiniteTransition(label = "shimmerRows").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerRowsX"
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(count) {
            val brush = Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(sweep.value * 300f - 150f, 0f),
                end = Offset(sweep.value * 300f + 150f, 200f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush)
            )
        }
    }
}
