package com.waveyy.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.PlaylistRepository
import com.waveyy.app.domain.model.Playlist
import com.waveyy.app.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Library screen state. */
data class LibraryState(
    val playlists: List<Playlist> = emptyList(),
    val likedPlaylistId: String? = null,
    val backendMissing: Boolean = false,
    val loading: Boolean = true,
    val userId: String? = null
)

/**
 * Library logic: playlist CRUD, liked-songs membership, and backend-missing
 * surfacing (PGRST205 → "run supabase/playlists.sql" card). Mirrors the
 * playlist half of `music-provider.jsx`.
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val playlists: PlaylistRepository,
    private val auth: AuthRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow(LibraryState())
    /** Screen state. */
    val state: StateFlow<LibraryState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    /** Reloads playlists for the current user (empty for guests). */
    fun refresh() {
        val userId = auth.currentUser()?.id
        if (userId.isNullOrBlank()) {
            mutableState.value = LibraryState(loading = false)
            return
        }
        mutableState.value = mutableState.value.copy(loading = true, userId = userId)
        viewModelScope.launch {
            playlists.getPlaylists(userId)
                .onSuccess { loaded ->
                    mutableState.value = LibraryState(
                        playlists = loaded,
                        likedPlaylistId = PlaylistRepository.likedPlaylistId(userId),
                        loading = false,
                        userId = userId
                    )
                }
                .onFailure { error ->
                    mutableState.value = mutableState.value.copy(
                        loading = false,
                        backendMissing = playlists.isBackendMissing(error),
                        userId = userId
                    )
                }
        }
    }

    /** Creates a playlist, then reloads. */
    fun createPlaylist(name: String) {
        val userId = mutableState.value.userId ?: return
        viewModelScope.launch {
            playlists.createPlaylist(userId, name)
                .onSuccess { refresh() }
        }
    }

    /** Deletes a playlist, then reloads. */
    fun deletePlaylist(playlistId: String) {
        val userId = mutableState.value.userId ?: return
        viewModelScope.launch {
            playlists.deletePlaylist(userId, playlistId)
                .onSuccess { refresh() }
        }
    }

    /** Adds a song, then reloads. */
    fun addSong(playlistId: String, song: Song) {
        val userId = mutableState.value.userId ?: return
        viewModelScope.launch {
            playlists.addSong(userId, playlistId, song)
                .onSuccess { refresh() }
        }
    }

    /** Removes a song, then reloads. */
    fun removeSong(playlistId: String, songId: String) {
        val userId = mutableState.value.userId ?: return
        viewModelScope.launch {
            playlists.removeSong(userId, playlistId, songId)
                .onSuccess { refresh() }
        }
    }

    /** True when [songId] sits in the Liked list. */
    fun isSongLiked(songId: String): Boolean {
        val state = mutableState.value
        val likedId = state.likedPlaylistId ?: return false
        return state.playlists.firstOrNull { it.id == likedId }
            ?.songs.orEmpty().any { it.id == songId }
    }

    /** Toggles a song in the Liked list (ensuring it exists first). */
    fun toggleLikedSong(song: Song) {
        val userId = mutableState.value.userId ?: return
        viewModelScope.launch {
            val liked = playlists.ensureLikedPlaylist(userId).getOrNull() ?: return@launch
            if (isSongLiked(song.id)) {
                playlists.removeSong(userId, liked.id, song.id)
            } else {
                playlists.addSong(userId, liked.id, song)
            }.onSuccess { refresh() }
        }
    }
}
