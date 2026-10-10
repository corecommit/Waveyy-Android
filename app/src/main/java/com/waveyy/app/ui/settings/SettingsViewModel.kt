package com.waveyy.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.BuildConfig
import com.waveyy.app.data.local.HistoryEntity
import com.waveyy.app.data.local.dao.HistoryDao
import com.waveyy.app.data.local.datastore.SearchHistoryStore
import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.data.lyrics.LyricsRaceRepository
import com.waveyy.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Audio quality options (best = highest-ranked stream). */
val AUDIO_QUALITY_OPTIONS = listOf("Best", "High (320kbps)", "Medium (160kbps)", "Low (96kbps)")

/** Lyrics provider options (reliable mode), plus disabled. */
val LYRICS_PROVIDER_OPTIONS = listOf(
    "lrclib", "netease", "qqmusic", "lyricsovh", "tekstowo", "musixmatch", "genius", "none"
)

/** Settings backing state: audio quality preference, version, sign-out. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
    private val auth: AuthRepository,
    private val lyricsRace: LyricsRaceRepository,
    private val searchHistory: SearchHistoryStore,
    private val historyDao: HistoryDao
) : ViewModel() {

    /** Persisted audio quality label. */
    val audioQuality = userPrefs.audioQualityFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), AUDIO_QUALITY_OPTIONS.first())

    /** App version name. */
    val versionName: String = BuildConfig.VERSION_NAME

    /** Current session (null for guests). */
    val session = auth.sessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    /** Persists the audio quality choice. */
    fun setAudioQuality(option: String) {
        viewModelScope.launch { userPrefs.setAudioQuality(option) }
    }

    /** Whether the primary catalog (JioSaavn) is enabled. */
    val saavnEnabled = userPrefs.saavnEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    /** Toggles the primary catalog. */
    fun setSaavnEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setSaavnEnabled(enabled) }
    }

    /** Whether to prefer music videos over audio tracks (YT Music only). */
    val preferVideo = userPrefs.preferVideoFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    /** Toggles prefer-music-videos. */
    fun setPreferVideo(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setPreferVideo(enabled) }
    }

    /** Whether YouTube Music source is enabled. */
    val ytmusicEnabled = userPrefs.ytmusicEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    /** Toggles the YouTube Music source. */
    fun setYtmusicEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setYtmusicEnabled(enabled) }
    }

    /** Whether haptic feedback is enabled (default on). */
    val hapticsEnabled = userPrefs.hapticsEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    /** Persists the haptics toggle. */
    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setHapticsEnabled(enabled) }
    }

    /** Whether searches return the first source instead of merging. */
    val preferFast = userPrefs.preferFastFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    /** Toggles fast search mode. */
    fun setPreferFast(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setPreferFast(enabled) }
    }

    /** Lyrics loading strategy (`auto` = race; `preferred` = single source). */
    val lyricsMode = userPrefs.lyricsModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UserPrefs.LYRICS_MODE_AUTO)

    /** Persists the lyrics loading strategy. */
    fun setLyricsMode(mode: String) {
        viewModelScope.launch { userPrefs.setLyricsMode(mode) }
    }

    /** Preferred lyrics provider (reliable mode only). */
    val preferredLyricsProvider = userPrefs.lyricsProviderFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UserPrefs.DEFAULT_LYRICS_PROVIDER)

    /** Persists the lyrics source. */
    fun setPreferredLyricsProvider(provider: String) {
        viewModelScope.launch { userPrefs.setLyricsProvider(provider) }
    }

    /** Clears the lyrics race cache. */
    fun clearLyricsCache() {
        viewModelScope.launch { lyricsRace.clearCache() }
    }

    /** Whether to stop playback when the app is task-removed. */
    val stopMusicOnClose = userPrefs.stopMusicOnCloseFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    /** Toggles stop-on-close. */
    fun setStopMusicOnClose(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setStopMusicOnClose(enabled) }
    }

    /** Signs out. */
    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    // ─── Library cleanup (undoable) ───

    /**
     * Captures the current search history and clears it. Returns the
     * captured list so the caller can offer an Undo.
     */
    suspend fun captureAndClearSearchHistory(): List<String> {
        val previous = searchHistory.history.first()
        searchHistory.clear()
        return previous
    }

    /** Restores a previously captured search history list. */
    suspend fun restoreSearchHistory(previous: List<String>) {
        searchHistory.restore(previous)
    }

    /**
     * Captures the current listening history and clears it. Returns the
     * captured rows so the caller can offer an Undo.
     */
    suspend fun captureAndClearListeningHistory(): List<HistoryEntity> {
        val previous = historyDao.all()
        historyDao.clearAll()
        return previous
    }

    /** Restores a previously captured listening history. */
    suspend fun restoreListeningHistory(previous: List<HistoryEntity>) {
        previous.forEach { historyDao.upsert(it) }
    }
}
