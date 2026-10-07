package com.waveyy.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.remote.dto.SongDto
import com.waveyy.app.data.remote.mapper.SongMapper
import com.waveyy.app.data.repository.SongRepository
import com.waveyy.app.domain.model.Album
import com.waveyy.app.domain.model.Artist
import com.waveyy.app.domain.model.Song
import com.waveyy.app.domain.usecase.SearchRanking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Top artist match shown above results. */
data class TopArtist(val artist: Artist, val score: Double)

/** Search screen state. */
sealed interface SearchUiState {
    /** Loading results. */
    data object Loading : SearchUiState

    /** Loaded songs, albums, artists, and optional top artist. */
    data class Success(
        val songs: List<Song>,
        val albums: List<Album>,
        val artists: List<Artist>,
        val topArtist: TopArtist?
    ) : SearchUiState

    /** Load failed with [message]. */
    data class Error(val message: String) : SearchUiState
}

/**
 * Search pipeline ported from `Search.jsx`: query variants fan out to song
 * (first 3) and album (first 2) searches, songs dedupe by id or
 * name-plus-first-artist, artists mine from song attributions, and the top
 * artist shows when its score reaches 50.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SongRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val query: String = savedStateHandle.get<String>("query").orEmpty()

    private val mutableState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    /** Screen state. */
    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()

    init {
        if (query.isBlank()) {
            mutableState.value = SearchUiState.Success(emptyList(), emptyList(), emptyList(), null)
        } else {
            search()
        }
    }

    private fun search() {
        mutableState.value = SearchUiState.Loading
        viewModelScope.launch {
            runCatching { load(query) }
                .onSuccess { mutableState.value = it }
                .onFailure { mutableState.value = SearchUiState.Error(it.message ?: "Search failed") }
        }
    }

    private suspend fun load(rawQuery: String): SearchUiState.Success {
        val variants = SearchRanking.queryVariants(rawQuery)
        val songDtos = variants.take(3).flatMap { variant ->
            runCatching { repository.searchSongDtos(variant).first() }.getOrDefault(emptyList())
        }.distinctBy { it.id.ifBlank { null } ?: (it.name to firstArtistName(it)) }
        val songs = SongMapper.toDomainList(songDtos)
        val albums = variants.take(2).flatMap { variant ->
            runCatching { repository.searchAlbums(variant).first() }.getOrDefault(emptyList())
        }.distinctBy { it.id }
        val artists = uniqueArtists(songDtos)
        val top = artists
            .map { artist -> TopArtist(artist, SearchRanking.artistScore(artist.name, rawQuery)) }
            .maxByOrNull { it.score }
            ?.takeIf { it.score >= TOP_ARTIST_THRESHOLD }
        return SearchUiState.Success(songs, albums, artists, top)
    }

    private fun firstArtistName(dto: SongDto): String {
        return dto.artists?.primary?.firstOrNull()?.name.orEmpty()
    }

    private fun uniqueArtists(dtos: List<SongDto>): List<Artist> {
        val seen = mutableSetOf<String>()
        return dtos.mapNotNull { dto ->
            val ref = dto.artists?.primary?.firstOrNull() ?: return@mapNotNull null
            val id = ref.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (!seen.add(id)) return@mapNotNull null
            Artist(
                id = id,
                name = ref.name.ifBlank { "unknown" },
                imageUrl = SongMapper.imageOf(ref.image),
                followerCount = 0L,
                topSongs = emptyList(),
                topAlbums = emptyList()
            )
        }
    }

    companion object {
        private const val TOP_ARTIST_THRESHOLD = 50.0
    }
}
