package com.waveyy.app.data.lyrics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * tekstowo.pl scraper: search page then first song page, no key.
 * Search: `tekstowo.pl/szukaj,wykonawca,{artist},tytul,{title}.html`.
 * Lyrics live in `<div class="song-text">`.
 */
@Singleton
class TekstowoProvider @Inject constructor(
    private val http: ProviderHttp
) : LyricsProvider {
    override val name: String = "tekstowo"
    override val enabled: Boolean = true

    override suspend fun fetchLyrics(artist: String, title: String, duration: Long): LyricsResult? {
        if (artist.isBlank() || title.isBlank()) return null
        return runCatching {
            val searchUrl = "https://www.tekstowo.pl/szukaj,wykonawca,${slug(artist)},tytul,${slug(title)}.html"
            val search = fetchBody(http.client, http.get(searchUrl)) ?: return null
            val href = SONG_LINK.find(search)?.groupValues?.get(1) ?: return null
            val page = fetchBody(http.client, http.get("https://www.tekstowo.pl$href")) ?: return null
            val raw = SONG_TEXT.find(page)?.groupValues?.get(1) ?: return null
            val text = raw
                .replace(BR, "\n")
                .replace(TAG, "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
                .trim()
            if (text.length < MIN_CHARS) null else LyricsResult.Plain(text)
        }.getOrNull()
    }

    private fun slug(value: String): String {
        return encode(value.trim().lowercase().replace(WHITESPACE, "+"))
            .replace("%2B", "+", ignoreCase = true)
    }

    companion object {
        private val SONG_LINK = Regex("""href="(/piosenka,[^"]+\.html)"""")
        private val SONG_TEXT = Regex("""<div class="song-text"[^>]*>([\s\S]*?)</div>""")
        private val BR = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
        private val TAG = Regex("""<[^>]+>""")
        private val WHITESPACE = Regex("""\s+""")
        private const val MIN_CHARS = 200
    }
}
