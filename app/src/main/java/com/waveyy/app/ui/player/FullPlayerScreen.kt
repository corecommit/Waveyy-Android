package com.waveyy.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.theme.rememberAlbumAccent
import kotlinx.coroutines.launch

/**
 * Full-screen Now Playing: header row, square artwork, title/artist,
 * accent slider, transport pill, secondary actions. Everything fits on
 * one screen (no scrolling, no swipe gestures). Ports the player route;
 * queue and lyrics open as bottom sheets.
 *
 * @param viewModel player logic (shared instance).
 * @param onClose minimize/back handler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    viewModel: PlayerViewModel = sharedPlayerViewModel(),
    onClose: () -> Unit = {}
) {
    val connection = viewModel.connection
    val state by connection.state.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val scope = rememberCoroutineScope()
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    val currentSong = queue.firstOrNull { it.id == state.mediaId }
    val accent = rememberAlbumAccent(currentSong?.imageUrl, currentSong?.id)
    val fraction = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Minimize")
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = accent.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "NOW PLAYING",
                    color = accent,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showOverflow = true }) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
            }
        }

        Spacer(Modifier.height(24.dp))

        AdaptiveImage(
            imageUrl = currentSong?.imageUrl.orEmpty(),
            contentDescription = currentSong?.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = currentSong?.name ?: "Nothing playing",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = currentSong?.artist ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(24.dp))

        Slider(
            value = fraction,
            onValueChange = { scope.launch { connection.seekTo((it * state.durationMs).toLong()) } },
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatDuration(state.positionMs / 1000L),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(state.durationMs / 1000L),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { scope.launch { connection.previous() } },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }

            Spacer(Modifier.width(24.dp))

            Button(
                onClick = { scope.launch { connection.togglePlayPause() } },
                modifier = Modifier.width(160.dp).height(72.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(24.dp))

            FilledIconButton(
                onClick = { scope.launch { connection.next() } },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { showLyrics = true }) {
                Icon(imageVector = Icons.Filled.Info, contentDescription = "Lyrics")
            }
            IconButton(onClick = { scope.launch { connection.toggleShuffle() } }) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffleEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { scope.launch { connection.cycleRepeatMode() } }) {
                Icon(
                    imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Filled.RepeatOne
                    } else {
                        Icons.Filled.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        accent
                    }
                )
            }
            IconButton(onClick = { currentSong?.let { viewModel.appendSong(it) } }) {
                Icon(imageVector = Icons.Filled.PlaylistAdd, contentDescription = "Add to queue")
            }
            IconButton(onClick = { showLyrics = true }) {
                Icon(imageVector = Icons.Filled.Lyrics, contentDescription = "Lyrics")
            }
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
                onPlay = { index ->
                    scope.launch { connection.playSongs(queue, index) }
                    showQueue = false
                }
            )
        }
    }
    if (showLyrics) {
        ModalBottomSheet(
            onDismissRequest = { showLyrics = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            LyricsSheet(viewModel = viewModel, connection = connection, song = currentSong)
        }
    }
    DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(imageVector = Icons.Filled.Share, contentDescription = null) },
            onClick = {
                showOverflow = false
                currentSong?.let(viewModel::shareSong)
            }
        )
        DropdownMenuItem(
            text = { Text("Download") },
            leadingIcon = { Icon(imageVector = Icons.Filled.Download, contentDescription = null) },
            onClick = {
                showOverflow = false
                currentSong?.let { scope.launch { viewModel.downloadSong(it) } }
            }
        )
        DropdownMenuItem(
            text = { Text("Queue") },
            leadingIcon = { Icon(imageVector = Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
            onClick = {
                showOverflow = false
                showQueue = true
            }
        )
    }
}

/** Formats seconds as m:ss. */
private fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0:00"
    val minutes = totalSeconds / 60L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
