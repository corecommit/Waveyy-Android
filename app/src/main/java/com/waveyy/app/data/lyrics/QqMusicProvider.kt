package com.waveyy.app.data.lyrics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
            val mid = searchMid("$artist $title") ?: return null
            fetchLyric(mid)
        }.getOrNull()
    }

    private suspend fun searchMid(query: String): String? {
        val url = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp" +
            "?w=${encode(query)}&format=json&p=1&n=3"
        val body = fetchBody(http.client, http.get(url, "https://y.qq.com/")) ?: return null
        val list = json.parseToJsonElement(body).jsonObject["data"]
            ?.jsonObject?.get("song")?.jsonObject?.get("list")?.jsonArray
            ?: return null
        return list.firstOrNull()?.jsonObject?.get("songmid")
            ?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
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
        return if (TIMESTAMP.matcher(lyric).find()) LyricsResult.Synced(lyric) else LyricsResult.Plain(lyric)
    }

    companion object {
        private val TIMESTAMP = Regex("""\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?\]""")
    }
}
