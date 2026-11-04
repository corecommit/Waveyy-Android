package com.waveyy.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Shimmer skeleton primitives replacing the old stripe implementation.
 * Sweep: 1200ms linear loop, horizontal gradient
 * (`surfaceContainerHigh` base with an 8%-alpha white sweep) traveling
 * -200.dp → +200.dp on x. Base fill is `surfaceContainerHigh` so shapes
 * stay visible against the near-black background; every shape mirrors its
 * content counterpart's corners.
 */
@Composable
private fun shimmerBrush(sweep: Float): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(sweep, 0f),
        end = Offset(sweep + 400f, 300f)
    )
}

/**
 * Single shimmer box.
 *
 * @param width fixed width, or null to fill max width.
 * @param height fixed height.
 * @param shape corner shape matching the content shape.
 */
@Composable
fun SkeletonBox(width: Dp?, height: Dp, shape: Shape, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonSweep"
    )
    BoxWithConstraints(modifier = modifier) {
        val spanPx = with(density) { (maxWidth + 400.dp).toPx() }
        val x = with(density) { -200.dp.toPx() } + progress * spanPx
        Box(
            modifier = Modifier
                .then(if (width == null) Modifier.fillMaxWidth() else Modifier.width(width))
                .height(height)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .background(shimmerBrush(x))
        )
    }
}

/**
 * Home loading state: real header row, hero skeleton, 2x2 grid, label
 * pill, second 2x2 grid — mirroring the loaded layout shapes.
 */
@Composable
fun SkeletonHomeScreen(
    modifier: Modifier = Modifier,
    topContentPadding: Dp = 0.dp,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    header: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hazeState != null) {
                        Modifier.hazeSource(state = hazeState)
                    } else {
                        Modifier
                    }
                ),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = topContentPadding,
                bottom = 4.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                SkeletonBox(
                    width = null,
                    height = 200.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                )
            }
            items(4) {
                SkeletonBox(width = null, height = 150.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                SkeletonBox(
                    width = 120.dp,
                    height = 20.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            items(4) {
                SkeletonBox(width = null, height = 150.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            }
        }
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            header()
        }
    }
}

/**
 * Vertical shimmer rows (56.dp + text bars) for list screens.
 *
 * @param count row count.
 */
@Composable
fun SkeletonRows(count: Int = 6, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(count) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                SkeletonBox(width = 56.dp, height = 56.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    SkeletonBox(width = 180.dp, height = 16.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    SkeletonBox(
                        width = 120.dp,
                        height = 12.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Lyrics-shaped shimmer: centered bars of varying widths mimicking lyric
 * lines. Used while the provider race runs.
 *
 * @param count line count.
 */
@Composable
fun SkeletonLyrics(count: Int = 8, modifier: Modifier = Modifier) {
    val widths = listOf(220.dp, 180.dp, 240.dp, 160.dp, 200.dp, 240.dp, 180.dp, 210.dp)
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        repeat(count) { index ->
            SkeletonBox(
                width = widths[index % widths.size],
                height = 16.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
        }
    }
}

/**
 * Artist loading state: circular avatar + name/subtitle pills + hero
 * play button pill + "Popular" label + 3-podium row + "Tracks" label +
 * horizontal card rail.
 */
@Composable
fun SkeletonArtistScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // Header: avatar + name + followers
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            SkeletonBox(
                width = 120.dp,
                height = 120.dp,
                shape = androidx.compose.foundation.shape.CircleShape
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                SkeletonBox(
                    width = 160.dp,
                    height = 28.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                SkeletonBox(
                    width = 100.dp,
                    height = 16.dp,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Latest Release section
        SkeletonBox(
            width = 120.dp,
            height = 16.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        )
        SkeletonBox(
            width = 200.dp,
            height = 24.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        )
        SkeletonBox(
            width = 100.dp,
            height = 40.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
        )

        // Popular section label + 3-podium circles
        SkeletonBox(
            width = 100.dp,
            height = 20.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = androidx.compose.ui.Alignment.Bottom
        ) {
            listOf(72.dp, 96.dp, 72.dp).forEach { size ->
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    SkeletonBox(
                        width = size,
                        height = size,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    SkeletonBox(
                        width = 60.dp,
                        height = 12.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    SkeletonBox(
                        width = 80.dp,
                        height = 10.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Tracks section label + horizontal cards
        SkeletonBox(
            width = 80.dp,
            height = 20.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Column {
                    SkeletonBox(
                        width = 120.dp,
                        height = 120.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    SkeletonBox(
                        width = 100.dp,
                        height = 14.dp,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
