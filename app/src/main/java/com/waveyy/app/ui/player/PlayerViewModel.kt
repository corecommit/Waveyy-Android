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
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.ListeningHistoryRepository
import com.waveyy.app.data.repository.LyricsRepository
import com.waveyy.app.domain.model.CachedLyrics
import com.waveyy.app.domain.model.Song
import com.waveyy.app.playback.PlaybackConnection
import com.waveyy.app.util.LyricsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

/** Lyrics panel state. */
sealed interface LyricsUiState {
    /** Nothing loaded yet. */
    data object Empty : LyricsUiState

    /** Loading. */
    data object Loading : LyricsUiState

    /** Parsed lines (raw lyrics, transliteration applied at render). */
    data class Ready(val lines: List<com.waveyy.app.domain.model.LyricLine>) : LyricsUiState
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
    private val authRepository: AuthRepository
) : ViewModel() {

    private val mutableQueue = MutableStateFlow<List<Song>>(emptyList())
    /** Current queue. */
    val queue: StateFlow<List<Song>> = mutableQueue.asStateFlow()

    private val mutableLyrics = MutableStateFlow<LyricsUiState>(LyricsUiState.Empty)
    /** Lyrics panel state. */
    val lyrics: StateFlow<LyricsUiState> = mutableLyrics.asStateFlow()

    private val mutableHinglish = MutableStateFlow(false)
    /** Romanized-lyrics toggle. */
    val hinglishEnabled: StateFlow<Boolean> = mutableHinglish.asStateFlow()

    private var lastLoggedId: String? = null

    /**
     * Replaces the queue and starts playback, then records history.
     * Mirrors tapping a song anywhere in the web app.
     */
    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        mutableQueue.value = songs
        viewModelScope.launch {
            connection.playSongs(songs, startIndex)
            onTrackChanged(songs.getOrNull(startIndex)?.id)
        }
    }

    /** Plays a single song (queue of one). */
    fun playSong(song: Song) {
        playSongs(listOf(song), 0)
    }

    /** Records history + listening event on track change (fire-and-forget). */
    fun onTrackChanged(songId: String?) {
        if (songId.isNullOrBlank() || songId == lastLoggedId) return
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

    /** Loads lyrics for [song] with cache-then-fetch. */
    fun loadLyrics(song: Song) {
        mutableLyrics.value = LyricsUiState.Loading
        viewModelScope.launch {
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

    /** Toggles romanized lyrics. */
    fun toggleHinglish() {
        mutableHinglish.value = !mutableHinglish.value
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

    /** Appends a song to the queue (add-to-queue action). */
    fun appendSong(song: Song) {
        if (song.id.isBlank()) return
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
    }
}
