package com.waveyy.app.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.waveyy.app.domain.model.Song

/**
 * Heart toggle backed by the reserved Liked Songs list. Ports
 * `components/playlists/like-song-button.jsx`. Guests see the outline
 * heart; taps no-op without a session.
 *
 * @param song song to like/unlike.
 * @param modifier layout modifier.
 * @param viewModel library logic (destination-scoped).
 */
@Composable
fun LikeSongButton(
    song: Song,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val liked = viewModel.isSongLiked(song.id)
    IconButton(
        onClick = { if (state.userId != null) viewModel.toggleLikedSong(song) },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (liked) "Unlike" else "Like"
        )
    }
}
