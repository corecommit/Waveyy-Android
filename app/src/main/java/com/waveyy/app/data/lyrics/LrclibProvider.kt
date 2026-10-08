package com.waveyy.app.data.lyrics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LRCLIB (`https://lrclib.net/api/get`): synced + plain, no key.
 * Response: `{syncedLyrics, plainLyrics, instrumental}` (404 when missing).
 */
@Singleton
class LrclibProvider @Inject constructor(
    private val http: ProviderHttp,
    private val json: Json
) : LyricsProvider {
    override val name: String = "lrclib"
    override val enabled: Boolean = true

    override suspend fun fetchLyrics(artist: String, title: String, duration: Long): LyricsResult? {
        if (artist.isBlank() || title.isBlank()) return null
        val url = buildString {
            append("https://lrclib.net/api/get?artist=${encode(artist)}&track=${encode(title)}")
            if (duration > 0) append("&duration=$duration")
        }
        val body = fetchBody(http.client, http.get(url)) ?: return null
        return runCatching {
            val obj = json.parseToJsonElement(body).jsonObject
            if (obj["instrumental"]?.jsonPrimitive?.booleanOrNull == true) {
                return LyricsResult.Instrumental
            }
            val synced = obj["syncedLyrics"]?.jsonPrimitive?.content.orEmpty()
            val plain = obj["plainLyrics"]?.jsonPrimitive?.content.orEmpty()
            when {
                synced.isNotBlank() -> LyricsResult.Synced(synced)
                plain.isNotBlank() -> LyricsResult.Plain(plain)
                else -> null
            }
        }.getOrNull()
    }
}
