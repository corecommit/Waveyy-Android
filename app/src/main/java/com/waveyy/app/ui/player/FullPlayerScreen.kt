package com.waveyy.app.ui.player

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.components.WaveyySlider
import com.waveyy.app.ui.library.AddToQueueButton
import com.waveyy.app.ui.library.LikeSongButton
import com.waveyy.app.ui.theme.rememberAlbumAccent
import com.waveyy.app.util.AudioQualitySelector
import kotlinx.coroutines.launch

/**
 * Full-screen player in Material 3 Expressive style: header row, large
 * rounded artwork, dynamic accent from the art, pill play button, and a
 * chip row for secondary actions. Ports `full-player.jsx`. Queue and
 * lyrics live in bottom sheets (opened from chips), replacing the old
 * text tabs. Playback itself runs in [PlayerViewModel.connection].
 *
 * @param viewModel player logic (shared instance).
 * @param onClose back navigation handler.
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
    val context = LocalContext.current
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    val currentSong = queue.firstOrNull { it.id == state.mediaId }
    val accent = rememberAlbumAccent(currentSong?.imageUrl, currentSong?.id)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerHeaderRow(
            onClose = onClose,
            onOverflow = { showOverflow = true },
            accent = accent,
            modifier = Modifier.fillMaxWidth()
        )
        AdaptiveImage(
            imageUrl = currentSong?.imageUrl.orEmpty(),
            contentDescription = currentSong?.name,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(28.dp))
        )
        Text(
            text = currentSong?.name ?: "Nothing playing",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = currentSong?.artist ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentSong != null) {
                AddToQueueButton(song = currentSong)
                LikeSongButton(song = currentSong)
            }
        }
        val fraction = if (state.durationMs > 0L) {
            (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }
        WaveyySlider(
            value = fraction,
            onValueChange = { scope.launch { connection.seekTo((it * state.durationMs).toLong()) } },
            activeTrackColor = accent,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(state.positionMs / 1000L),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(state.durationMs / 1000L),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            CircleControl(
                onClick = { scope.launch { connection.previous() } },
                description = "Previous"
            ) {
                Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = null)
            }
            val scale by animateFloatAsState(
                targetValue = if (state.isPlaying) 1f else 0.94f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "playScale"
            )
            Button(
                onClick = { scope.launch { connection.togglePlayPause() } },
                modifier = Modifier.size(width = 160.dp, height = 72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsScale(scale)
                )
            }
            CircleControl(
                onClick = { scope.launch { connection.next() } },
                description = "Next"
            ) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = null)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            ChipButton(Icons.Filled.Info, "Lyrics") { showLyrics = true }
            ChipButton(Icons.Filled.Headphones, "Audio") {
                val best = currentSong?.let { AudioQualitySelector.selectBest(it.streams) }
                val label = best?.let { "${it.quality.ifBlank { "stream" }} ${it.bitrateKbps}kbps" }
                    ?: "No stream"
                Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
            }
            ChipButton(
                Icons.Filled.Shuffle,
                "Shuffle",
                selected = state.shuffleEnabled,
                accent = accent
            ) {
                scope.launch { connection.toggleShuffle() }
            }
            ChipButton(
                if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                "Repeat",
                selected = state.repeatMode != Player.REPEAT_MODE_OFF,
                accent = accent
            ) {
                scope.launch { connection.cycleRepeatMode() }
            }
            ChipButton(Icons.Filled.Add, "Queue") {
                currentSong?.let { viewModel.appendSong(it) }
            }
            ChipButton(Icons.AutoMirrored.Filled.QueueMusic, "Up next") { showQueue = true }
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
    if (showOverflow) {
        ModalBottomSheet(onDismissRequest = { showOverflow = false }) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showOverflow = false
                        currentSong?.let(viewModel::shareSong)
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                    Text("Share", modifier = Modifier.padding(start = 8.dp))
                }
                androidx.compose.material3.TextButton(
                    onClick = {
                        showOverflow = false
                        currentSong?.let { scope.launch { viewModel.downloadSong(it) } }
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = null)
                    Text("Download", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** Header: back chevron, NOW PLAYING label, overflow menu trigger. */

/** Header: back chevron, NOW PLAYING label, overflow menu trigger. */
@Composable
private fun PlayerHeaderRow(
    onClose: () -> Unit,
    onOverflow: () -> Unit,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    // Accent is resolved by the caller screen and passed via composition
    // local to keep this header reusable; default falls back to primary.
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.material3.IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Close player")
        }
        NowPlayingChip(accent = accent)
        androidx.compose.material3.IconButton(onClick = onOverflow) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
        }
    }
}

/** Accent-highlighted NOW PLAYING chip. */
@Composable
private fun NowPlayingChip(accent: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.25f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelLarge,
            color = accent
        )
    }
}

/** 64.dp muted circle transport button. */
@Composable
private fun CircleControl(
    onClick: () -> Unit,
    description: String,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            content()
        }
    }
}

/** 56.dp circular action chip. */
@Composable
private fun ChipButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean = false,
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (selected) accent.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Scale modifier helper. */
private fun Modifier.graphicsScale(scale: Float): Modifier {
    return this.then(
        androidx.compose.ui.graphics.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}

/** Formats seconds as m:ss. */
private fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0:00"
    val minutes = totalSeconds / 60L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
