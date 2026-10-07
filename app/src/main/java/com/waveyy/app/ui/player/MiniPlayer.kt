package com.waveyy.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.PlaybackConnection
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.theme.WaveyyShapes
import kotlinx.coroutines.launch

/**
 * Slim persistent bar: artwork, title, play/pause. Ports
 * `components/player/mini-player.jsx`. Tap expands to the full player.
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
    if (state.mediaId == null) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveImage(
            imageUrl = currentSong?.imageUrl.orEmpty(),
            contentDescription = currentSong?.name,
            modifier = Modifier
                .size(44.dp)
                .clip(WaveyyShapes.small)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = currentSong?.name ?: "",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentSong?.artist ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { scope.launch { connection.togglePlayPause() } }) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play"
            )
        }
    }
}
