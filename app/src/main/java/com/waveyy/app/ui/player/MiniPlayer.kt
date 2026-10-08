package com.waveyy.app.ui.player

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
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

    val offsetX = remember(currentSong.id) { Animatable(0f) }
    val offsetY = remember(currentSong.id) { Animatable(0f) }
    val thresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    var hapticFired by remember(currentSong.id) { mutableStateOf(false) }
    val accent = rememberAlbumAccent(currentSong.imageUrl, currentSong.id)

    fun snapBack() {
        scope.launch {
            offsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f))
            offsetY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 400f))
        }
    }

    val dragAlpha = { value: Float -> (abs(value) / thresholdPx).coerceIn(0f, 1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
            }
            .pointerInput(currentSong.id) {
                detectDragGestures(
                    onDragStart = { hapticFired = false },
                    onDragEnd = {
                        when {
                            offsetX.value > thresholdPx -> {
                                scope.launch { connection.previous() }
                                snapBack()
                            }
                            offsetX.value < -thresholdPx -> {
                                scope.launch { connection.next() }
                                snapBack()
                            }
                            offsetY.value > thresholdPx -> playerVm.hideMiniPlayer()
                            offsetY.value < -thresholdPx -> onExpand()
                            else -> snapBack()
                        }
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + drag.x)
                            offsetY.snapTo(offsetY.value + drag.y)
                        }
                        val crossed = abs(offsetX.value) > thresholdPx ||
                            abs(offsetY.value) > thresholdPx
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
        if (offsetX.value < -8f) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = dragAlpha(offsetX.value)),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)
            )
        }
        if (offsetX.value > 8f) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = dragAlpha(offsetX.value)),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp)
            )
        }
        if (offsetY.value < -8f) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = dragAlpha(offsetY.value)),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        if (offsetY.value > 8f) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = dragAlpha(offsetY.value)),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
