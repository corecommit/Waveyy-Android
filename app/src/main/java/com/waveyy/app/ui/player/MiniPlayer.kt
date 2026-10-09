package com.waveyy.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.PlaybackConnection
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.library.LikeSongButton
import com.waveyy.app.ui.theme.rememberAlbumAccent
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs

/**
 * Floating pill mini-player: rounded thumb, title/artist, like + play
 * controls. Sits 8.dp above the bottom bar with 12.dp side margins.
 * Tap (outside buttons) opens the full player. Four-way swipe:
 * left = next, right = previous, down = pause + hide, up = full player.
 * Ports `components/player/mini-player.jsx`.
 *
 * @param connection playback connection.
 * @param currentSong currently loaded song metadata, if any.
 * @param onExpand expand-to-full-player handler.
 * @param modifier layout modifier.
 * @param playerVm shared player logic (hidden flag).
 */
@Composable
fun MiniPlayer(
    connection: PlaybackConnection,
    currentSong: Song?,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    playerVm: PlayerViewModel = sharedPlayerViewModel()
) {
    val state by connection.state.collectAsState()
    val hidden by playerVm.miniPlayerHidden.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    if (state.mediaId == null || currentSong == null || hidden) return
    LaunchedEffect(currentSong.id) { playerVm.clearMiniPlayerHidden() }

    // Raw drag amounts — unbounded, drive action thresholds.
    var rawDragX by remember(currentSong.id) { mutableFloatStateOf(0f) }
    var rawDragY by remember(currentSong.id) { mutableFloatStateOf(0f) }
    val thresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    val maxVisual = with(LocalDensity.current) { 6.dp.toPx() }

    // Visual offsets spring back to 0 smoothly after release.
    val visualX = remember(currentSong.id) { Animatable(0f) }
    val visualY = remember(currentSong.id) { Animatable(0f) }

    LaunchedEffect(rawDragX) {
        val target = rawDragX.coerceIn(-maxVisual, maxVisual)
        visualX.animateTo(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    LaunchedEffect(rawDragY) {
        val target = rawDragY.coerceIn(-maxVisual, maxVisual)
        visualY.animateTo(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    // Visible translation comes from visualX/visualY below.
    var hapticFired by remember(currentSong.id) { mutableStateOf(false) }
    val accent = rememberAlbumAccent(currentSong.imageUrl, currentSong.id)

    val dragAlpha = { value: Float -> (abs(value) / thresholdPx).coerceIn(0f, 1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .pointerInput(currentSong.id) {
                detectDragGestures(
                    onDragStart = { hapticFired = false },
                    onDragEnd = {
                        Timber.tag("MiniPlayer").d("drag end: x=$rawDragX y=$rawDragY")
                        when {
                            rawDragX > thresholdPx -> scope.launch { connection.previous() }
                            rawDragX < -thresholdPx -> scope.launch { connection.next() }
                            rawDragY > thresholdPx -> playerVm.stopAndHide()
                        }
                        // Setting these to 0 triggers the LaunchedEffect spring-back above.
                        rawDragX = 0f
                        rawDragY = 0f
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        // Raw drag: unbounded, drives the action threshold.
                        rawDragX += drag.x
                        rawDragY += drag.y
                        val crossed = abs(rawDragX) > thresholdPx || abs(rawDragY) > thresholdPx
                        if (crossed && !hapticFired) {
                            hapticFired = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .background(accent.copy(alpha = 0.08f))
                .graphicsLayer {
                    translationX = visualX.value
                    translationY = visualY.value
                    alpha = if (rawDragY > 0f) {
                        (1f - rawDragY / (thresholdPx * 1.5f)).coerceIn(0.4f, 1f)
                    } else {
                        1f
                    }
                    val s = if (abs(visualX.value) > 2f || abs(visualY.value) > 2f) 1.02f else 1.0f
                    scaleX = s
                    scaleY = s
                }
                .clickable(onClick = onExpand)
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdaptiveImage(
                imageUrl = currentSong.imageUrl,
                contentDescription = currentSong.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = currentSong.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LikeSongButton(song = currentSong)
            IconButton(onClick = { scope.launch { connection.togglePlayPause() } }) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play"
                )
            }
        }
        // Directional hints that fade in while dragging.
        if (visualX.value < -2f) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = dragAlpha(visualX.value)),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)
            )
        }
        if (visualX.value > 2f) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = dragAlpha(visualX.value)),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp)
            )
        }
        if (visualY.value > 2f) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = dragAlpha(visualY.value)),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
