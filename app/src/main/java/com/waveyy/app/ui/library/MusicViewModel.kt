package com.waveyy.app.ui.library

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.PlaylistRepository
import com.waveyy.app.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single source of truth for liked songs, shared by the mini-player heart
 * and the full-player heart. Emits the union of backend Liked Songs ids
 * (logged-in users) and the guest DataStore set (works without a session,
 * survives kill + reopen). Both hearts collect [likedSongIds] and call
 * [toggleLike], so they can never disagree.
 */
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val playlists: PlaylistRepository,
    private val auth: AuthRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val backendLiked = MutableStateFlow<Set<String>>(emptySet())

    /** Union of backend + guest liked ids. */
    val likedSongIds: StateFlow<Set<String>> = combine(
        backendLiked,
        userPrefs.guestLikedIdsFlow()
    ) { backend, guest -> backend + guest }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptySet())

    init {
        refreshBackendLikes()
    }

    /** Toggles [song]: optimistic DataStore flip plus backend sync when logged in. */
    fun toggleLike(song: Song) {
        viewModelScope.launch {
            val current = userPrefs.guestLikedIdsFlow().first()
            val next = if (song.id in current) current - song.id else current + song.id
            userPrefs.setGuestLikedIds(next)
            val userId = auth.currentUser()?.id
            if (!userId.isNullOrBlank()) {
                val liked = playlists.ensureLikedPlaylist(userId).getOrNull()
                if (liked != null) {
                    if (song.id in backendLiked.value) {
                        playlists.removeSong(userId, liked.id, song.id)
                    } else {
                        playlists.addSong(userId, liked.id, song)
                    }
                    refreshBackendLikes(userId)
                }
            }
        }
    }

    /** Reloads backend liked ids (no-op state reset for guests). */
    fun refreshBackendLikes() {
        val userId = auth.currentUser()?.id
        if (userId.isNullOrBlank()) {
            backendLiked.value = emptySet()
            return
        }
        refreshBackendLikes(userId)
    }

    private fun refreshBackendLikes(userId: String) {
        viewModelScope.launch {
            val ids = playlists.getPlaylists(userId).getOrNull()
                ?.firstOrNull { it.id == PlaylistRepository.likedPlaylistId(userId) }
                ?.songs.orEmpty().map { it.id }.toSet()
            backendLiked.value = ids
        }
    }
}

/**
 * Activity-scoped [MusicViewModel] shared by mini-player and full player.
 * Destination-level `hiltViewModel()` calls are scoped to nav back-stack
 * entries (a different instance per screen), which would let the two hearts
 * disagree. Screens showing a like heart must use this helper instead.
 */
@Composable
fun sharedMusicViewModel(): MusicViewModel {
    val activity = LocalActivity.current ?: error("MusicViewModel requires an Activity host")
    return hiltViewModel(activity)
}
