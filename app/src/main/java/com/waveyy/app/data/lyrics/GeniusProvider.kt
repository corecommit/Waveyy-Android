package com.waveyy.app.data.lyrics

import com.waveyy.app.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Genius: search API then song-page scrape, needs `GENIUS_API_KEY`
 * (Bearer token). Self-disables when the key is blank.
 * Lyrics live in `<div data-lyrics-container="true">` blocks.
 */
@Singleton
class GeniusProvider @Inject constructor(
    private val http: ProviderHttp,
    private val json: Json
) : LyricsProvider {
    override val name: String = "genius"
    override val enabled: Boolean = BuildConfig.GENIUS_API_KEY.isNotBlank()

    override suspend fun fetchLyrics(artist: String, title: String, duration: Long): LyricsResult? {
        if (!enabled || artist.isBlank() || title.isBlank()) return null
        return runCatching {
            val url = searchUrl("$artist $title") ?: return null
            scrape(url)
        }.getOrNull()
    }

    private suspend fun searchUrl(query: String): String? {
        val request = Request.Builder()
            .url("https://api.genius.com/search?q=${encode(query)}")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Waveyy/1.0")
            .header("Authorization", "Bearer ${BuildConfig.GENIUS_API_KEY}")
            .build()
        val body = fetchBody(http.client, request) ?: return null
        return json.parseToJsonElement(body).jsonObject["response"]
            ?.jsonObject?.get("hits")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("result")?.jsonObject?.get("url")
            ?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
    }

    private suspend fun scrape(songUrl: String): LyricsResult? {
        val page = fetchBody(http.client, http.get(songUrl)) ?: return null
        val blocks = CONTAINER.findAll(page).map { it.groupValues[1] }.toList()
        if (blocks.isEmpty()) return null
        val text = blocks.joinToString("\n") { block ->
            block.replace(BR, "\n").replace(TAG, "")
        }.replace("&amp;", "&")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("[") }
            .joinToString("\n")
            .trim()
        if (text.length < MIN_CHARS) return null
        return LyricsResult.Plain(text)
    }

    companion object {
        private val CONTAINER = Regex(
            """<div[^>]*data-lyrics-container="true"[^>]*>([\s\S]*?)</div>"""
        )
        private val BR = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
        private val TAG = Regex("""<[^>]+>""")
        private const val MIN_CHARS = 200
    }
}
