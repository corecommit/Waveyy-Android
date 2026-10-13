package com.waveyy.app.data.lyrics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QQ Music: search then lyric fetch, no key.
 * Search: `c.y.qq.com/soso/fcgi-bin/client_search_cp` (JSON).
 * Lyric: `c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg` with
 * `nobase64=1` (plain text) falling back to base64 decode.
 */
@Singleton
class QqMusicProvider @Inject constructor(
    private val http: ProviderHttp,
    private val json: Json
) : LyricsProvider {
    override val name: String = "qqmusic"
    override val enabled: Boolean = true

    override suspend fun fetchLyrics(artist: String, title: String, duration: Long): LyricsResult? {
        if (artist.isBlank() || title.isBlank()) return null
        return runCatching {
            val mid = searchMid("$artist $title", title, artist) ?: return null
            fetchLyric(mid)
        }.getOrNull()
    }

    /**
     * Searches QQ Music and returns the songmid of the first result whose
     * (songname, singer) plausibly matches the query. Without this the
     * first hit wins regardless of title — wrong-lyrics bug.
     */
    private suspend fun searchMid(query: String, title: String, artist: String): String? {
        val url = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp" +
            "?w=${encode(query)}&format=json&p=1&n=5"
        val body = fetchBody(http.client, http.get(url, "https://y.qq.com/")) ?: return null
        val list = json.parseToJsonElement(body).jsonObject["data"]
            ?.jsonObject?.get("song")?.jsonObject?.get("list")?.jsonArray
            ?: return null

        for (element in list) {
            val obj = element.jsonObject
            val mid = obj["songmid"]?.jsonPrimitive?.content
                ?.takeIf { it.isNotBlank() } ?: continue
            val songName = obj["songname"]?.jsonPrimitive?.contentOrNull
                ?: obj["title"]?.jsonPrimitive?.contentOrNull
            val singer = obj["singer"]?.jsonArray
                ?.mapNotNull { (it as? kotlinx.serialization.json.JsonObject)
                    ?.get("name")?.jsonPrimitive?.contentOrNull }
                ?.joinToString(", ")
            if (LyricsMatcher.isMatch(songName, singer, title, artist)) {
                return mid
            }
            Timber.tag("QQMusic").d(
                "rejected mid=%s name='%s' artist='%s' (query: '%s' by '%s')",
                mid, songName, singer, title, artist
            )
        }
        return null
    }

    private suspend fun fetchLyric(mid: String): LyricsResult? {
        val url = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg" +
            "?songmid=$mid&format=json&nobase64=1&g_tk=5381"
        val body = fetchBody(http.client, http.get(url, "https://y.qq.com/")) ?: return null
        val obj = json.parseToJsonElement(body).jsonObject
        if (obj["retcode"]?.jsonPrimitive?.intOrNull != 0) return null
        var lyric = obj["lyric"]?.jsonPrimitive?.content.orEmpty()
        if (lyric.isBlank()) return null
        // Without nobase64 support the payload arrives base64-encoded.
        if (!lyric.contains("[") && !lyric.contains("\n")) {
            lyric = runCatching {
                String(java.util.Base64.getDecoder().decode(lyric), Charsets.UTF_8)
            }.getOrDefault(lyric)
        }
        lyric = lyric.trim()
        if (lyric.isBlank()) return null
        return if (TIMESTAMP.containsMatchIn(lyric)) LyricsResult.Synced(lyric) else LyricsResult.Plain(lyric)
    }

    companion object {
        private val TIMESTAMP = Regex("""\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?\]""")
    }
}
