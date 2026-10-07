package com.waveyy.app.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.components.WaveyySlider
import com.waveyy.app.ui.components.WaveyyTextButton
import com.waveyy.app.ui.theme.WaveyyShapes
import kotlinx.coroutines.launch

/** Full-player tabs. */
private enum class PlayerTab { NOW_PLAYING, UP_NEXT, LYRICS }

/**
 * Full-screen player with Now Playing / Up Next / Lyrics tabs, repeat and
 * shuffle, share, and MediaStore download. Ports `full-player.jsx` (the
 * largest single port). Playback itself runs in [PlayerViewModel.connection].
 *
 * @param viewModel player logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val connection = viewModel.connection
    val state by connection.state.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(PlayerTab.NOW_PLAYING.ordinal) }
    var showQueue by remember { mutableStateOf(false) }
    val currentSong = queue.firstOrNull { it.id == state.mediaId }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            PlayerTab.entries.forEachIndexed { index, entry ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(entry.name.replace("_", " ")) }
                )
            }
        }
        when (PlayerTab.entries[tab]) {
            PlayerTab.NOW_PLAYING -> NowPlayingTab(
                song = currentSong,
                statePositionMs = state.positionMs,
                stateDurationMs = state.durationMs,
                isPlaying = state.isPlaying,
                repeatMode = state.repeatMode,
                shuffleEnabled = state.shuffleEnabled,
                onToggle = { scope.launch { connection.togglePlayPause() } },
                onNext = { scope.launch { connection.next() } },
                onPrevious = { scope.launch { connection.previous() } },
                onSeek = { scope.launch { connection.seekTo(it) } },
                onRepeat = { scope.launch { connection.cycleRepeatMode() } },
                onShuffle = { scope.launch { connection.toggleShuffle() } },
                onShare = { currentSong?.let(viewModel::shareSong) },
                onDownload = { currentSong?.let { scope.launch { viewModel.downloadSong(it) } } },
                onQueue = { showQueue = true }
            )
            PlayerTab.UP_NEXT -> QueueSheet(
                queue = queue,
                currentId = state.mediaId,
                onMove = viewModel::moveQueueItem,
                onRemove = viewModel::removeQueueItem,
                onClear = viewModel::clearQueue,
                onPlay = { index ->
                    scope.launch { connection.playSongs(queue, index) }
                }
            )
            PlayerTab.LYRICS -> LyricsSheet(
                viewModel = viewModel,
                connection = connection,
                song = currentSong
            )
        }
    }
    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            QueueSheet(
                queue = queue,
                currentId = state.mediaId,
                onMove = viewModel::moveQueueItem,
                onRemove = viewModel::removeQueueItem,
                onClear = viewModel::clearQueue,
                onPlay = { showQueue = false }
            )
        }
    }
}

/** Now-playing tab: artwork, metadata, slider, transport, actions. */
@Composable
private fun NowPlayingTab(
    song: Song?,
    statePositionMs: Long,
    stateDurationMs: Long,
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onQueue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdaptiveImage(
            imageUrl = song?.imageUrl.orEmpty(),
            contentDescription = song?.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(WaveyyShapes.medium)
        )
        Text(
            text = song?.name ?: "Nothing playing",
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song?.artist ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val fraction = if (stateDurationMs > 0L) {
            (statePositionMs.toFloat() / stateDurationMs).coerceIn(0f, 1f)
        } else {
            0f
        }
        WaveyySlider(
            value = fraction,
            onValueChange = { onSeek((it * stateDurationMs).toLong()) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onShuffle) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (shuffleEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
            IconButton(onClick = onNext) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next")
            }
            IconButton(onClick = onRepeat) {
                Icon(
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onShare) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
            }
            IconButton(onClick = onDownload) {
                Icon(imageVector = Icons.Filled.Download, contentDescription = "Download")
            }
            WaveyyTextButton(text = "Queue", onClick = onQueue)
            IconButton(onClick = onQueue) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue"
                )
            }
        }
    }
}
