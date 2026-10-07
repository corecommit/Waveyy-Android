package com.waveyy.app.util

import com.waveyy.app.domain.model.LyricLine

/**
 * LRC parsing and lyric text cleanup. Ports the parsing in
 * `components/player/sidebar-player.jsx` (`parseLrc`) plus the
 * `LYRICS_TEXT_FIXES` romanized-Hindi corrections from `lib/lyrics-client.js`.
 */
object LyricsParser {

    private val timestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    private val textFixes = listOf(
        Regex("\\baashikli\\b", RegexOption.IGNORE_CASE) to "aashiki",
        Regex("\\baashikii\\b", RegexOption.IGNORE_CASE) to "aashiki",
        Regex("\\bbhil\\b", RegexOption.IGNORE_CASE) to "bhi",
        Regex("\\bbhii\\b", RegexOption.IGNORE_CASE) to "bhi",
        Regex("\\bmeraa\\b", RegexOption.IGNORE_CASE) to "mera",
        Regex("\\bjindagii\\b", RegexOption.IGNORE_CASE) to "jindagi"
    )

    /**
     * Sanitizes a lyric payload, applying the romanized-Hindi fixes.
     *
     * @return pair of (synced, plain).
     */
    fun sanitize(synced: String, plain: String): Pair<String, String> {
        return sanitizeText(synced) to sanitizeText(plain)
    }

    /** Applies the known transliteration corrections to [text]. */
    fun sanitizeText(text: String): String {
        var out = text
        for ((pattern, replacement) in textFixes) {
            out = pattern.replace(out, replacement)
        }
        return out
    }

    /**
     * Parses LRC text into time-ordered lines. A source line may carry
     * several timestamps (each yields a line); lines without a valid
     * timestamp are dropped.
     */
    fun parseLrc(lrc: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        for (raw in lrc.lineSequence()) {
            val matches = timestamp.findAll(raw).toList()
            if (matches.isEmpty()) continue
            val text = timestamp.replace(raw, "").trim()
            if (text.isEmpty()) continue
            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: continue
                val seconds = match.groupValues[2].toLongOrNull() ?: continue
                val fraction = match.groupValues[3]
                val millis = when (fraction.length) {
                    1 -> (fraction.toLongOrNull() ?: 0L) * 100L
                    2 -> (fraction.toLongOrNull() ?: 0L) * 10L
                    3 -> fraction.toLongOrNull() ?: 0L
                    else -> 0L
                }
                lines += LyricLine(
                    timeMs = minutes * 60_000L + seconds * 1000L + millis,
                    text = text
                )
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
