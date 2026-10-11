package com.waveyy.app.data.lyrics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
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

    /**
     * Searches LRCLIB's phrase index. Returns raw matches as
     * [LyricsTextMatch] pairs of (title, artist, id) — the ID is
     * LRCLIB's own id, not a JioSaavn song id. Callers must resolve
     * back to JioSaavn by title+artist search.
     *
     * @param phrase free-text lyric fragment (2+ words recommended).
     * @return matches in LRCLIB's natural relevance order, up to 20.
     */
    suspend fun searchByLyrics(phrase: String): List<LyricsTextMatch> {
        val cleaned = phrase.trim().takeIf { it.length >= 6 } ?: return emptyList()
        val url = "https://lrclib.net/api/search?q=${encode(cleaned)}"
        val body = fetchBody(http.client, http.get(url)) ?: return emptyList()
        return runCatching {
            val arr = json.parseToJsonElement(body).jsonArray
            arr.mapNotNull { element ->
                val obj = element.jsonObject
                val title = obj["trackName"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val artist = obj["artistName"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() }.orEmpty()
                LyricsTextMatch(
                    title = title,
                    artist = artist,
                    lrclibId = obj["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )
            }.take(20)
        }.getOrNull().orEmpty()
    }
}

/** A lyrics-content match returned by LRCLIB's text-search endpoint. */
data class LyricsTextMatch(
    /** Track title as indexed by LRCLIB. */
    val title: String,
    /** Primary artist name. */
    val artist: String,
    /** LRCLIB's own row id (not a JioSaavn song id). */
    val lrclibId: String
)
