package com.waveyy.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.waveyy.app.domain.model.Playlist
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefs by preferencesDataStore(name = "user_prefs")

/**
 * Typed DataStore access. Holds the `local-username-map` (username to email)
 * from `auth-provider.jsx`, used for username login resolution.
 */
@Singleton
class UserPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usernameMapKey = stringPreferencesKey(USERNAME_MAP_KEY)
    private val audioQualityKey = stringPreferencesKey(AUDIO_QUALITY_KEY)

    /** Returns the stored username-to-email map (empty when absent). */
    suspend fun usernameMap(): Map<String, String> {
        val raw = context.userPrefs.data.map { it[usernameMapKey] }.first() ?: return emptyMap()
        return runCatching {
            val obj = Json.parseToJsonElement(raw) as? JsonObject ?: return emptyMap()
            obj.entries.associate { (key, value) -> key to (value as? JsonPrimitive)?.content.orEmpty() }
        }.getOrDefault(emptyMap())
    }

    /** Stores a username-to-email association. */
    suspend fun putUsername(username: String, email: String) {
        val next = buildJsonObject {
            usernameMap().forEach { (key, value) -> put(key, JsonPrimitive(value)) }
            put(username, JsonPrimitive(email))
        }.toString()
        context.userPrefs.edit { prefs -> prefs[usernameMapKey] = next }
    }

    companion object {
        /** Storage key, identical to the web client's localStorage key. */
        const val USERNAME_MAP_KEY = "local-username-map"

        /** Audio quality preference key. */
        const val AUDIO_QUALITY_KEY = "audio-quality"

        /** Default audio quality label. */
        const val DEFAULT_AUDIO_QUALITY = "Best"

        /** Lyrics provider preference key (`lrclib` or `none`). */
        const val LYRICS_PROVIDER_KEY = "lyrics-provider"

        /** Default lyrics provider. */
        const val DEFAULT_LYRICS_PROVIDER = "lrclib"

        /** Hinglish romanization toggle key. */
        const val HINGLISH_KEY = "hinglish-enabled"

        /** InnerTube visitor token key. */
        const val VISITOR_DATA_KEY = "yt-visitor-data"

        /** YouTube Music source toggle key. */
        const val YTMUSIC_ENABLED_KEY = "ytmusic-enabled"

        /** Race-mode toggle key (fast-first vs complete). */
        const val PREFER_FAST_KEY = "prefer-fast"

        /** Saavn source toggle key (default enabled). */
        const val SAAVN_ENABLED_KEY = "saavn-enabled"

        /** Prefer video over music (YT Music only). */
        const val PREFER_VIDEO_KEY = "prefer-video"

        /** Kill music when the app is closed. */
        const val STOP_MUSIC_ON_CLOSE_KEY = "stop-music-on-close"

        /** Guest playlist blob (JSON-encoded list of minimal playlist rows). */
        const val GUEST_PLAYLISTS_KEY = "guest-playlists"

        /** Guest liked-song ids key (JSON string set, works without a session). */
        const val GUEST_LIKED_IDS_KEY = "guest-liked-ids"

        /** Mini-player hidden-after-swipe-down key. */
        const val MINI_PLAYER_HIDDEN_KEY = "mini-player-hidden"

        /** Lyrics race-mode key (`auto` or `preferred`). */
        const val LYRICS_MODE_KEY = "lyrics-race-mode"

        /** Lyrics race-mode `auto` value (race all providers). */
        const val LYRICS_MODE_AUTO = "auto"

        /** Lyrics race-mode `preferred` value (single provider). */
        const val LYRICS_MODE_PREFERRED = "preferred"

        /** Lyrics race-winner debug caption toggle key. */
        const val LYRICS_DEBUG_KEY = "lyrics-debug"

        /** Last-played song id (restored on app reopen). */
        const val LAST_PLAYED_SONG_ID_KEY = "last-played-song-id"

        /** Last-played position in millis. */
        const val LAST_PLAYED_POSITION_MS_KEY = "last-played-position-ms"

        /** Tag of the newest release the user has already seen (popup gate). */
        const val LAST_SEEN_UPDATE_TAG_KEY = "last-seen-update-tag"

        /** Epoch millis of the last successful update check. */
        const val LAST_UPDATE_CHECK_MS_KEY = "last-update-check-ms"

        /** Haptic feedback enabled. */
        const val HAPTICS_ENABLED_KEY = "haptics-enabled"
    }

    /** Lyrics provider id (`lrclib` or `none`). */
    fun lyricsProviderFlow(): Flow<String> {
        return context.userPrefs.data.map { it[lyricsProviderKey] ?: DEFAULT_LYRICS_PROVIDER }
    }

    /** Persists the lyrics provider choice. */
    suspend fun setLyricsProvider(provider: String) {
        context.userPrefs.edit { prefs -> prefs[lyricsProviderKey] = provider }
    }

    /** Hinglish romanization toggle. */
    fun hinglishFlow(): Flow<Boolean> {
        return context.userPrefs.data.map { it[hinglishKey] ?: false }
    }

    /** Persists the Hinglish toggle. */
    suspend fun setHinglish(enabled: Boolean) {
        context.userPrefs.edit { prefs -> prefs[hinglishKey] = enabled }
    }

    /** InnerTube visitor token (rotated by the client on receipt). */
    fun ytVisitorFlow(): Flow<String?> {
        return context.userPrefs.data.map { it[ytVisitorKey] }
    }

    /** Reads the stored visitor token (null when never seen). */
    suspend fun ytVisitor(): String? {
        return context.userPrefs.data.map { it[ytVisitorKey] }.first()
    }

    /** Persists a refreshed visitor token. */
    suspend fun setYtVisitor(token: String) {
        context.userPrefs.edit { prefs -> prefs[ytVisitorKey] = token }
    }

    /** Whether YouTube Music source is enabled (default on). */
    fun ytmusicEnabledFlow(): Flow<Boolean> {
        return context.userPrefs.data.map { it[ytmusicEnabledKey] ?: true }
    }

    /** Persists the YouTube Music toggle. */
    suspend fun setYtmusicEnabled(enabled: Boolean) {
        context.userPrefs.edit { prefs -> prefs[ytmusicEnabledKey] = enabled }
    }

    /** Whether races resolve fast-first (default on) instead of complete. */
    fun preferFastFlow(): Flow<Boolean> {
        return context.userPrefs.data.map { it[preferFastKey] ?: true }
    }

    /** Persists the race-mode toggle. */
    suspend fun setPreferFast(enabled: Boolean) {
        context.userPrefs.edit { prefs -> prefs[preferFastKey] = enabled }
    }

    /** Whether JioSaavn source is enabled (default on). */
    fun saavnEnabledFlow(): Flow<Boolean> {
        return context.userPrefs.data.map { it[saavnEnabledKey] ?: true }
    }

    /** Persists the JioSaavn toggle. */
    suspend fun setSaavnEnabled(enabled: Boolean) {
        context.userPrefs.edit { prefs -> prefs[saavnEnabledKey] = enabled }
    }

    /** Whether to prefer video results (YT Music only). */
    fun preferVideoFlow(): Flow<Boolean> {
        return context.userPrefs.data.map { it[preferVideoKey] ?: false }
    }

    /** Persists the prefer-video toggle. */
    suspend fun setPreferVideo(enabled: Boolean) {
        context.userPrefs.edit { prefs -> prefs[preferVideoKey] = enabled }
    }

    /** Whether to stop playback when the app is task-removed. */
    fun stopMusicOnCloseFlow(): Flow<Boolean> {
        return context.userPrefs.data.map { it[stopMusicOnCloseKey] ?: false }
    }

    /** Persists the stop-on-close toggle. */
    suspend fun setStopMusicOnClose(enabled: Boolean) {
        context.userPrefs.edit { prefs -> prefs[stopMusicOnCloseKey] = enabled }
    }

    /** Guest playlists (empty for signed-in users or never-created). */
    fun guestPlaylistsFlow(): Flow<List<Playlist>> {
        return context.userPrefs.data.map { prefs ->
            val raw = prefs[guestPlaylistsKey] ?: return@map emptyList()
            runCatching { Json.decodeFromString<List<Playlist>>(raw) }.getOrDefault(emptyList())
        }
    }

    /** Reads guest playlists synchronously. */
    suspend fun guestPlaylists(): List<Playlist> {
        val raw = context.userPrefs.data.map { it[guestPlaylistsKey] }.first() ?: return emptyList()
        return runCatching { Json.decodeFromString<List<Playlist>>(raw) }.getOrDefault(emptyList())
    }

    /** Persists the guest playlists list. */
    suspend fun setGuestPlaylists(playlists: List<Playlist>) {
        val raw = Json.encodeToString(playlists)
        context.userPrefs.edit { prefs -> prefs[guestPlaylistsKey] = raw }
    }

    /** Guest liked-song ids (empty when never liked). */
    fun guestLikedIdsFlow(): Flow<Set<String>> {
        return context.userPrefs.data.map { prefs ->
            val raw = prefs[guestLikedIdsKey] ?: return@map emptySet()
            runCatching { Json.decodeFromString<Set<String>>(raw) }.getOrDefault(emptySet())
        }
    }

    /** Persists the guest liked-song ids. */
    suspend fun setGuestLikedIds(ids: Set<String>) {
        val raw = Json.encodeToString(ids)
        context.userPrefs.edit { prefs -> prefs[guestLikedIdsKey] = raw }
    }

    /** Whether the mini-player was swiped away (reset on new song start). */
    fun miniPlayerHiddenFlow(): Flow<Boolean> {
        return context.userPrefs.data.map { it[miniPlayerHiddenKey] ?: false }
    }

    /** Persists the mini-player hidden flag. */
    suspend fun setMiniPlayerHidden(hidden: Boolean) {
        context.userPrefs.edit { prefs -> prefs[miniPlayerHiddenKey] = hidden }
    }

    /** Lyrics race mode (`auto` default, or `preferred`). */
    fun lyricsModeFlow(): Flow<String> {
        return context.userPrefs.data.map { it[lyricsModeKey] ?: LYRICS_MODE_AUTO }
    }

    /** Persists the lyrics race mode. */
    suspend fun setLyricsMode(mode: String) {
        context.userPrefs.edit { prefs -> prefs[lyricsModeKey] = mode }
    }

    /** Whether to show the race-winner caption (debug builds only). */
    fun lyricsDebugFlow(): Flow<Boolean> {
        return context.userPrefs.data.map { it[lyricsDebugKey] ?: false }
    }

    /** Persists the race-winner debug toggle. */
    suspend fun setLyricsDebug(enabled: Boolean) {
        context.userPrefs.edit { prefs -> prefs[lyricsDebugKey] = enabled }
    }

    private val lyricsProviderKey = stringPreferencesKey(LYRICS_PROVIDER_KEY)
    private val hinglishKey = booleanPreferencesKey(HINGLISH_KEY)
    private val ytVisitorKey = stringPreferencesKey(VISITOR_DATA_KEY)
    private val ytmusicEnabledKey = booleanPreferencesKey(YTMUSIC_ENABLED_KEY)
    private val preferFastKey = booleanPreferencesKey(PREFER_FAST_KEY)
    private val saavnEnabledKey = booleanPreferencesKey(SAAVN_ENABLED_KEY)
    private val preferVideoKey = booleanPreferencesKey(PREFER_VIDEO_KEY)
    private val stopMusicOnCloseKey = booleanPreferencesKey(STOP_MUSIC_ON_CLOSE_KEY)
    private val guestPlaylistsKey = stringPreferencesKey(GUEST_PLAYLISTS_KEY)
    private val guestLikedIdsKey = stringPreferencesKey(GUEST_LIKED_IDS_KEY)
    private val miniPlayerHiddenKey = booleanPreferencesKey(MINI_PLAYER_HIDDEN_KEY)
    private val lyricsModeKey = stringPreferencesKey(LYRICS_MODE_KEY)
    private val lyricsDebugKey = booleanPreferencesKey(LYRICS_DEBUG_KEY)
    private val lastPlayedSongIdKey = stringPreferencesKey(LAST_PLAYED_SONG_ID_KEY)
    private val lastPlayedPositionMsKey = longPreferencesKey(LAST_PLAYED_POSITION_MS_KEY)
    private val lastSeenUpdateTagKey = stringPreferencesKey(LAST_SEEN_UPDATE_TAG_KEY)
    private val lastUpdateCheckMsKey = longPreferencesKey(LAST_UPDATE_CHECK_MS_KEY)
    private val hapticsEnabledKey = booleanPreferencesKey(HAPTICS_ENABLED_KEY)

    /** Audio quality preference as a flow. */
    fun audioQualityFlow(): Flow<String> {
        return context.userPrefs.data.map { it[audioQualityKey] ?: DEFAULT_AUDIO_QUALITY }
    }

    /** Persists the audio quality choice. */
    suspend fun setAudioQuality(option: String) {
        context.userPrefs.edit { prefs -> prefs[audioQualityKey] = option }
    }

    /** Persists the currently playing song id + position. */
    suspend fun setLastPlayed(songId: String, positionMs: Long) {
        context.userPrefs.edit { prefs ->
            prefs[lastPlayedSongIdKey] = songId
            prefs[lastPlayedPositionMsKey] = positionMs
        }
    }

    /** Reads the stored last-played pair, or null when never written. */
    suspend fun lastPlayed(): Pair<String, Long>? {
        val prefs = context.userPrefs.data.first()
        val id = prefs[lastPlayedSongIdKey] ?: return null
        val pos = prefs[lastPlayedPositionMsKey] ?: 0L
        return id to pos
    }

    /** Whether haptic feedback is enabled (default on). */
    fun hapticsEnabledFlow(): Flow<Boolean> {
        return context.userPrefs.data.map { it[hapticsEnabledKey] ?: true }
    }

    /** Persists the haptics toggle. */
    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.userPrefs.edit { prefs -> prefs[hapticsEnabledKey] = enabled }
    }

    /** Tag of the newest release already shown to the user (dismissal gate). */
    suspend fun lastSeenUpdateTag(): String? {
        return context.userPrefs.data.map { it[lastSeenUpdateTagKey] }.first()
    }

    /** Records the tag of a release whose update popup has been dismissed. */
    suspend fun setLastSeenUpdateTag(tag: String) {
        context.userPrefs.edit { prefs -> prefs[lastSeenUpdateTagKey] = tag }
    }

    /** Epoch millis of the last successful check (0 when never). */
    suspend fun lastUpdateCheckMs(): Long {
        return context.userPrefs.data.map { it[lastUpdateCheckMsKey] ?: 0L }.first()
    }

    /** Records when the last successful check happened. */
    suspend fun setLastUpdateCheckMs(ms: Long) {
        context.userPrefs.edit { prefs -> prefs[lastUpdateCheckMsKey] = ms }
    }
}
