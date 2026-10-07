package com.waveyy.app.ui.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.repository.SongRepository
import com.waveyy.app.domain.model.Album
import com.waveyy.app.domain.model.Artist
import com.waveyy.app.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Curated artist page content. */
data class ArtistPage(
    val artist: Artist,
    val podium: List<Song>,
    val tracks: List<Song>,
    val latestRelease: Song?,
    val albums: List<Album>
)

/** Artist screen state. */
sealed interface ArtistUiState {
    /** Loading the artist. */
    data object Loading : ArtistUiState

    /** Loaded page. */
    data class Success(val page: ArtistPage) : ArtistUiState

    /** Load failed with [message]. */
    data class Error(val message: String) : ArtistUiState
}

/**
 * Artist page pipeline ported from `artist/[id]/page.js`: dedupe top songs
 * by id, split podium (first 3) from tracks, pick the latest release by
 * year/release date, and surface top albums.
 */
@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repository: SongRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = savedStateHandle.get<String>("id").orEmpty()

    private val mutableState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    /** Screen state. */
    val state: StateFlow<ArtistUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.artistById(artistId).first() }
                .onSuccess { artist ->
                    mutableState.value = artist?.let { ArtistUiState.Success(curate(it)) }
                        ?: ArtistUiState.Error("Artist not found")
                }
                .onFailure { mutableState.value = ArtistUiState.Error(it.message ?: "Artist failed") }
        }
    }

    private fun curate(artist: Artist): ArtistPage {
        val unique = artist.topSongs.distinctBy { it.id }
        val podium = unique.take(PODIUM_SIZE)
        val latest = unique.maxByOrNull { it.year }
        return ArtistPage(
            artist = artist,
            podium = podium,
            tracks = unique.drop(PODIUM_SIZE),
            latestRelease = latest,
            albums = artist.topAlbums
        )
    }

    companion object {
        private const val PODIUM_SIZE = 3
    }
}
