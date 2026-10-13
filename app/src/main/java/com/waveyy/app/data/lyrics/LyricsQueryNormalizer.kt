package com.waveyy.app.data.lyrics

/**
 * Produces ordered fallback query candidates for lyrics lookups.
 * Many songs arrive with featured-artist chains ("Alan Walker, Sabrina
 * Carpenter") and title suffixes ("On My Way (feat. Sabrina Carpenter)")
 * that break strict index lookups. Providers accept fuzzy search but
 * shorter queries still hit more reliably.
 *
 * Ordering is best-guess first: the primary artist + cleaned title
 * usually matches what every lyrics provider indexes.
 */
object LyricsQueryNormalizer {

    /** One query attempt: artist + title to send to a provider. */
    data class LyricsQuery(val artist: String, val title: String)

    private val FEAT_PATTERN = Regex(
        """\s*[\(\[]\s*(?:feat|ft|featuring|with)\.?\s*[^\)\]]+[\)\]]""",
        RegexOption.IGNORE_CASE
    )
    private val SUFFIX_PATTERN = Regex(
        """\s*[\(\[]\s*(?:official\s+)?(?:video|audio|lyric(?:s)?(?:\s+video)?|visualizer|remaster(?:ed)?|radio\s+edit|album\s+version|live|acoustic|instrumental)[^\)\]]*[\)\]]""",
        RegexOption.IGNORE_CASE
    )
    private val DASH_SUFFIX_PATTERN = Regex(
        """\s*[-–—]\s*(?:remaster(?:ed)?|official\s+video|official\s+audio|lyric\s+video|visualizer|audio|video)\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val PRIMARY_SPLIT = Regex(
        """\s*(?:,|&|/|\bfeat\.?\b|\bft\.?\b|\bwith\b)\s+""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Returns up to 3 unique query candidates ordered by likelihood.
     * Always non-empty when [title] is non-blank.
     */
    fun candidates(artist: String, title: String): List<LyricsQuery> {
        val trimmedArtist = artist.trim()
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return emptyList()

        val primary = primaryArtist(trimmedArtist)
        val cleaned = cleanTitle(trimmedTitle)

        val candidates = mutableListOf<LyricsQuery>()

        // 1. Best guess: primary artist + cleaned title.
        if (primary.isNotBlank() && cleaned.isNotBlank()) {
            candidates += LyricsQuery(primary, cleaned)
        }

        // 2. Primary artist + original title (some providers index with
        //    the "(feat. X)" suffix intact).
        if (primary.isNotBlank() && trimmedTitle != cleaned) {
            candidates += LyricsQuery(primary, trimmedTitle)
        }

        // 3. Full artist chain + cleaned title (fallback when the primary
        //    is an alias that providers don't recognize).
        if (trimmedArtist.isNotBlank() && trimmedArtist != primary && cleaned.isNotBlank()) {
            candidates += LyricsQuery(trimmedArtist, cleaned)
        }

        // 4. Cleaned title with no artist at all — last resort.
        if (candidates.isEmpty() && cleaned.isNotBlank()) {
            candidates += LyricsQuery("", cleaned)
        }

        return candidates.distinctBy { it.artist.lowercase() + "|" + it.title.lowercase() }
    }

    /** Extracts the first artist from a comma / & / feat. chain. */
    fun primaryArtist(artist: String): String {
        if (artist.isBlank()) return ""
        return artist.split(PRIMARY_SPLIT)
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            .orEmpty()
    }

    /** Strips bracketed feature credits and common video/remaster suffixes. */
    fun cleanTitle(title: String): String {
        if (title.isBlank()) return ""
        var result = title
        result = FEAT_PATTERN.replace(result, "")
        result = SUFFIX_PATTERN.replace(result, "")
        result = DASH_SUFFIX_PATTERN.replace(result, "")
        return result.trim().replace(Regex("\\s+"), " ")
    }
}
