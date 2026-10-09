package com.waveyy.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.repository.PlaylistRepository
import com.waveyy.app.domain.model.Playlist
import com.waveyy.app.ui.components.SongRow
import com.waveyy.app.ui.components.SkeletonRows
import com.waveyy.app.ui.player.rememberChromeBottomReserve
import com.waveyy.app.ui.player.sharedPlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Detail screen state. */
sealed interface PlaylistDetailUiState {
    /** Loading. */
    data object Loading : PlaylistDetailUiState

    /** Loaded playlist. */
    data class Success(val playlist: Playlist) : PlaylistDetailUiState

    /** Not found or load failed. */
    data object Missing : PlaylistDetailUiState
}

/** Loads one playlist by id for the detail screen. */
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlists: PlaylistRepository,
    private val auth: com.waveyy.app.data.repository.AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = savedStateHandle.get<String>("id").orEmpty()

    private val mutableState =
        MutableStateFlow<PlaylistDetailUiState>(PlaylistDetailUiState.Loading)
    /** Screen state. */
    val state: StateFlow<PlaylistDetailUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    /** Reloads the playlist. */
    fun refresh() {
        val userId = auth.currentUser()?.id
        viewModelScope.launch {
            playlists.getPlaylists(userId)
                .onSuccess { loaded ->
                    mutableState.value = loaded.firstOrNull { it.id == playlistId }
                        ?.let(PlaylistDetailUiState::Success)
                        ?: PlaylistDetailUiState.Missing
                }
                .onFailure { mutableState.value = PlaylistDetailUiState.Missing }
        }
    }

    /** Removes a song, then reloads. */
    fun removeSong(songId: String) {
        val userId = auth.currentUser()?.id
        viewModelScope.launch {
            playlists.removeSong(userId, playlistId, songId)
                .onSuccess { refresh() }
        }
    }
}

/**
 * Playlist detail: title, song count, tap-to-play-all, per-song remove.
 */
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val player = sharedPlayerViewModel()
    when (val current = state) {
        PlaylistDetailUiState.Loading -> {
            SkeletonRows(count = 8)
        }
        PlaylistDetailUiState.Missing -> {
            Text(
                text = "Playlist not found",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(24.dp)
            )
        }
        is PlaylistDetailUiState.Success -> {
            val playlist = current.playlist
            val chromeBottom = rememberChromeBottomReserve()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = chromeBottom),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                items(playlist.songs, key = { it.id }) { song ->
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        SongRow(
                            title = song.name,
                            artist = song.artist,
                            imageUrl = song.imageUrl,
                            songId = song.id,
                            onPlay = {
                                val index = playlist.songs.indexOfFirst { it.id == song.id }
                                player.playSource(com.waveyy.app.playback.QueueSource.Playlist(playlist.songs), index.coerceAtLeast(0))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.IconButton(
                            onClick = { viewModel.removeSong(song.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Remove"
                            )
                        }
                    }
                }
            }
        }
    }
}
