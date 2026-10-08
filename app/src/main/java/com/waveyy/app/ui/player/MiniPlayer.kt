package com.waveyy.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.PlaybackConnection
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.library.LikeSongButton
import com.waveyy.app.ui.theme.rememberAlbumAccent
import kotlinx.coroutines.launch

/**
 * Floating pill mini-player: rounded thumb, title/artist, like + play
 * controls. Sits 8.dp above the bottom bar with 12.dp side margins.
 * Tap (outside buttons) opens the full player; swipe left skips.
 * Ports `components/player/mini-player.jsx`.
 *
 * @param connection playback connection.
 * @param currentSong currently loaded song metadata, if any.
 * @param onExpand expand-to-full-player handler.
 * @param modifier layout modifier.
 */
@Composable
fun MiniPlayer(
    connection: PlaybackConnection,
    currentSong: Song?,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by connection.state.collectAsState()
    val scope = rememberCoroutineScope()
    if (state.mediaId == null || currentSong == null) return
    val accent = rememberAlbumAccent(currentSong.imageUrl, currentSong.id)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .background(accent.copy(alpha = 0.08f))
            .clickable(onClick = onExpand)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -100f) {
                        scope.launch { connection.next() }
                    }
                }
            }
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
}
