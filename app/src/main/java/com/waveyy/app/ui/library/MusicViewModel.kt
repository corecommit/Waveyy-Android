package com.waveyy.app.ui.library

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.PlaylistRepository
import com.waveyy.app.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single source of truth for liked songs, backed by PlaylistRepository's
 * reserved Liked Songs playlist (Supabase for signed-in users, guest
 * DataStore playlists otherwise). Both the mini-player/full-player hearts
 * and the Library's Liked Songs screen read the same data, so they can
 * never disagree.
 *
 * Previously guest likes were tracked as a flat id set in
 * `guestLikedIdsFlow`, which stored no metadata and could not be rendered
 * as a song list — that dual-write was the root of the "liked songs
 * disappear" bug.
 */
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val playlists: PlaylistRepository,
    private val auth: AuthRepository
) : ViewModel() {

    private val mutableLikedIds = MutableStateFlow<Set<String>>(emptySet())
    /** Currently-liked song ids (matches the Liked Songs playlist). */
    val likedSongIds: StateFlow<Set<String>> = mutableLikedIds.asStateFlow()

    init {
        // React to sign-in / sign-out so the guest set swaps cleanly.
        viewModelScope.launch {
            auth.sessionFlow.collect { refreshLikes() }
        }
    }

    /**
     * Toggles [song] in the Liked Songs playlist. Optimistic update: the
     * heart flips immediately; on backend failure we roll back.
     */
    fun toggleLike(song: Song) {
        viewModelScope.launch {
            val userId = auth.currentUser()?.id
            val current = mutableLikedIds.value
            val wasLiked = song.id in current
            val liked = playlists.ensureLikedPlaylist(userId).getOrNull() ?: return@launch
            val result = if (wasLiked) {
                playlists.removeSong(userId, liked.id, song.id)
            } else {
                playlists.addSong(userId, liked.id, song)
            }
            result
                .onSuccess {
                    mutableLikedIds.value = if (wasLiked) {
                        current - song.id
                    } else {
                        current + song.id
                    }
                }
                .onFailure {
                    // Roll back optimistic flip on failure.
                    mutableLikedIds.value = current
                }
        }
    }

    /** Reloads the liked id set from the repository. */
    fun refreshLikes() {
        viewModelScope.launch {
            val userId = auth.currentUser()?.id
            val likedId = if (userId.isNullOrBlank()) {
                "local_liked"
            } else {
                PlaylistRepository.likedPlaylistId(userId)
            }
            val liked = playlists.getPlaylists(userId).getOrNull()
                ?.firstOrNull { it.id == likedId }
            mutableLikedIds.value = liked?.songs.orEmpty().map { it.id }.toSet()
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
    val activity = LocalActivity.current as? ComponentActivity
        ?: error("MusicViewModel requires a ComponentActivity host")
    return hiltViewModel(activity)
}
