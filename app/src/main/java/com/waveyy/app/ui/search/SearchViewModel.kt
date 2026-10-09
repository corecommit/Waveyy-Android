package com.waveyy.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.remote.dto.SongDto
import com.waveyy.app.data.remote.mapper.SongMapper
import com.waveyy.app.data.local.datastore.SearchHistoryStore
import com.waveyy.app.data.repository.SongRepository
import com.waveyy.app.domain.model.Album
import com.waveyy.app.domain.model.Artist
import com.waveyy.app.domain.model.Song
import com.waveyy.app.domain.usecase.SearchRanking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val history: SearchHistoryStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialQuery: String = savedStateHandle.get<String>("query").orEmpty()

    private val mutableQuery = MutableStateFlow(initialQuery)
    /** Current query text. */
    val query: StateFlow<String> = mutableQuery.asStateFlow()

    private val mutableState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    /** Screen state. */
    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()

    private val mutableSuggestions = MutableStateFlow<List<String>>(emptyList())
    /**
     * Generated suggestions for the current results (query + top artist /
     * album, plus response-backed variants). Empty when the query is blank —
     * a blank query shows history or nothing, never fallbacks.
     */
    val suggestions: StateFlow<List<String>> = mutableSuggestions.asStateFlow()

    /** Search history, newest first. */
    val searchHistory: StateFlow<List<String>> = history.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    init {
        if (initialQuery.isBlank()) {
            mutableState.value = SearchUiState.Success(emptyList(), emptyList(), emptyList(), null)
        } else {
            refresh()
        }
    }

    /** Submits a new query from the in-screen field (no navigation). */
    fun submitQuery(newQuery: String) {
        mutableQuery.value = newQuery
        refresh()
    }

    /** Removes one history entry. */
    fun removeHistoryEntry(entry: String) {
        viewModelScope.launch { history.remove(entry) }
    }

    /** Clears all history. */
    fun clearHistory() {
        viewModelScope.launch { history.clear() }
    }

    /** (Re)runs the search. */
    fun refresh() {
        search()
    }

    private fun search() {
        Timber.tag("SearchVM").d("search triggered for query='${mutableQuery.value}'")
        mutableState.value = SearchUiState.Loading
        viewModelScope.launch {
            runCatching { load(mutableQuery.value) }
                .onSuccess {
                    mutableState.value = it
                    if (it.songs.isNotEmpty() && mutableQuery.value.isNotBlank()) {
                        history.add(mutableQuery.value)
                    }
                }
                .onFailure { mutableState.value = SearchUiState.Error(it.message ?: "Search failed") }
        }
    }

    private suspend fun load(rawQuery: String): SearchUiState.Success {
        val variants = SearchRanking.queryVariants(rawQuery)
        var songFailures = 0
        val songDtos = variants.take(3).flatMap { variant ->
            runCatching { repository.searchSongDtos(variant).first() }
                .onFailure { songFailures++ }
                .getOrDefault(emptyList())
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
        mutableSuggestions.value = buildSuggestions(rawQuery, songs, albums)
        if (songs.isEmpty() && albums.isEmpty() && artists.isEmpty() &&
            rawQuery.isNotBlank() && songFailures >= variants.take(3).size
        ) {
            // Backend answered with failures on every variant: surface an
            // error (with Retry) instead of masquerading as "no results".
            throw java.io.IOException("Search unavailable. Check your connection and retry.")
        }
        return SearchUiState.Success(songs, albums, artists, top)
    }

    /**
     * Builds tappable suggestions from actual results: query + top artist /
     * album (when not already contained), plus generic variants only when a
     * returned title contains them. Deduplicated case-insensitively, capped.
     */
    fun buildSuggestions(query: String, songs: List<Song>, albums: List<Album>): List<String> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        fun add(candidate: String) {
            val trimmed = candidate.trim()
            if (trimmed.isNotEmpty() && seen.add(trimmed.lowercase()) && result.size < MAX_SUGGESTIONS) {
                result += trimmed
            }
        }
        val topArtist = songs.firstOrNull()?.artist?.split(",")?.firstOrNull()?.trim().orEmpty()
        if (topArtist.isNotEmpty() && !q.contains(topArtist, ignoreCase = true)) {
            add("$q $topArtist")
        }
        val topAlbum = albums.firstOrNull()?.name?.trim().orEmpty()
        if (topAlbum.isNotEmpty() && !q.contains(topAlbum, ignoreCase = true)) {
            add("$q $topAlbum")
        }
        for (suffix in GENERIC_SUFFIXES) {
            if (songs.any { it.name.contains(suffix, ignoreCase = true) }) {
                add("$q $suffix")
            }
        }
        return result
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
        private const val MAX_SUGGESTIONS = 6
        private val GENERIC_SUFFIXES = listOf("song", "remix", "live")
    }
}
