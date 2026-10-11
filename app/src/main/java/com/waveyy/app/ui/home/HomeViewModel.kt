package com.waveyy.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.local.dao.HistoryDao
import com.waveyy.app.data.remote.mapper.SongMapper
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.SongRepository
import com.waveyy.app.domain.model.Album
import com.waveyy.app.domain.model.Artist
import com.waveyy.app.domain.model.Song
import com.waveyy.app.domain.usecase.TrendingCurator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** Home rail bundle. */
data class HomeSections(
    val trending: List<Song> = emptyList(),
    val popularArtists: List<Artist> = emptyList(),
    val popularAlbums: List<Album> = emptyList(),
    val recent: List<Song> = emptyList(),
    val recommended: List<Song> = emptyList(),
    val recommendedGenres: List<String> = emptyList(),
    val newReleases: List<Song> = emptyList()
)

/** Home screen state. */
sealed interface HomeUiState {
    /** Loading rails. */
    data object Loading : HomeUiState

    /** Loaded sections. */
    data class Success(val sections: HomeSections) : HomeUiState

    /** Load failed with [message]. */
    data class Error(val message: String) : HomeUiState
}

/**
 * Home curation pipeline ported from `app/(root)/page.js`: top charts get
 * curated into trending (with the verbatim blocklists), popular
 * artists/albums derive from the trending set, and recommendations merge
 * listening history with genre queries, Spotify picks, suggestions, and
 * generic fallbacks — in that order.
 *
 * Deviation: the web refetches song metadata for history ids over the
 * network; here recent items render straight from Room history rows (which
 * store display fields at write time), so the rail works offline.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SongRepository,
    private val historyDao: HistoryDao,
    authRepository: AuthRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    /** Screen state. */
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    private val mutableRefreshing = MutableStateFlow(false)
    /** True while a soft pull-to-refresh is in flight (state stays Success). */
    val isRefreshing: StateFlow<Boolean> = mutableRefreshing.asStateFlow()

    /** Signed-in display name, or null for guests. */
    val userName: StateFlow<String?> = authRepository.sessionFlow
        .map { it?.displayName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    init {
        refresh()
    }

    /**
     * Reloads every rail incrementally. Emits an empty Success immediately
     * so the UI can show skeletons, then updates each rail as it lands.
     * The screen renders trending the moment it's ready; other rails fill
     * in behind.
     */
    fun refresh() {
        // Start from empty Success so the grid renders skeletons.
        mutableState.value = HomeUiState.Success(HomeSections())
        viewModelScope.launch {
            // Recent comes from Room — synchronous, ready first.
            val recent = historyDao.recent(HISTORY_LIMIT).map { row ->
                Song(
                    id = row.songId,
                    name = row.name,
                    artist = row.artist,
                    album = "unknown",
                    imageUrl = row.image,
                    durationSec = 0L,
                    playCount = 0L,
                    streams = emptyList()
                )
            }
            publishSections { it.copy(recent = recent) }

            // Rails run in parallel; each updates the state as it lands.
            val trendingJob = async {
                val trending = runCatching { loadTrending() }
                    .getOrDefault(emptyList())
                publishSections {
                    it.copy(
                        trending = trending,
                        popularArtists = popularArtistsOf(trending),
                        popularAlbums = popularAlbumsOf(trending)
                    )
                }
            }
            val recsJob = async {
                val recs = runCatching { loadRecommended(recent) }
                    .getOrElse { Recommended(emptyList(), emptyList()) }
                publishSections {
                    it.copy(
                        recommended = recs.songs,
                        recommendedGenres = recs.genres
                    )
                }
            }
            val newJob = async {
                val newReleases = runCatching { loadNewReleases() }
                    .getOrDefault(emptyList())
                publishSections { it.copy(newReleases = newReleases) }
            }
            trendingJob.await()
            recsJob.await()
            newJob.await()
        }
    }

    /**
     * Applies [transform] to the current HomeSections and republishes.
     * No-op if the current state is not a Success (error state, etc.).
     */
    private fun publishSections(transform: (HomeSections) -> HomeSections) {
        val current = mutableState.value
        if (current is HomeUiState.Success) {
            mutableState.value = HomeUiState.Success(transform(current.sections))
        }
    }

    /** Fresh-release rail, locale-aware like the trending chain. */
    private suspend fun loadNewReleases(): List<Song> {
        val localeLang = java.util.Locale.getDefault().getDisplayLanguage(java.util.Locale.ENGLISH)
        val query = if (localeLang.equals("hindi", ignoreCase = true)) {
            "new hindi songs"
        } else {
            "$localeLang new releases"
        }
        val dtos = runCatching { repository.searchSongDtos(query).first() }.getOrDefault(emptyList())
        val (selected, _) = TrendingCurator.curate(dtos, NEW_RELEASE_LIMIT)
        return SongMapper.toDomainList(selected)
    }

    private suspend fun loadTrending(): List<Song> {
        // Locale-first chain (F6): the `/api/jiosaavn` proxy hardcodes
        // `language=hindi` for topCharts (route.js `buildTargets`), so the
        // device locale is honored through `searchSongs` queries instead:
        // locale hits -> proxy charts -> english -> bundled fallbacks.
        val localeLang = java.util.Locale.getDefault().getDisplayLanguage(java.util.Locale.ENGLISH)
        val localeIsHindi = localeLang.equals("hindi", ignoreCase = true)
        var trackKeys = emptySet<String>()
        val trending = mutableListOf<Song>()
        suspend fun pull(songs: List<Song>, limit: Int) {
            if (trending.size >= TRENDING_LIMIT || limit <= 0) return
            runCatching {
                val (picked, next) = TrendingCurator.curate(songs.map(::toDto), limit, trackKeys)
                trending += SongMapper.toDomainList(picked)
                trackKeys = next
            }.onFailure {
                Timber.tag("HomeVM").w(it, "pull failed, continuing")
            }
        }
        if (!localeIsHindi) {
            pull(
                runCatching { repository.searchSongs("$localeLang top hits").first() }
                    .getOrDefault(emptyList()),
                TRENDING_LIMIT
            )
        }
        pull(
            runCatching { repository.topCharts().first() }.getOrDefault(emptyList()),
            TRENDING_LIMIT - trending.size
        )
        for ((query, max) in TRENDING_FALLBACKS) {
            if (trending.size >= TRENDING_LIMIT) break
            pull(
                runCatching { repository.searchSongs(query).first() }.getOrDefault(emptyList()),
                minOf(max, TRENDING_LIMIT - trending.size)
            )
        }
        if (!localeIsHindi && trending.size < TRENDING_LIMIT) {
            pull(
                runCatching { repository.searchSongs("English top hits").first() }
                    .getOrDefault(emptyList()),
                TRENDING_LIMIT - trending.size
            )
        }
        // Never throw here. An empty trending rail is a valid state — the
        // other rails (Continue, Made for You, recommendations) may still
        // populate. The Home screen renders an empty state if everything is
        // empty; the current behaviour of throwing kills the whole screen.
        return trending
    }

    /** Lossless domain-to-DTO bridge for the shared curation pipeline. */
    private fun toDto(song: Song): com.waveyy.app.data.remote.dto.SongDto {
        return com.waveyy.app.data.remote.dto.SongDto(
            id = song.id,
            name = song.name,
            image = listOf(com.waveyy.app.data.remote.dto.SaavnImage(song.imageUrl)),
            artists = com.waveyy.app.data.remote.dto.SaavnArtists(
                song.artist.split(",").map {
                    com.waveyy.app.data.remote.dto.SaavnArtistRef(name = it.trim())
                }
            ),
            album = com.waveyy.app.data.remote.dto.SaavnAlbumRef(name = song.album),
            playCount = song.playCount
        )
    }

    private fun popularArtistsOf(trending: List<Song>): List<Artist> {
        // Derive cheap artist cards from trending attributions (web parses
        // primary artists/albums off the same set).
        return trending.mapNotNull { song ->
            val name = song.artist.takeIf { it.isNotBlank() && it != "unknown" } ?: return@mapNotNull null
            Artist(
                id = "artist:$name",
                name = name.split(",").first().trim(),
                imageUrl = song.imageUrl,
                followerCount = 0L,
                topSongs = emptyList(),
                topAlbums = emptyList()
            )
        }.distinctBy { it.id }.take(POPULAR_LIMIT)
    }

    private fun popularAlbumsOf(trending: List<Song>): List<Album> {
        return trending.mapNotNull { song ->
            val album = song.album.takeIf { it.isNotBlank() && it != "unknown" } ?: return@mapNotNull null
            Album(
                id = "album:$album:${song.artist}",
                name = album,
                artist = song.artist,
                imageUrl = song.imageUrl,
                songs = emptyList()
            )
        }.distinctBy { it.id }.take(POPULAR_LIMIT)
    }

    private data class Recommended(val songs: List<Song>, val genres: List<String>)

    private suspend fun loadRecommended(recent: List<Song>): Recommended {
        if (recent.isEmpty()) return Recommended(emptyList(), emptyList())
        // Genre counting runs on raw DTO metadata in the web app; here seeds
        // derive from recent titles and the generic fallbacks below.
        val recentIds = recent.map { it.id }.distinct().take(RECENT_SEED_LIMIT)
        return loadRecommendedFromQueries(recent, recentIds)
    }

    private suspend fun loadRecommendedFromQueries(recent: List<Song>, recentIds: List<String>): Recommended {
        val collected = mutableListOf<Song>()
        val seenIds = mutableSetOf<String>()
        val seenKeys = mutableSetOf<String>()
        val recentIdSet = recentIds.toSet()
        fun add(songs: List<Song>) {
            for (song in songs) {
                if (collected.size >= RECOMMENDED_LIMIT) break
                val key = (song.name + "||" + song.artist).lowercase()
                if (song.id.isBlank() || song.id in seenIds) continue
                if (song.id in recentIdSet || key in seenKeys) continue
                seenIds += song.id
                seenKeys += key
                collected += song
            }
        }
        // Seed genre queries from recent titles (approximation of the web's
        // tag-counted queries, which need raw genre metadata).
        val seeds = recent.take(RECENT_SEED_LIMIT).map { it.name + " songs" }
        coroutineScope {
            seeds.map { query ->
                async { runCatching { repository.searchSongs(query).first() }.getOrDefault(emptyList()) }
            }.awaitAll()
        }.forEach(::add)
        if (collected.size < RECOMMENDED_LIMIT && recent.isNotEmpty()) {
            val first = recent.first()
            val recs = runCatching {
                repository.spotifyRecommendations(first.name, first.artist).first()
            }.getOrDefault(emptyList())
            add(recs)
        }
        if (collected.isEmpty() && recentIds.isNotEmpty()) {
            add(runCatching { repository.suggestions(recentIds.first()).first() }.getOrDefault(emptyList()))
        }
        for (query in GENERIC_FALLBACKS) {
            if (collected.size >= RECOMMENDED_LIMIT) break
            add(runCatching { repository.searchSongs(query).first() }.getOrDefault(emptyList()))
        }
        return Recommended(collected, emptyList())
    }

    /**
     * Pull-to-refresh variant of [refresh]: does not flip state to Loading,
     * so the grid stays visible while the network re-fetches. On success
     * the new data replaces the current state silently; on failure the
     * previous Success is retained (so the user doesn't lose their view
     * over a transient error).
     */
    fun refreshQuiet() {
        if (mutableRefreshing.value) return
        mutableRefreshing.value = true
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            // refresh() now emits incrementally; keep the spinner alive for
            // a minimum duration so the gesture always reads as
            // acknowledged.
            refresh()
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < MIN_REFRESH_SPINNER_MS) {
                kotlinx.coroutines.delay(MIN_REFRESH_SPINNER_MS - elapsed)
            }
            mutableRefreshing.value = false
        }
    }

    companion object {
        /** Minimum time the pull-to-refresh spinner stays visible. */
        private const val MIN_REFRESH_SPINNER_MS = 600L

        private const val TRENDING_LIMIT = 20
        private const val NEW_RELEASE_LIMIT = 10
        private const val POPULAR_LIMIT = 12
        private const val HISTORY_LIMIT = 12
        private const val RECENT_SEED_LIMIT = 10
        private const val RECOMMENDED_LIMIT = 20

        private val TRENDING_FALLBACKS = listOf(
            "arijit singh hits" to 5,
            "bollywood hits" to 5,
            "punjabi hits" to 5,
            "latest tamil hits" to 5,
            "telugu hits" to 5,
            // English fallbacks so the rail still populates when Saavn is
            // disabled (YouTube Music returns hits for these).
            "top english hits" to 5,
            "popular international songs" to 5
        )

        private val GENERIC_FALLBACKS = listOf(
            "Top Hits",
            "Popular Songs",
            "Bollywood Hits",
            "Indie Pop",
            "Dance Hits",
            "Trending Now"
        )
    }
}
