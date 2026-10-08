package com.waveyy.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    private val lyricsProviderKey = stringPreferencesKey(LYRICS_PROVIDER_KEY)
    private val hinglishKey = stringPreferencesKey(HINGLISH_KEY)

    /** Audio quality preference as a flow. */
    fun audioQualityFlow(): Flow<String> {
        return context.userPrefs.data.map { it[audioQualityKey] ?: DEFAULT_AUDIO_QUALITY }
    }

    /** Persists the audio quality choice. */
    suspend fun setAudioQuality(option: String) {
        context.userPrefs.edit { prefs -> prefs[audioQualityKey] = option }
    }
}
