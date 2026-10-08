package com.waveyy.app.data.lyrics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * lyrics.ovh (`https://api.lyrics.ovh/v1/{artist}/{title}`): plain, no key.
 * Response: `{lyrics: "..."}` (404 when missing).
 */
@Singleton
class LyricsOvhProvider @Inject constructor(
    private val http: ProviderHttp
) : LyricsProvider {
    override val name: String = "lyricsovh"
    override val enabled: Boolean = true

    override suspend fun fetchLyrics(artist: String, title: String, duration: Long): LyricsResult? {
        if (artist.isBlank() || title.isBlank()) return null
        val url = "https://api.lyrics.ovh/v1/${encode(artist)}/${encode(title)}"
        val body = fetchBody(http.client, http.get(url)) ?: return null
        return runCatching {
            // Minimal parse: {"lyrics":"..."} — escaped quotes handled by unescaping.
            val marker = "\"lyrics\":"
            val start = body.indexOf(marker)
            if (start < 0) return null
            var quote = body.indexOf('"', start + marker.length)
            if (quote < 0) return null
            val out = StringBuilder()
            var i = quote + 1
            while (i < body.length) {
                val c = body[i]
                if (c == '\\' && i + 1 < body.length) {
                    when (body[i + 1]) {
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        '"' -> out.append('"')
                        '\\' -> out.append('\\')
                        'u' -> {
                            val hex = body.substring((i + 2).coerceAtMost(body.length), (i + 6).coerceAtMost(body.length))
                            out.append(hex.toIntOrNull(16)?.toChar() ?: '?')
                            i += 4
                        }
                        else -> out.append(body[i + 1])
                    }
                    i += 2
                } else if (c == '"') {
                    break
                } else {
                    out.append(c)
                    i += 1
                }
            }
            val text = out.toString().trim()
            if (text.isBlank()) null else LyricsResult.Plain(text)
        }.getOrNull()
    }
}
