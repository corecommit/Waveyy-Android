package com.waveyy.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.local.dao.HistoryDao
import com.waveyy.app.data.local.datastore.HomeSnapshotStore
import com.waveyy.app.data.remote.mapper.SongMapper
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.SongRepository
import com.waveyy.app.domain.model.Album
import com.waveyy.app.domain.model.Artist
import com.waveyy.app.domain.model.HomeSnapshot
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
    /** Top-of-home list of 6 tracks drawn from history + recommendations. */
    val quickPicks: List<Song> = emptyList(),
    /** Horizontal rail: recently played songs. */
    val recentlyPlayed: List<Song> = emptyList(),
    /** Horizontal rail: personalized songs from genre seeds + fallbacks. */
    val madeForYou: List<Song> = emptyList(),
    /** Horizontal rail: artists derived from the base song set. */
    val artists: List<Artist> = emptyList(),
    /** Horizontal rail: albums derived from the base song set. */
    val albums: List<Album> = emptyList()
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
    private val snapshotStore: HomeSnapshotStore,
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
     * Reloads every rail. When [force] is false and a Home snapshot exists
     * that is under [FRESH_WINDOW_MS] old, skip the network entirely — the
     * user sees the cached rails instantly.
     *
     * Order of operations:
     *   1. recentlyPlayed from Room (always available synchronously).
     *   2. Cached snapshot for the four network rails, if any.
     *   3. Network fetch (skipped when fresh and !force), rails published
     *      incrementally as each source lands.
     *   4. Snapshot written on success.
     */
    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            // 1. Recent history from Room.
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

            // 2. Cached snapshot for the four network rails.
            val snapshot = runCatching { snapshotStore.read() }.getOrNull()
            if (snapshot != null) {
                mutableState.value = HomeUiState.Success(
                    HomeSections(
                        quickPicks = snapshot.quickPicks,
                        recentlyPlayed = recent,
                        madeForYou = snapshot.madeForYou,
                        artists = snapshot.artists,
                        albums = snapshot.albums
                    )
                )
                val ageMs = System.currentTimeMillis() - snapshot.timestampMs
                if (!force && ageMs < FRESH_WINDOW_MS) {
                    Timber.tag("HomeVM").d(
                        "snapshot fresh (%ds old), skipping network", ageMs / 1000
                    )
                    return@launch
                }
                Timber.tag("HomeVM").d(
                    "snapshot stale (%ds old), refreshing", ageMs / 1000
                )
            } else {
                // No snapshot — start from empty Success so skeletons render.
                mutableState.value = HomeUiState.Success(
                    HomeSections(recentlyPlayed = recent)
                )
            }

            // 3. Network fetch. Rails land incrementally.
            coroutineScope {
                val baseJob = async {
                    runCatching { loadTrending() }.getOrDefault(emptyList())
                }
                val mfyJob = async {
                    runCatching { loadRecommended(recent) }
                        .getOrElse { Recommended(emptyList(), emptyList()) }
                }
                val newReleasesJob = async {
                    runCatching { loadNewReleases() }.getOrDefault(emptyList())
                }

                val recs = mfyJob.await()
                publishSections {
                    it.copy(
                        madeForYou = recs.songs,
                        quickPicks = deriveQuickPicks(recent, recs.songs)
                    )
                }

                val base = baseJob.await()
                publishSections {
                    it.copy(artists = popularArtistsOf(base))
                }

                val newReleases = newReleasesJob.await()
                val popularAlbums = popularAlbumsOf(base)
                val newReleaseAlbums = newReleases.mapNotNull { song ->
                    val album = song.album.takeIf { it.isNotBlank() && it != "unknown" }
                        ?: return@mapNotNull null
                    Album(
                        id = "album:$album:${song.artist}",
                        name = album,
                        artist = song.artist,
                        imageUrl = song.imageUrl,
                        songs = emptyList()
                    )
                }
                val merged = (popularAlbums + newReleaseAlbums).distinctBy { it.id }
                publishSections { it.copy(albums = merged) }
            }

            // 4. Persist snapshot for next launch.
            val finalSections = (mutableState.value as? HomeUiState.Success)?.sections
            if (finalSections != null && finalSections.hasNetworkContent()) {
                runCatching {
                    snapshotStore.write(
                        HomeSnapshot(
                            quickPicks = finalSections.quickPicks,
                            madeForYou = finalSections.madeForYou,
                            artists = finalSections.artists,
                            albums = finalSections.albums,
                            timestampMs = System.currentTimeMillis()
                        )
                    )
                }.onFailure {
                    Timber.tag("HomeVM").w(it, "snapshot write failed")
                }
            }
        }
    }

    /** True when at least one network rail has content. Guards against
     *  overwriting a good snapshot with an all-empty failure result. */
    private fun HomeSections.hasNetworkContent(): Boolean {
        return quickPicks.isNotEmpty() ||
            madeForYou.isNotEmpty() ||
            artists.isNotEmpty() ||
            albums.isNotEmpty()
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

    /**
     * Interleaves [recent] and [recommended] to produce up to
     * [QUICK_PICKS_LIMIT] tracks, deduped by title-key + primary artist.
     * Yields the YT Music-style "quick picks" list.
     */
    private fun deriveQuickPicks(
        recent: List<Song>,
        recommended: List<Song>
    ): List<Song> {
        val out = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        val max = if (recent.size >= recommended.size) recent.size else recommended.size
        for (i in 0 until max) {
            if (out.size >= QUICK_PICKS_LIMIT) break
            recent.getOrNull(i)?.let { song ->
                val key = quickPickKey(song)
                if (seen.add(key)) out += song
            }
            if (out.size >= QUICK_PICKS_LIMIT) break
            recommended.getOrNull(i)?.let { song ->
                val key = quickPickKey(song)
                if (seen.add(key)) out += song
            }
        }
        return out
    }

    /** Dedupe key for Quick Picks. */
    private fun quickPickKey(song: Song): String {
        val title = com.waveyy.app.domain.usecase.SearchRanking.titleKey(song.name)
        val artist = song.artist.lowercase().split(",").first().trim()
        return "$title||$artist"
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

    /**
     * Builds the trending rail by firing every source query in parallel,
     * then curating the results in the same priority order the old
     * sequential version used: locale → topCharts → fallbacks →
     * english. Parallelising drops first-paint time from the sum of every
     * request to the slowest single request.
     */
    private suspend fun loadTrending(): List<Song> {
        val localeLang = java.util.Locale.getDefault().getDisplayLanguage(java.util.Locale.ENGLISH)
        val localeIsHindi = localeLang.equals("hindi", ignoreCase = true)

        // Each source is a (query, limit) pair. A special "__charts__"
        // marker means "call repository.topCharts() instead of searchSongs".
        val sources = buildList {
            if (!localeIsHindi) add("$localeLang top hits" to TRENDING_LIMIT)
            add("__charts__" to TRENDING_LIMIT)
            TRENDING_FALLBACKS.forEach { (query, max) -> add(query to max) }
            if (!localeIsHindi) add("English top hits" to TRENDING_LIMIT)
        }

        // Fire all requests in parallel.
        val results: List<List<Song>> = coroutineScope {
            sources.map { (query, _) ->
                async {
                    runCatching {
                        if (query == "__charts__") {
                            repository.topCharts().first()
                        } else {
                            repository.searchSongs(query).first()
                        }
                    }.getOrDefault(emptyList())
                }
            }.awaitAll()
        }

        // Curate in priority order. Curation itself stays sequential so
        // cross-source dedupe via trackKeys works exactly as before.
        var trackKeys = emptySet<String>()
        val trending = mutableListOf<Song>()
        sources.forEachIndexed { index, (query, max) ->
            if (trending.size >= TRENDING_LIMIT || max <= 0) return@forEachIndexed
            val songs = results.getOrNull(index).orEmpty()
            if (songs.isEmpty()) {
                Timber.tag("HomeVM").d("trending source '%s' empty", query)
                return@forEachIndexed
            }
            runCatching {
                val (picked, next) = TrendingCurator.curate(
                    songs.map(::toDto),
                    minOf(max, TRENDING_LIMIT - trending.size),
                    trackKeys
                )
                trending += SongMapper.toDomainList(picked)
                trackKeys = next
            }.onFailure {
                Timber.tag("HomeVM").w(it, "curate failed for '%s'", query)
            }
        }
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
            refresh(force = true)
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

        /** Snapshot TTL: skip the network when cached this recently. */
        private const val FRESH_WINDOW_MS = 15L * 60L * 1000L

        /** Quick Picks list size (top of Home). */
        private const val QUICK_PICKS_LIMIT = 6

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
