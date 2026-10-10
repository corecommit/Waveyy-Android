package com.waveyy.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.PlaylistRepository
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.QueueSource
import com.waveyy.app.ui.components.AdaptiveImage
import com.waveyy.app.ui.components.SkeletonRows
import com.waveyy.app.ui.components.WaveyyBackButton
import com.waveyy.app.ui.player.rememberChromeBottomReserve
import com.waveyy.app.util.rememberWaveyyHaptics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Liked Songs screen state. */
sealed interface LikedSongsUiState {
    /** Loading. */
    data object Loading : LikedSongsUiState

    /** Loaded liked songs. */
    data class Ready(val songs: List<Song>) : LikedSongsUiState
}

/** Provides the Liked Songs list and remove action. */
@HiltViewModel
class LikedSongsViewModel @Inject constructor(
    private val playlists: PlaylistRepository,
    private val auth: AuthRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow<LikedSongsUiState>(LikedSongsUiState.Loading)
    /** Screen state. */
    val state: StateFlow<LikedSongsUiState> = mutableState.asStateFlow()

    init { refresh() }

    /** Reloads from the repository. */
    fun refresh() {
        viewModelScope.launch {
            val userId = auth.currentUser()?.id
            val likedId = if (userId.isNullOrBlank()) {
                "local_liked"
            } else {
                PlaylistRepository.likedPlaylistId(userId)
            }
            val liked = playlists.getPlaylists(userId).getOrNull()
                ?.firstOrNull { it.id == likedId }
            mutableState.value = LikedSongsUiState.Ready(liked?.songs.orEmpty())
        }
    }

    /** Removes a song from the liked list, then reloads. */
    fun removeSong(songId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser()?.id
            val likedId = if (userId.isNullOrBlank()) {
                "local_liked"
            } else {
                PlaylistRepository.likedPlaylistId(userId)
            }
            playlists.removeSong(userId, likedId, songId).onSuccess { refresh() }
        }
    }
}

/**
 * Full-screen Liked Songs list with a Spotify-style header (back + title +
 * track count + play/shuffle/search) and a flat scrollable song list.
 *
 * @param viewModel screen logic.
 * @param onBack back handler.
 * @param onPlaySong queue playback handler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedSongsScreen(
    viewModel: LikedSongsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onPlaySong: (QueueSource, Int) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    val chromeBottom = rememberChromeBottomReserve()
    val haptics = rememberWaveyyHaptics()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Header row: back + title + actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WaveyyBackButton(onClick = onBack)
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = "Liked Songs",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                val count = (state as? LikedSongsUiState.Ready)?.songs?.size ?: 0
                Text(
                    text = "$count ${if (count == 1) "track" else "tracks"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val songs = (state as? LikedSongsUiState.Ready)?.songs.orEmpty()
            if (songs.isNotEmpty()) {
                IconButton(
                    onClick = {
                        haptics.tick()
                        onPlaySong(QueueSource.Playlist(songs), 0)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play all",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = {
                        haptics.tick()
                        val shuffled = songs.shuffled()
                        onPlaySong(QueueSource.Playlist(shuffled), 0)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Song list
        when (val current = state) {
            LikedSongsUiState.Loading -> SkeletonRows(count = 8)
            is LikedSongsUiState.Ready -> {
                if (current.songs.isEmpty()) {
                    EmptyLikedState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = chromeBottom),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(current.songs, key = { it.id }) { song ->
                            LikedSongRow(
                                song = song,
                                onClick = {
                                    val index = current.songs.indexOf(song)
                                    onPlaySong(QueueSource.Playlist(current.songs), index)
                                },
                                onRemove = { viewModel.removeSong(song.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Empty state when no liked songs yet. */
@Composable
private fun EmptyLikedState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No liked songs yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tap the heart on any song to save it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** One row: art + title + artist + heart (filled) as overflow trigger. */
@Composable
private fun LikedSongRow(
    song: Song,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AdaptiveImage(
            imageUrl = song.imageUrl,
            contentDescription = song.name,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Unlike",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
