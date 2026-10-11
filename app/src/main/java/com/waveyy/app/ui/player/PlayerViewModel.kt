package com.waveyy.app.ui.player

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.local.dao.HistoryDao
import com.waveyy.app.data.local.HistoryEntity
import com.waveyy.app.data.local.PlayLogEntity
import com.waveyy.app.data.local.dao.PlayLogDao
import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.data.lyrics.LyricsRaceOrchestrator
import com.waveyy.app.data.lyrics.LyricsRaceRepository
import com.waveyy.app.data.lyrics.LyricsResult
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.ListeningHistoryRepository
import com.waveyy.app.data.repository.LyricsRepository
import com.waveyy.app.data.repository.YtMusicRepository
import com.waveyy.app.domain.model.AudioStream
import com.waveyy.app.domain.model.Artist
import com.waveyy.app.domain.model.MusicSource
import com.waveyy.app.data.repository.SongRepository
import com.waveyy.app.domain.model.CachedLyrics
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.PlaybackConnection
import com.waveyy.app.util.LyricsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.net.URL
import javax.inject.Inject

/** Lyrics panel state. */
sealed interface LyricsUiState {
    /** Nothing loaded yet. */
    data object Empty : LyricsUiState

    /** Loading. */
    data object Loading : LyricsUiState

    /** Parsed synced lines (raw lyrics, transliteration applied at render). */
    data class Ready(val lines: List<com.waveyy.app.domain.model.LyricLine>) : LyricsUiState

    /** Untimed lyrics text (centered static display). */
    data class Plain(val text: String) : LyricsUiState

    /** Track has no vocals. */
    data object Instrumental : LyricsUiState
}

