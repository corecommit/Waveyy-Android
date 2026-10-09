package com.waveyy.app.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waveyy.app.domain.model.Song

/**
 * Heart toggle backed by the shared [MusicViewModel.likedSongIds] set, so
 * the mini-player and full-player hearts always agree. Guests persist to
 * DataStore; logged-in users also sync the backend Liked Songs list.
 * Ports `components/playlists/like-song-button.jsx`.
 *
 * @param song song to like/unlike.
 * @param modifier layout modifier.
 * @param viewModel shared likes logic (activity-scoped).
 */
@Composable
fun LikeSongButton(
    song: Song,
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = sharedMusicViewModel()
) {
    val liked by viewModel.likedSongIds.collectAsStateWithLifecycle()
    val isLiked = song.id in liked
    IconButton(
        onClick = { viewModel.toggleLike(song) },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isLiked) "Unlike" else "Like",
            tint = if (isLiked) Color(0xFFE53E3E)
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
