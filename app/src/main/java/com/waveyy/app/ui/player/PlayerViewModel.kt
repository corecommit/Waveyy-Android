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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var sleepJob: Job? = null
    private var sleepEndOfSong = false

    init {
        // Reconnects the UI to an already-playing session (swipe-away
        // reopen): the activity-scoped queue is gone but the singleton
        // controller still holds the items, so rebuild from its metadata.
        viewModelScope.launch {
            connection.refreshState()
            connection.state.collect { state ->
                if (state.mediaId != null && mutableQueue.value.isEmpty()) {
                    val restored = connection.currentQueueSongs()
                    if (restored.isNotEmpty()) mutableQueue.value = restored
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

    /** Race-winner caption toggle (debug builds only). */
    val lyricsDebug: StateFlow<Boolean> = userPrefs.lyricsDebugFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    private val mutableWinner = MutableStateFlow<LyricsRaceOrchestrator.Scored?>(null)
    /** Latest race winner (provider + score, for the debug caption). */
    val raceWinner: StateFlow<LyricsRaceOrchestrator.Scored?> = mutableWinner.asStateFlow()

    private val mutablePlayerError = MutableStateFlow<String?>(null)
    /** Playback failure message for the Snackbar (null when clear). */
    val playerError: StateFlow<String?> = mutablePlayerError.asStateFlow()

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

            // Capture the ORIGINAL id BEFORE any refresh — this is the key to
            // filtering the tapped song out of the backfill list, immune to
            // refresh-id-drift.
            val originalTappedId = tappedSong.id

            // 1. Resolve the tapped song. Its id may or may not change.
            val resolvedTapped = if (tappedSong.streams.any { it.url.isNotBlank() }) {
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
            userPrefs.setMiniPlayerHidden(false)
            connection.playSingleItem(singleItem)
            onTrackChanged(resolvedTapped.id)
            val generation = connection.currentGeneration()

            // 3. Background: resolve the rest and append. Every filter here
            // compares against originalTappedId, not resolvedTapped.id, so the
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

                // Dedupe: exclude the original tapped id AND the resolved
                // tapped id (refresh may have produced a new one), plus any
                // repeat of an already-seen id.
                val seenIds = mutableSetOf<String>()
                seenIds.add(originalTappedId)
                seenIds.add(resolvedTapped.id)
                val extraSongs = resolvedOthers.filter { seenIds.add(it.id) }
                if (extraSongs.isEmpty()) return@launch

                // Guard: user must not have moved on. Also guards against a
                // second tap having started a new playSingleItem — in that
                // case we'd be appending to the wrong session.
                val stillOnTapped = connection.state.value.mediaId == resolvedTapped.id
                if (!stillOnTapped) {
                    Timber.tag("PlayerVM").d("skipping backfill: user moved on")
                    return@launch
                }
                if (connection.currentGeneration() != generation) {
                    Timber.tag("PlayerVM").d("skipping backfill: superseded by newer tap")
                    return@launch
                }

                // Convert and append. Media3's addMediaItems grows the queue
                // without touching the current item — no re-prepare, no
                // re-seek, no index desync.
                val items = com.waveyy.app.playback.QueueManager()
                    .toMediaItems(extraSongs)
                if (items.isEmpty()) return@launch

                connection.appendItems(items)
                mutableQueue.value = listOf(resolvedTapped) + extraSongs
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

    /** Advances to the next item, refreshing its stream URL first. */
    fun advance() {
        viewModelScope.launch {
            val current = mutableQueue.value
            val at = connection.state.value.mediaIndex
            val next = current.getOrNull(at + 1)
            if (next != null) {
                val fresh = ensureFreshStreams(listOf(next)).firstOrNull()
                if (fresh != null && fresh != next) {
                    mutableQueue.value = current.toMutableList().also { it[at + 1] = fresh }
                    connection.replaceItem(at + 1, fresh)
                }
            }
            connection.next()
        }
    }

    /** Returns to the previous item, refreshing its stream URL first. */
    fun rewind() {
        viewModelScope.launch {
            val current = mutableQueue.value
            val at = connection.state.value.mediaIndex
            val prev = current.getOrNull(at - 1)
            if (prev != null) {
                val fresh = ensureFreshStreams(listOf(prev)).firstOrNull()
                if (fresh != null && fresh != prev) {
                    mutableQueue.value = current.toMutableList().also { it[at - 1] = fresh }
                    connection.replaceItem(at - 1, fresh)
                }
            }
            connection.previous()
        }
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
        mutableLyrics.value = LyricsUiState.Loading
        mutableWinner.value = null
        viewModelScope.launch {
            val auto = userPrefs.lyricsModeFlow().first() != UserPrefs.LYRICS_MODE_PREFERRED
            val preferred = userPrefs.lyricsProviderFlow().first()
            if (!auto && preferred == "none") {
                mutableLyrics.value = LyricsUiState.Empty
                return@launch
            }
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
            val recs = runCatching {
                songRepository.suggestions(song.id).first()
            }.getOrDefault(emptyList())
            val queue = (listOf(song) + recs).distinctBy { it.id }
            if (queue.isNotEmpty()) {
                playSource(com.waveyy.app.playback.QueueSource.Recommendations(queue), 0)
            }
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
        return withContext(Dispatchers.IO) {
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
    }

    companion object {
        private const val HISTORY_CAP = 50
        private const val PLAY_LOG_CAP = 300

        /** Stream freshness TTL (re-resolve URLs older than this). */
        private const val STREAM_TTL_MS = 30L * 60L * 1000L

        /** Snackbar text when nothing tapped is playable. */
        const val UNPLAYABLE_MESSAGE = "Unable to play this song. Try again."
    }
}
