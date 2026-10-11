package com.waveyy.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waveyy.app.domain.model.Song
import com.waveyy.app.ui.components.AdaptiveImage

/** Song actions available from the info sheet. */
enum class SongAction {
    LIKE,
    DOWNLOAD,
    ADD_TO_PLAYLIST,
    PLAY_NEXT,
    ADD_TO_QUEUE,
    ARTISTS,
    SONG_RADIO,
    START_RADIO,
    LYRICS_PROVIDER,
    SLEEP_TIMER,
    SPEED_PITCH,
    SHARE
}

/**
 * Song info bottom sheet: header (art, title, artist), divider, then the
 * 12 action rows. Each row fires [onAction] and dismisses. Rows whose
 * feature is unavailable still render (caller toasts "Coming soon").
 *
 * @param song subject song.
 * @param isLiked whether the song is in Liked Songs.
 * @param onDismiss dismiss handler.
 * @param onAction action handler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoSheet(
    song: Song,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onAction: (SongAction) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        LazyColumn {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdaptiveImage(
                        imageUrl = song.imageUrl,
                        contentDescription = song.name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = song.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            item {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            items(SongAction.entries.toList()) { action ->
                InfoRow(
                    action = action,
                    isLiked = isLiked,
                    onClick = {
                        onDismiss()
                        onAction(action)
                    }
                )
            }
        }
    }
}

/** One 56.dp action row: leading icon, label, optional trailing badge. */
@Composable
private fun InfoRow(
    action: SongAction,
    isLiked: Boolean,
    onClick: () -> Unit
) {
    val (icon, label) = when (action) {
        SongAction.LIKE -> (if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder) to
            if (isLiked) "Liked" else "Like"
        SongAction.DOWNLOAD -> Icons.Filled.Download to "Download"
        SongAction.ADD_TO_PLAYLIST -> Icons.Filled.Add to "Add to a playlist"
        SongAction.PLAY_NEXT -> Icons.Filled.SkipNext to "Play next"
        SongAction.ADD_TO_QUEUE -> Icons.AutoMirrored.Filled.QueueMusic to "Add to queue"
        SongAction.ARTISTS -> Icons.Filled.Group to "Artists"
        SongAction.SONG_RADIO -> Icons.Filled.PlayArrow to "Go to song radio"
        SongAction.START_RADIO -> Icons.Filled.Radio to "Start radio"
        SongAction.LYRICS_PROVIDER -> Icons.Filled.Info to "Main Lyrics Provider"
        SongAction.SLEEP_TIMER -> Icons.Filled.Schedule to "Sleep Timer"
        SongAction.SPEED_PITCH -> Icons.Filled.Speed to "Playback speed & pitch"
        SongAction.SHARE -> Icons.Filled.Share to "Share"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 40.dp)
        )
    }
}
