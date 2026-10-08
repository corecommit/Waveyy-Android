package com.waveyy.app.data.lyrics

import com.waveyy.app.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Musixmatch (`matcher.lyrics.get`): plain, needs `MUSIXMATCH_API_KEY`.
 * Self-disables when the key is blank (no crash, no prompt).
 */
@Singleton
class MusixmatchProvider @Inject constructor(
    private val http: ProviderHttp,
    private val json: Json
) : LyricsProvider {
    override val name: String = "musixmatch"
    override val enabled: Boolean = BuildConfig.MUSIXMATCH_API_KEY.isNotBlank()

    override suspend fun fetchLyrics(artist: String, title: String, duration: Long): LyricsResult? {
        if (!enabled || artist.isBlank() || title.isBlank()) return null
        return runCatching {
            val url = "https://api.musixmatch.com/ws/1.1/matcher.lyrics.get" +
                "?q_track=${encode(title)}&q_artist=${encode(artist)}" +
                "&apikey=${encode(BuildConfig.MUSIXMATCH_API_KEY)}"
            val body = fetchBody(http.client, http.get(url)) ?: return null
            val header = json.parseToJsonElement(body).jsonObject["message"]
                ?.jsonObject?.get("header")?.jsonObject
            if (header?.get("status_code")?.jsonPrimitive?.intOrNull != 200) return null
            val lyrics = json.parseToJsonElement(body).jsonObject["message"]
                ?.jsonObject?.get("body")?.jsonObject?.get("lyrics")?.jsonObject
                ?: return null
            if (lyrics["instrumental"]?.jsonPrimitive?.intOrNull == 1) {
                return LyricsResult.Instrumental
            }
            val text = lyrics["lyrics_body"]?.jsonPrimitive?.content.orEmpty()
                .replace(COPYRIGHT_TRAILER, "")
                .trim()
            if (text.isBlank()) null else LyricsResult.Plain(text)
        }.getOrNull()
    }

    companion object {
        private val COPYRIGHT_TRAILER = Regex("""\*{3}.*This Lyrics.*$""", RegexOption.DOT_MATCHES_ALL)
    }
}