/**
 * Player screen logic: queue ownership, lyric loading, history hookup,
 * share and MediaStore download. Transport itself lives in
 * [PlaybackConnection]; this model never calls `audio.play()` equivalents.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val connection: PlaybackConnection,
    private val lyricsRepository: LyricsRepository,
    private val historyDao: HistoryDao,
    private val playLogDao: PlayLogDao,
    private val historyRepository: ListeningHistoryRepository,
    private val authRepository: AuthRepository,
    private val songRepository: SongRepository,
    private val userPrefs: UserPrefs,
    private val ytmusic: YtMusicRepository,
    private val lyricsRace: LyricsRaceRepository
) : ViewModel() {

    private val mutableQueue = MutableStateFlow<List<Song>>(emptyList())
    /** Current queue. */
    val queue: StateFlow<List<Song>> = mutableQueue.asStateFlow()

    private val mutableLyrics = MutableStateFlow<LyricsUiState>(LyricsUiState.Empty)
    /** Lyrics panel state. */
    val lyrics: StateFlow<LyricsUiState> = mutableLyrics.asStateFlow()

    private val mutableHinglish = MutableStateFlow(false)
    /** Romanized-lyrics toggle (persisted). */
    val hinglishEnabled: StateFlow<Boolean> = userPrefs.hinglishFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    private var lastLoggedId: String? = null

    /** Last seed id we fetched radio for (debounce). */
    private var lastRadioSeedId: String? = null
    private var sleepJob: Job? = null
    private var sleepEndOfSong = false

    init {
        // Reconnects the UI to an already-playing session (swipe-away
        // reopen): the activity-scoped queue is gone but the singleton
        // controller still holds the items, so rebuild from its metadata.
        // Also resumes the last-played position once, if the same song
        // is still loaded and the saved position is past the floor.
        viewModelScope.launch {
            connection.refreshState()
            val saved = userPrefs.lastPlayed()
            var didSeek = false
            connection.state.collect { state ->
                if (state.mediaId != null && mutableQueue.value.isEmpty()) {
                    val restored = connection.currentQueueSongs()
                    if (restored.isNotEmpty()) mutableQueue.value = restored
                }
                if (!didSeek && saved != null && state.mediaId != null) {
                    val (savedId, savedPos) = saved
                    if (state.mediaId == savedId &&
                        savedPos >= RESUME_MIN_POSITION_MS &&
                        state.positionMs < 1_000L
                    ) {
                        connection.seekTo(savedPos)
                        Timber.tag("PlayerVM").d(
                            "resumed %s at %dms", savedId, savedPos
                        )
                    }
                    didSeek = true
                }
            }
        }
    }

    /** Lyrics provider id (preferred-provider mode). */
    val lyricsProvider: StateFlow<String> = userPrefs.lyricsProviderFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UserPrefs.DEFAULT_LYRICS_PROVIDER)

    /** Lyrics race mode (`auto` races all providers). */
    val lyricsMode: StateFlow<String> = userPrefs.lyricsModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UserPrefs.LYRICS_MODE_AUTO)

    /** Race-winner caption toggle (always-on after phase 68). */
    val lyricsDebug: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    private val mutableWinner = MutableStateFlow<LyricsRaceOrchestrator.Scored?>(null)
    /** Latest race winner (provider + score, for the debug caption). */
    val raceWinner: StateFlow<LyricsRaceOrchestrator.Scored?> = mutableWinner.asStateFlow()

    private val mutablePlayerError = MutableStateFlow<String?>(null)
    /** Playback failure message for the Snackbar (null when clear). */
    val playerError: StateFlow<String?> = mutablePlayerError.asStateFlow()

    private val mutablePreparingFor = MutableStateFlow<String?>(null)
    /** True while a tap is resolving / preparing (drives the play-button spinner). */
    val isPreparing: StateFlow<Boolean> = mutablePreparingFor
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    /** Clears a surfaced playback error (after the Snackbar is shown). */
    fun clearPlayerError() {
        mutablePlayerError.value = null
    }

    /** Artist details for the player cards (null when unknown). */
    fun artistFlow(id: String): kotlinx.coroutines.flow.Flow<Artist?> {
        return songRepository.artistById(id)
    }

    /** Mini-player hidden flag (set by swipe-down, cleared on new song start). */
    val miniPlayerHidden: StateFlow<Boolean> = userPrefs.miniPlayerHiddenFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    /** Pauses playback and hides the mini-player until the next song starts. */
    fun hideMiniPlayer() {
        viewModelScope.launch {
            connection.pause()
            userPrefs.setMiniPlayerHidden(true)
        }
    }

    /** Stops playback and hides the mini-player (swipe-down gesture). */
    fun stopAndHide() {
        viewModelScope.launch {
            connection.stopPlayback()
            userPrefs.setMiniPlayerHidden(true)
        }
    }

    /** Clears the swipe-down hidden flag (called when a new song starts). */
    fun clearMiniPlayerHidden() {
        viewModelScope.launch { userPrefs.setMiniPlayerHidden(false) }
    }

    /** Persists the lyrics provider choice. */
    fun setLyricsProvider(provider: String) {
        viewModelScope.launch { userPrefs.setLyricsProvider(provider) }
    }

    /**
     * Replaces the queue and starts playback, then records history.
     * Mirrors tapping a song anywhere in the web app. Stream-less songs are
     * dropped up front so this queue stays index-identical to the
     * controller queue built by [QueueManager]; the tapped song is
     * re-resolved by id so its index survives filtering. YouTube songs
     * resolve their stream URLs lazily first.
     */
    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            val resolved = ensureFreshStreams(resolveStreams(songs))
            if (resolved.isEmpty()) {
                mutablePlayerError.value = UNPLAYABLE_MESSAGE
                return@launch
            }
            val tappedId = songs.getOrNull(startIndex)?.id
            val index = resolved.indexOfFirst { it.id == tappedId }.takeIf { it >= 0 } ?: 0
            mutableQueue.value = resolved
            userPrefs.setMiniPlayerHidden(false)
            connection.playSongs(resolved, index)
            onTrackChanged(resolved[index].id)
        }
    }

    /**
     * Plays a tap source. Resolves ONLY the tapped song synchronously so
     * playback starts within ~500ms; the rest of the list is resolved in the
     * background and the queue is swapped in once ready, preserving the
     * current position. Fixes the "spam Continue to make it play" symptom
     * when the source list has 10+ stream-less songs.
     */
    /**
     * Plays a tap source. Resolves only the tapped song synchronously, then
     * plays it as a single-item queue. The rest of the list is resolved and
     * appended in the background.
     *
     * The tap id is captured BEFORE refreshStreams runs, because that method
     * can return a song with a different id (JioSaavn matches a different
     * version of the same track by name). Comparing against the resolved id
     * would let the original tapped entry slip through the filter and get
     * appended as a duplicate — that's the "same song plays twice" bug.
     */
    fun playSource(source: com.waveyy.app.playback.QueueSource, startIndex: Int = 0) {
        viewModelScope.launch {
            val tappedSong = source.songs.getOrNull(startIndex)
            if (tappedSong == null || tappedSong.id.isBlank()) {
                mutablePlayerError.value = UNPLAYABLE_MESSAGE
                return@launch
            }

            // Show the mini player immediately with the tapped song's
            // metadata (unresolved — its stream is not yet fetched). This
            // is what makes the pill appear on tap instead of after
            // resolution completes. The queue is replaced with the
            // resolved version further down.
            mutableQueue.value = listOf(tappedSong)
            userPrefs.setMiniPlayerHidden(false)
            lastRadioSeedId = null
            val myPreparingId = tappedSong.id
            mutablePreparingFor.value = myPreparingId
            // Stop the previous song immediately. Without this, the old
            // track plays for 2-10s while the new one resolves. User-
            // reported: "previous song keeps playing until the new song
            // starts."
            connection.pause()

            val originalTappedId = tappedSong.id
            var resolvedTapped: Song? = null

            try {
                // 1. Resolve the tapped song. Its id may or may not change.
                resolvedTapped = if (tappedSong.streams.any { it.url.isNotBlank() }) {
                    tappedSong
                } else {
                    runCatching { refreshStreams(tappedSong) }.getOrNull() ?: tappedSong
                }
                if (resolvedTapped.streams.none { it.url.isNotBlank() }) {
                    mutablePlayerError.value = UNPLAYABLE_MESSAGE
                    return@launch
                }

                // 2. Build the single MediaItem and play it.
                val singleItem = com.waveyy.app.playback.QueueManager()
                    .toMediaItem(resolvedTapped) ?: run {
                        mutablePlayerError.value = UNPLAYABLE_MESSAGE
                        return@launch
                    }
                mutableQueue.value = listOf(resolvedTapped)
                connection.playSingleItem(singleItem)
                onTrackChanged(resolvedTapped.id)

                // 3. Wait until the player has actually left the buffering
                // state for this item, or timeout at 8s. The spinner stays
                // visible until this resolves.
                withTimeoutOrNull(8_000L) {
                    connection.state.first { s ->
                        s.mediaId == resolvedTapped.id && !s.isBuffering
                    }
                }
            } finally {
                // Only clear if THIS call still owns the spinner. A newer tap
                // that started resolving after us must not have its spinner
                // cleared by our finally block.
                if (mutablePreparingFor.value == myPreparingId) {
                    mutablePreparingFor.value = null
                }
            }

            val finalResolved = resolvedTapped ?: return@launch
            val generation = connection.currentGeneration()

            // 4. Background: resolve the rest and append. Every filter here
            // compares against originalTappedId, not finalResolved.id, so the
            // tapped song can never slip back in under a different id.
            launch {
                val others = source.songs.filterIndexed { i, s ->
                    i != startIndex &&
                        s.id.isNotBlank() &&
                        s.id != originalTappedId
                }
                if (others.isEmpty()) return@launch

                val resolvedOthers = coroutineScope {
                    others.map { song ->
                        async {
                            if (song.streams.any { s -> s.url.isNotBlank() }) {
                                song
                            } else {
                                runCatching { refreshStreams(song) }.getOrNull() ?: song
                            }
                        }
                    }.awaitAll()
                }.filter { it.streams.any { s -> s.url.isNotBlank() } }

                val seenIds = mutableSetOf<String>()
                seenIds.add(originalTappedId)
                seenIds.add(finalResolved.id)
                val extraSongs = resolvedOthers.filter { seenIds.add(it.id) }
                if (extraSongs.isEmpty()) return@launch

                val stillOnTapped = connection.state.value.mediaId == finalResolved.id
                if (!stillOnTapped) {
                    Timber.tag("PlayerVM").d("skipping backfill: user moved on")
                    return@launch
                }
                if (connection.currentGeneration() != generation) {
                    Timber.tag("PlayerVM").d("skipping backfill: superseded by newer tap")
                    return@launch
                }

                val items = com.waveyy.app.playback.QueueManager()
                    .toMediaItems(extraSongs)
                if (items.isEmpty()) return@launch

                connection.appendItems(items)
                mutableQueue.value = listOf(finalResolved) + extraSongs
                Timber.tag("PlayerVM").d(
                    "queue appended: %d extra songs, current kept",
                    extraSongs.size
                )
            }
        }
    }

    /**
     * Resolves missing stream URLs in parallel before playback: YouTube
     * songs resolve lazily, and stream-less JioSaavn songs (history,
     * playlists, artist tops) are re-fetched by id so a tap never
     * silently no-ops on stale metadata. Unresolvable songs are dropped.
     */
    private suspend fun resolveStreams(songs: List<Song>): List<Song> {
        val needsResolution = songs.any { song ->
            song.streams.none { it.url.isNotBlank() }
        }
        if (!needsResolution) {
            return songs.filter { it.streams.any { s -> s.url.isNotBlank() } }.dedupe()
                .also(::stampAll)
        }
        return coroutineScope {
            songs.map { song ->
                async {
                    runCatching {
                        if (song.streams.none { it.url.isNotBlank() }) {
                            refreshStreams(song) ?: song
                        } else {
                            song
                        }
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull().filter { it.streams.any { s -> s.url.isNotBlank() } }.dedupe()
                .also(::stampAll)
        }
    }

    /** Marks [songs] as freshly resolved (stream TTL counts from now). */
    private fun stampAll(songs: List<Song>) {
        val now = System.currentTimeMillis()
        songs.forEach { queueResolvedAt[it.id] = now }
    }

    /** Deduplicates by song id, keeping the first occurrence (never by name). */
    private fun List<Song>.dedupe(): List<Song> {
        val seen = mutableSetOf<String>()
        return filter { it.id.isNotBlank() && seen.add(it.id) }
    }

    /** Re-fetches playable streams for one stream-less [song]. */
    private suspend fun refreshStreams(song: Song): Song? {
        if (song.source == MusicSource.YTMUSIC) {
            val url = ytmusic.streamUrl(song.id.removePrefix("yt:"))
            if (url.isNullOrBlank()) return null
            return song.copy(
                streams = listOf(
                    AudioStream(url = url, quality = "", bitrateKbps = 0, lossless = false)
                )
            )
        }
        return songRepository.refreshSong(song)
    }

    private val queueResolvedAt = mutableMapOf<String, Long>()

    /** Ids of songs whose prefetch is currently in flight (dedup). */
    private val prefetchInFlight = mutableSetOf<String>()

    /** Re-resolves stale streams (older than 30 min) before playing. */
    suspend fun ensureFreshStreams(songs: List<Song>): List<Song> {
        val now = System.currentTimeMillis()
        return songs.map { song ->
            val lastResolved = queueResolvedAt[song.id] ?: 0L
            if (now - lastResolved < STREAM_TTL_MS && song.streams.any { it.url.isNotBlank() }) {
                song
            } else {
                val refreshed = refreshStreams(song)
                if (refreshed != null) {
                    queueResolvedAt[refreshed.id] = now
                    refreshed
                } else {
                    song
                }
            }
        }
    }

    /**
     * Kicks off background resolution of the stream URL for the songs
     * ahead of the current index, so the next advance()/rewind() is
     * instant. Resolves up to [aheadCount] songs after [fromIndex] AND
     * the one song before it (for rewind). Silent — failures are logged,
     * not surfaced.
     */
    private fun prefetchAround(fromIndex: Int) {
        val queue = mutableQueue.value
        if (queue.isEmpty()) return

        // Targets: next 2 forward + 1 backward, in that priority order.
        val targets = buildList {
            for (offset in 1..2) {
                queue.getOrNull(fromIndex + offset)?.let { add(it) }
            }
            queue.getOrNull(fromIndex - 1)?.let { add(it) }
        }.filter { song ->
            song.id.isNotBlank() &&
                !prefetchInFlight.contains(song.id) &&
                isStale(song)
        }

        if (targets.isEmpty()) return

        targets.forEach { song ->
            prefetchInFlight += song.id
            viewModelScope.launch {
                try {
                    val fresh = refreshStreams(song)
                    if (fresh != null) {
                        // Swap in the freshly resolved song wherever it
                        // currently sits in the queue, but only if the user
                        // hasn't moved on since we started.
                        val current = mutableQueue.value
                        val idx = current.indexOfFirst { it.id == song.id }
                        if (idx >= 0) {
                            val updated = current.toMutableList().also { it[idx] = fresh }
                            mutableQueue.value = updated
                            queueResolvedAt[fresh.id] = System.currentTimeMillis()
                            // Also swap the controller item so the URL is
                            // ready when ExoPlayer advances to it.
                            connection.replaceItem(idx, fresh)
                            Timber.tag("PlayerVM").d(
                                "prefetched %s (idx=%d)", fresh.id, idx
                            )
                        }
                    }
                } finally {
                    prefetchInFlight -= song.id
                }
            }
        }
    }

    /** True when [song]'s URL is missing or older than the freshness TTL. */
    private fun isStale(song: Song): Boolean {
        val last = queueResolvedAt[song.id] ?: 0L
        val hasStream = song.streams.any { it.url.isNotBlank() }
        return !hasStream || System.currentTimeMillis() - last >= STREAM_TTL_MS
    }

    /**
     * Advances to the next item. Instant: URL freshness is handled by
     * [prefetchAround], which runs while the current song plays. If the
     * prefetch hasn't finished (rare — usually fires well before the
     * user skips), the controller plays with the stale URL; ExoPlayer's
     * built-in retry logic surfaces an error only if the URL is actually
     * dead, and the Snackbar's Retry re-resolves.
     */
    fun advance() {
        viewModelScope.launch { connection.next() }
    }

    /** Returns to the previous item. Instant, same reasoning as [advance]. */
    fun rewind() {
        viewModelScope.launch { connection.previous() }
    }

    /** Plays a single song (queue of one). */
    fun playSong(song: Song) {
        playSongs(listOf(song), 0)
    }

    /** Records history + listening event on track change (fire-and-forget). */
    fun onTrackChanged(songId: String?) {
        if (songId.isNullOrBlank() || songId == lastLoggedId) return
        if (sleepEndOfSong) {
            sleepEndOfSong = false
            viewModelScope.launch {
                delay(500L)
                if (connection.state.value.isPlaying) connection.togglePlayPause()
            }
        }
        lastLoggedId = songId
        // Radio autoplay: queue up related songs behind the current track
        // so playback doesn't fall off the end into random covers. Fires
        // in the background; won't block anything.
        maybeExtendQueueWithRadio(songId)
        // Prefetch around the new track so skip-forward/skip-back is
        // instant. Fires on every track change (including natural advance
        // and manual tap).
        val currentIndex = mutableQueue.value.indexOfFirst { it.id == songId }
        if (currentIndex >= 0) {
            prefetchAround(currentIndex)
        }
        val song = mutableQueue.value.firstOrNull { it.id == songId }
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (song != null) {
                historyDao.upsert(
                    HistoryEntity(song.id, song.name, song.artist, song.imageUrl, now)
                )
                historyDao.trimTo(HISTORY_CAP)
            }
            playLogDao.insert(PlayLogEntity(songId = songId, playedAt = now))
            playLogDao.trimTo(PLAY_LOG_CAP)
            val userId = authRepository.currentUser()?.id
            if (userId != null) {
                // Best-effort cloud log; failures (incl. missing table) stay silent here.
                historyRepository.logListeningEvent(userId, songId)
            }
        }
    }

    /**
     * Loads lyrics for [song]: provider race first (auto or preferred mode),
     * legacy proxy fallback, empty when disabled. [bypassCache] re-races
     * (Retry button).
     */
    fun loadLyrics(song: Song, bypassCache: Boolean = false) {
        viewModelScope.launch {
            val auto = userPrefs.lyricsModeFlow().first() != UserPrefs.LYRICS_MODE_PREFERRED
            val preferred = userPrefs.lyricsProviderFlow().first()
            if (!auto && preferred == "none") {
                mutableLyrics.value = LyricsUiState.Empty
                return@launch
            }

            // Cache-first: when fresh lyrics are already stored, render them
            // immediately without flashing Loading. Cuts the lyrics-panel
            // open time on repeat visits to ~0.
            if (!bypassCache) {
                val cached = lyricsRace.getCachedFast(
                    songId = song.id,
                    title = song.name,
                    artist = song.artist
                )
                if (cached != null) {
                    mutableWinner.value = cached
                    mutableLyrics.value = raceStateOf(cached.result)
                    return@launch
                }
            }

            // Cache miss — show Loading and race.
            mutableLyrics.value = LyricsUiState.Loading
            mutableWinner.value = null
            val raced = lyricsRace.getLyrics(
                songId = song.id,
                artist = song.artist,
                title = song.name,
                duration = song.durationSec,
                bypassCache = bypassCache,
                onlyProvider = if (auto) null else preferred
            )
            if (raced != null) {
                mutableWinner.value = raced
                mutableLyrics.value = raceStateOf(raced.result)
                return@launch
            }
            if (!auto && preferred != UserPrefs.DEFAULT_LYRICS_PROVIDER) {
                mutableLyrics.value = LyricsUiState.Empty
                return@launch
            }
            val key = lyricsKey(song)
            lyricsRepository.getLyrics(
                cacheKey = key,
                track = song.name,
                artist = song.artist,
                album = song.album.takeIf { it != "unknown" },
                duration = song.durationSec.takeIf { it > 0 }?.toString()
            ).collect { cached ->
                mutableLyrics.value = lyricsStateOf(cached)
            }
        }
    }

    private fun raceStateOf(result: LyricsResult): LyricsUiState {
        return when (result) {
            is LyricsResult.Instrumental -> LyricsUiState.Instrumental
            is LyricsResult.Plain -> {
                val text = LyricsParser.sanitizeText(result.text)
                if (text.isBlank()) LyricsUiState.Empty else LyricsUiState.Plain(text)
            }
            is LyricsResult.Synced -> {
                val lines = LyricsParser.parseLrc(LyricsParser.sanitizeText(result.lrc))
                if (lines.isEmpty()) LyricsUiState.Empty else LyricsUiState.Ready(lines)
            }
        }
    }

    private fun lyricsStateOf(cached: CachedLyrics?): LyricsUiState {
        if (cached == null || (cached.synced.isBlank() && cached.plain.isBlank())) {
            return LyricsUiState.Empty
        }
        val lines = LyricsParser.parseLrc(cached.synced.ifBlank { cached.plain })
        return LyricsUiState.Ready(lines)
    }

    private fun lyricsKey(song: Song): String {
        return "lyrics:${song.name}:${song.artist}:${song.album}:${song.durationSec}".lowercase()
    }

    /** Starts a radio queue: the seed song plus its recommendations. */
    fun startRadio(song: Song) {
        viewModelScope.launch {
            val recs = fetchRadioFor(song)
            val queue = (listOf(song) + recs).distinctBy { it.id }
            if (queue.isNotEmpty()) {
                playSource(com.waveyy.app.playback.QueueSource.Recommendations(queue), 0)
            }
        }
    }

    /**
     * Fetches a radio-style list of songs related to [seed]. Combines:
     *  1. JioSaavn's `/song/{id}/suggestions` endpoint (curated by mood)
     *  2. Same-artist search (top tracks by the primary artist)
     * Merged, deduped, filtered against covers/karaoke/live versions.
     * Returned in ranking order: suggestions first, then same-artist.
     */
    private suspend fun fetchRadioFor(seed: Song): List<Song> = coroutineScope {
        // YT Music's /next endpoint gives the actual YouTube-curated radio
        // for a video. Only available when the seed is a YT track (we have
        // a videoId). For JioSaavn seeds, fall back to JioSaavn suggestions
        // + same-artist search.
        val isYtSeed = seed.source == com.waveyy.app.domain.model.MusicSource.YTMUSIC &&
            seed.id.startsWith("yt:")
        val ytDeferred = if (isYtSeed) {
            async {
                runCatching {
                    songRepository.ytRadioFor(seed.id.removePrefix("yt:"))
                }.getOrDefault(emptyList())
            }
        } else {
            null
        }
        val suggestionsDeferred = async {
            runCatching { songRepository.suggestions(seed.id).first() }
                .getOrDefault(emptyList())
        }
        val primaryArtist = seed.artist
            .split(",").firstOrNull()?.trim().orEmpty()
            .takeIf { it.isNotBlank() && it != "unknown" }
        val sameArtistDeferred: Deferred<List<Song>>? = if (primaryArtist != null) {
            async {
                runCatching { songRepository.searchSongs(primaryArtist).first() }
                    .getOrDefault(emptyList())
            }
        } else {
            null
        }

        val ytRadio = ytDeferred?.await().orEmpty()
        val suggestions = suggestionsDeferred.await()
        val sameArtist = sameArtistDeferred?.await().orEmpty()

        // Priority order: YT radio first (best), then JioSaavn suggestions,
        // then same-artist search. Interleave to keep the mix varied.
        val merged = mutableListOf<Song>()
        if (ytRadio.isNotEmpty()) {
            merged += ytRadio
        }
        val max = if (suggestions.size >= sameArtist.size) suggestions.size else sameArtist.size
        for (i in 0 until max) {
            suggestions.getOrNull(i)?.let { merged += it }
            sameArtist.getOrNull(i)?.let { merged += it }
        }

        // Filter: exclude the seed, covers/karaoke/live/instrumental
        // versions, and anything already in the current queue.
        val existingIds = mutableQueue.value.map { it.id }.toSet()
        merged
            .asSequence()
            .filter { it.id != seed.id && it.id !in existingIds }
            .filter { it.id.isNotBlank() }
            .filter { song -> !isCoverOrKaraoke(song) }
            .filter { song -> song.streams.isNotEmpty() || song.id.isNotBlank() }
            .distinctBy { song ->
                com.waveyy.app.domain.usecase.SearchRanking.titleKey(song.name) + "||" +
                    song.artist.lowercase().split(",").first().trim()
            }
            .take(RADIO_APPEND_LIMIT)
            .toList()
    }

    /**
     * True when [song] looks like a cover, karaoke, live, instrumental,
     * 8-bit, or remix version — anything we don't want in the autoplay
     * tail. Uses title + artist heuristics; skip-lists are case-insensitive.
     */
    private fun isCoverOrKaraoke(song: Song): Boolean {
        val blob = (song.name + " " + song.artist).lowercase()
        return COVER_KEYWORDS.any { it in blob }
    }

    /**
     * Appends related songs to the end of the queue when the current track
     * has 3 or fewer tracks left after it. Debounced — won't re-fire for
     * the same seed id. Radio songs are appended, never inserted, so the
     * user's own queue order stays intact.
     */
    private fun maybeExtendQueueWithRadio(seedId: String) {
        if (seedId == lastRadioSeedId) return
        val queue = mutableQueue.value
        val seedIndex = queue.indexOfFirst { it.id == seedId }
        if (seedIndex < 0) return
        val remaining = queue.size - seedIndex - 1
        if (remaining > RADIO_TRIGGER_AT_REMAINING) return

        lastRadioSeedId = seedId
        val seed = queue[seedIndex]
        val generationAtStart = connection.currentGeneration()

        viewModelScope.launch {
            val radio = fetchRadioFor(seed)
            if (radio.isEmpty()) return@launch

            // Guard: user must not have skipped to a different song, and
            // a new playSource must not have replaced the queue.
            if (connection.currentGeneration() != generationAtStart) {
                Timber.tag("PlayerVM").d("radio skipped: newer session")
                return@launch
            }
            if (connection.state.value.mediaId != seedId) {
                Timber.tag("PlayerVM").d("radio skipped: moved past seed")
                return@launch
            }

            val items = com.waveyy.app.playback.QueueManager()
                .toMediaItems(radio)
            if (items.isEmpty()) return@launch

            connection.appendItems(items)
            mutableQueue.value = mutableQueue.value + radio
            Timber.tag("PlayerVM").d(
                "radio appended: %d tracks behind %s", radio.size, seed.name
            )
        }
    }

    /**
     * Schedules a sleep timer. Null cancels. Minutes count down in the
     * background; on fire, playback pauses.
     */
    fun setSleepTimer(minutes: Long?) {
        sleepJob?.cancel()
        sleepJob = null
        sleepEndOfSong = false
        if (minutes == null || minutes <= 0L) return
        sleepJob = viewModelScope.launch {
            delay(minutes * 60L * 1000L)
            if (connection.state.value.isPlaying) connection.togglePlayPause()
        }
    }

    /** Pauses when the current song ends (one-shot). */
    fun setSleepEndOfSong() {
        sleepJob?.cancel()
        sleepJob = null
        sleepEndOfSong = true
    }

    /** Sets playback speed (0.5x-2.0x). */
    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch { connection.setPlaybackSpeed(speed) }
    }

    /** Toggles romanized lyrics (persisted across restarts). */
    fun toggleHinglish() {
        viewModelScope.launch {
            userPrefs.setHinglish(!(hinglishEnabled.value))
        }
    }

    /** Sets the romanized-lyrics toggle explicitly. */
    fun setHinglish(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setHinglish(enabled) }
    }

    /** Moves a queue item, mirroring the controller queue. */
    fun moveQueueItem(from: Int, to: Int) {
        val current = mutableQueue.value.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        mutableQueue.value = current
        viewModelScope.launch { connection.moveItem(from, to) }
    }

    /** Removes a queue item. */
    fun removeQueueItem(index: Int) {
        val current = mutableQueue.value.toMutableList()
        if (index !in current.indices) return
        current.removeAt(index)
        mutableQueue.value = current
        viewModelScope.launch { connection.removeItem(index) }
    }

    /** Clears the queue everywhere. */
    fun clearQueue() {
        mutableQueue.value = emptyList()
        viewModelScope.launch { connection.clearQueue() }
    }

    /** Appends a song to the queue (add-to-queue action). Stream-less songs are skipped. */
    fun appendSong(song: Song) {
        if (song.id.isBlank() || song.streams.none { it.url.isNotBlank() }) return
        mutableQueue.value = mutableQueue.value + song
        viewModelScope.launch { connection.enqueueSong(song) }
    }

    /** Shares a song link via the system sheet. */
    fun shareSong(song: Song) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, song.name)
            putExtra(Intent.EXTRA_TEXT, "${song.name} by ${song.artist}\nwaveyy://song/${song.id}")
        }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Saves the best stream to Music/Waveyy via MediaStore (replaces the
     * web's blob download). Returns the saved URI, or null on failure.
     */
    suspend fun downloadSong(song: Song): Uri? {
        val url = com.waveyy.app.util.AudioQualitySelector.selectBestUrl(song.streams)
        if (url.isBlank()) return null
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "${song.name}.mp3")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Waveyy")
                    put(MediaStore.Audio.Media.ARTIST, song.artist)
                }
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching null
                URL(url).openStream().use { input ->
                    resolver.openOutputStream(uri)?.use { output -> input.copyTo(output) }
                }
                uri
            }.getOrNull()
        }
        withContext(Dispatchers.Main) {
            val msg = if (result != null) {
                "Downloaded: ${song.name}"
            } else {
                "Download failed: ${song.name}"
            }
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
        return result
    }

    companion object {
        private const val HISTORY_CAP = 50
        private const val PLAY_LOG_CAP = 300

        /** Keywords that flag a track as an unwanted variant. */
        val COVER_KEYWORDS = listOf(
            "karaoke",
            "cover by",
            "cover version",
            "instrumental",
            "8-bit",
            "8 bit",
            "made famous by",
            "in the style of",
            "tribute",
            "backing track",
            "ringtone",
            "slowed",
            "reverb",
            "sped up",
            "nightcore"
        )

        /** Max radio tracks appended per song. */
        const val RADIO_APPEND_LIMIT = 15

        /** Trigger radio fetch when the queue has this many tracks left
         *  after the current one. */
        private const val RADIO_TRIGGER_AT_REMAINING = 3

        /** Minimum saved position for a resume checkpoint. */
        private const val RESUME_MIN_POSITION_MS = 5_000L

        /** Stream freshness TTL (re-resolve URLs older than this). */
        private const val STREAM_TTL_MS = 30L * 60L * 1000L

        /** Snackbar text when nothing tapped is playable. */
        const val UNPLAYABLE_MESSAGE = "Unable to play this song. Try again."
    }
}
