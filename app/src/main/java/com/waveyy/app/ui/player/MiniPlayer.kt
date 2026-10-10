package com.waveyy.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.PlaybackConnection
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.library.LikeSongButton
import com.waveyy.app.ui.library.sharedMusicViewModel
import com.waveyy.app.ui.theme.rememberAlbumAccent
import com.waveyy.app.util.rememberWaveyyHaptics
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs

/**
 * Floating pill mini-player: rounded thumb, title/artist, like + play
 * controls. Sits 8.dp above the bottom bar with 12.dp side margins.
 * Tap opens the full player. Axis-locked swipe:
 * left = next, right = previous, down = pause + hide. Upward drags do
 * nothing. The heart/play controls stay fixed on screen while the pill
 * slides.
 * Ports `components/player/mini-player.jsx`.
 *
 * @param connection playback connection.
 * @param currentSong currently loaded song metadata, if any.
 * @param onExpand expand-to-full-player handler.
 * @param modifier layout modifier.
 * @param playerVm shared player logic (hidden flag).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    connection: PlaybackConnection,
    currentSong: Song?,
    onExpand: () -> Unit,
    onOpenLyrics: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    playerVm: PlayerViewModel = sharedPlayerViewModel()
) {
    val state by connection.state.collectAsState()
    val hidden by playerVm.miniPlayerHidden.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val waveyyHaptics = rememberWaveyyHaptics()
    val musicVm = sharedMusicViewModel()
    if (state.mediaId == null || currentSong == null || hidden) return
    LaunchedEffect(currentSong.id) { playerVm.clearMiniPlayerHidden() }

    // Raw drag amounts — instant during drag, spring back on release.
    var rawDragX by remember(currentSong.id) { mutableFloatStateOf(0f) }
    var rawDragY by remember(currentSong.id) { mutableFloatStateOf(0f) }
    var isDragging by remember(currentSong.id) { mutableStateOf(false) }
    var dragAxis by remember(currentSong.id) { mutableStateOf<DragAxis?>(null) }
    val thresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    val maxVisualPx = with(LocalDensity.current) { 150.dp.toPx() }

    // Release animation: snap back to center.
    val settledX = remember(currentSong.id) { Animatable(0f) }
    val settledY = remember(currentSong.id) { Animatable(0f) }

    // Rendered translation: raw while dragging, animated otherwise.
    val visualX: Float = if (isDragging) {
        rawDragX.coerceIn(-maxVisualPx, maxVisualPx)
    } else {
        settledX.value
    }
    val visualY: Float = if (isDragging) {
        // No upward motion: clamp to [0, maxVisualPx].
        rawDragY.coerceIn(0f, maxVisualPx)
    } else {
        settledY.value
    }

    // Visible translation comes from visualX/visualY below (raw while
    // dragging, settled spring value otherwise).
    var hapticFired by remember(currentSong.id) { mutableStateOf(false) }
    val accent = rememberAlbumAccent(currentSong.imageUrl, currentSong.id)
    var showQuickSheet by remember { mutableStateOf(false) }
    var quickSheetShowQueue by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val dragAlpha = { value: Float -> (abs(value) / thresholdPx).coerceIn(0f, 1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .pointerInput(currentSong.id) {
                detectDragGestures(
                    onDragStart = {
                        hapticFired = false
                        dragAxis = null
                        rawDragX = settledX.value
                        rawDragY = settledY.value
                        isDragging = true
                    },
                    onDragEnd = {
                        Timber.tag("MiniPlayer").d(
                            "drag end: axis=$dragAxis x=$rawDragX y=$rawDragY"
                        )
                        when (dragAxis) {
                            DragAxis.Horizontal -> when {
                                rawDragX > thresholdPx -> scope.launch { connection.previous() }
                                rawDragX < -thresholdPx -> scope.launch { connection.next() }
                            }
                            DragAxis.Vertical -> {
                                if (rawDragY > thresholdPx) playerVm.stopAndHide()
                            }
                            null -> Unit
                        }
                        // Spring back to center.
                        val releaseX = rawDragX.coerceIn(-maxVisualPx, maxVisualPx)
                        val releaseY = rawDragY.coerceIn(0f, maxVisualPx)
                        scope.launch {
                            settledX.snapTo(releaseX)
                            settledY.snapTo(releaseY)
                            isDragging = false
                            launch {
                                settledX.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            launch {
                                settledY.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                        dragAxis = null
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        // Lock to one axis on the first significant movement
                        // so a mostly-horizontal drag never picks up Y motion
                        // (and vice-versa). 3px dead zone avoids noise.
                        if (dragAxis == null) {
                            val ax = abs(drag.x)
                            val ay = abs(drag.y)
                            if (ax > 3f || ay > 3f) {
                                dragAxis = if (ax >= ay) DragAxis.Horizontal else DragAxis.Vertical
                            }
                        }
                        when (dragAxis) {
                            DragAxis.Horizontal -> rawDragX += drag.x
                            // Down-only: coerce to [0, maxVisualPx] so upward
                            // drags don't move the pill at all.
                            DragAxis.Vertical -> {
                                rawDragY = (rawDragY + drag.y).coerceIn(0f, maxVisualPx)
                            }
                            null -> Unit
                        }
                        val crossed = abs(rawDragX) > thresholdPx || rawDragY > thresholdPx
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
                .background(Color.Black.copy(alpha = 0.5f))
                .graphicsLayer {
                    translationX = visualX
                    translationY = visualY
                    // Alpha and scale driven by the *visual* offsets so they recover
                    // during spring-back. Reading rawDragX/rawDragY here would keep
                    // the pill faded after release, because those are not reset.
                    val horizontalAlpha = (1f - abs(visualX) / (thresholdPx * 1.4f))
                        .coerceIn(0.35f, 1f)
                    val verticalAlpha = if (visualY > 0f) {
                        (1f - visualY / (thresholdPx * 1.2f)).coerceIn(0.2f, 1f)
                    } else {
                        1f
                    }
                    alpha = minOf(horizontalAlpha, verticalAlpha)
                    // Scale: 1.0 at rest, up to 1.03 mid-drag for a subtle "lift".
                    val dragMagnitude = maxOf(abs(visualX), abs(visualY))
                    val s = (1f + (dragMagnitude / thresholdPx) * 0.03f).coerceIn(1f, 1.03f)
                    scaleX = s
                    scaleY = s
                }
                .combinedClickable(
                    onLongClick = {
                        waveyyHaptics.tick()
                        quickSheetShowQueue = false
                        showQuickSheet = true
                    },
                    onClick = {
                        waveyyHaptics.tick()
                        onExpand()
                    }
                )
                .height(64.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(currentSong.id) {
                        detectTapGestures(
                            onTap = {
                                // Single tap on the art expands. The parent
                                // Row's onClick would not fire because
                                // detectTapGestures consumes the event.
                                waveyyHaptics.tick()
                                onExpand()
                            },
                            onDoubleTap = {
                                waveyyHaptics.tick()
                                musicVm.toggleLike(currentSong)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val fraction = if (state.durationMs > 0L) {
                    (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val strokeWidthPx = with(LocalDensity.current) { 2.dp.toPx() }
                val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val inset = strokeWidthPx / 2f
                    val arcSize = Size(
                        width = size.width - strokeWidthPx,
                        height = size.height - strokeWidthPx
                    )
                    // Dim track: full circle.
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                    // Progress arc in accent, fills clockwise from 12 o'clock.
                    if (fraction > 0f) {
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 360f * fraction,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                        )
                    }
                }
                AdaptiveImage(
                    imageUrl = currentSong.imageUrl,
                    contentDescription = currentSong.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = currentSong.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
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
            // Counter-translate the buttons so they stay anchored on screen
            // while the pill slides. The outer Row's graphicsLayer moves
            // everything by (+visualX, +visualY); the -visualX/-visualY here
            // cancels that out for the controls only.
            LikeSongButton(
                song = currentSong,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        translationX = -visualX
                        translationY = -visualY
                    }
            )
            Spacer(Modifier.size(12.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        translationX = -visualX
                        translationY = -visualY
                    }
                    .clip(CircleShape)
                    .background(accent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        waveyyHaptics.tick()
                        scope.launch { connection.togglePlayPause() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        // Directional hints that fade in while dragging.
        if (visualX < -2f) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = dragAlpha(visualX)),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp)
            )
        }
        if (visualX > 2f) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = dragAlpha(visualX)),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp)
            )
        }
        if (visualY > 2f) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = dragAlpha(visualY)),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    if (showQuickSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showQuickSheet = false
                quickSheetShowQueue = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            if (quickSheetShowQueue) {
                QueueSheet(
                    queue = playerVm.queue.collectAsState().value,
                    currentId = state.mediaId,
                    onMove = playerVm::moveQueueItem,
                    onRemove = playerVm::removeQueueItem,
                    onClear = playerVm::clearQueue,
                    onPlay = { index ->
                        scope.launch {
                            connection.playSongs(playerVm.queue.value, index)
                        }
                        showQuickSheet = false
                        quickSheetShowQueue = false
                    }
                )
            } else {
                QuickActionsMenu(
                    onLyrics = {
                        showQuickSheet = false
                        currentSong?.let { onOpenLyrics(it.id) }
                    },
                    onQueue = { quickSheetShowQueue = true },
                    onSleepTimer = {
                        showQuickSheet = false
                        showSleepDialog = true
                    },
                    onPlaybackSpeed = {
                        showQuickSheet = false
                        showSpeedDialog = true
                    }
                )
            }
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepDialog = false },
            onSelect = { minutes ->
                showSleepDialog = false
                if (minutes == null) {
                    playerVm.setSleepEndOfSong()
                } else {
                    playerVm.setSleepTimer(minutes)
                }
            }
        )
    }

    if (showSpeedDialog) {
        SpeedDialog(
            initial = 1f,
            onDismiss = { showSpeedDialog = false },
            onSelect = { speed ->
                showSpeedDialog = false
                playerVm.setPlaybackSpeed(speed)
            }
        )
    }
}

/** Axis chosen at drag start; locked for the rest of the gesture. */
private enum class DragAxis { Horizontal, Vertical }

/**
 * Quick-actions menu shown when the mini player pill is long-pressed:
 * four rows that route to lyrics, the queue, sleep timer, or speed.
 */
@Composable
private fun QuickActionsMenu(
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onSleepTimer: () -> Unit,
    onPlaybackSpeed: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        QuickActionRow(
            icon = Icons.Filled.Lyrics,
            label = "Lyrics",
            onClick = onLyrics
        )
        QuickActionRow(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            label = "Queue",
            onClick = onQueue
        )
        QuickActionRow(
            icon = Icons.Filled.Schedule,
            label = "Sleep timer",
            onClick = onSleepTimer
        )
        QuickActionRow(
            icon = Icons.Filled.Speed,
            label = "Playback speed",
            onClick = onPlaybackSpeed
        )
    }
}

/** One row inside [QuickActionsMenu]. */
@Composable
private fun QuickActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}
