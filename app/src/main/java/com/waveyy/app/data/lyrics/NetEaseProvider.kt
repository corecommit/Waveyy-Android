package com.waveyy.app.data.lyrics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.FormBody
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NetEase Cloud Music: search then lyric fetch, no key.
 * Search: `POST music.163.com/api/search/pc` (`{result:{songs:[{id,...}]}}`).
 * Lyric: `GET music.163.com/api/song/lyric?id=..&lv=-1` (`{lrc:{lyric}}`).
 */
@Singleton
class NetEaseProvider @Inject constructor(
    private val http: ProviderHttp,
    private val json: Json
) : LyricsProvider {
    override val name: String = "netease"
    override val enabled: Boolean = true

    override suspend fun fetchLyrics(artist: String, title: String, duration: Long): LyricsResult? {
        if (artist.isBlank() || title.isBlank()) return null
        return runCatching {
            val songId = searchId("$artist $title", title, artist) ?: return null
            fetchLyric(songId)
        }.getOrNull()
    }

    /**
     * Searches NetEase and returns the id of the first result whose
     * (name, artist) plausibly matches the query. Without this check the
     * old "first hit wins" pattern returned the wrong song whenever the
     * artist matched but the title didn't.
     */
    private suspend fun searchId(query: String, title: String, artist: String): Long? {
        val form = FormBody.Builder()
            .add("s", query)
            .add("type", "1")
            .add("limit", "5")
            .add("offset", "0")
            .build()
        val request = Request.Builder()
            .url("https://music.163.com/api/search/pc")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Waveyy/1.0")
            .header("Referer", "https://music.163.com/")
            .post(form)
            .build()
        val body = fetchBody(http.client, request) ?: return null
        val songs = json.parseToJsonElement(body).jsonObject["result"]
            ?.jsonObject?.get("songs")?.jsonArray ?: return null

        for (element in songs) {
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.longOrNull?.takeIf { it > 0 } ?: continue
            val name = obj["name"]?.jsonPrimitive?.contentOrNull
            val hitArtist = obj["artists"]?.jsonArray
                ?.mapNotNull { (it as? kotlinx.serialization.json.JsonObject)
                    ?.get("name")?.jsonPrimitive?.contentOrNull }
                ?.joinToString(", ")
            if (LyricsMatcher.isMatch(name, hitArtist, title, artist)) {
                return id
            }
            Timber.tag("NetEase").d(
                "rejected hit id=%d name='%s' artist='%s' (query: '%s' by '%s')",
                id, name, hitArtist, title, artist
            )
        }
        return null
    }

    private suspend fun fetchLyric(songId: Long): LyricsResult? {
        val url = "https://music.163.com/api/song/lyric?id=$songId&lv=-1&tv=-1"
        val body = fetchBody(http.client, http.get(url, "https://music.163.com/")) ?: return null
        val obj = json.parseToJsonElement(body).jsonObject
        // Absent-lyrics markers: never claim instrumental from here.
        if (obj["nolyric"]?.jsonPrimitive?.intOrNull == 1) return null
        val lrc = obj["lrc"]?.jsonObject?.get("lyric")?.jsonPrimitive?.content.orEmpty()
        if (lrc.isBlank()) return null
        return if (TIMESTAMP.containsMatchIn(lrc)) LyricsResult.Synced(lrc) else LyricsResult.Plain(lrc)
    }

    companion object {
        private val TIMESTAMP = Regex("""\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?\]""")
    }
}
