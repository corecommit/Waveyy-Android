package com.waveyy.app.data.lyrics

import com.waveyy.app.domain.usecase.SearchRanking

/**
 * Verifies that a lyrics-provider search hit is actually the requested
 * song. Search providers (NetEase, QQ, Tekstowo, Genius) use fuzzy
 * matching internally — "TheFatRat Unity" can return "Someday" by
 * TheFatRat as the first hit. Without verification we cache and display
 * lyrics for the wrong song.
 *
 * Thresholds are lenient enough to accept formatting differences
 * ("On My Way" vs. "On My Way (feat. X)") and strict enough to reject
 * different songs by the same artist.
 */
object LyricsMatcher {

    /** Minimum title similarity to accept a match. */
    private const val TITLE_THRESHOLD = 0.72

    /** Minimum artist similarity to accept a match. */
    private const val ARTIST_THRESHOLD = 0.5

    /**
     * True when the search hit ([matchedTitle], [matchedArtist]) is
     * plausibly the same track as the query ([queryTitle], [queryArtist]).
     * Blank matched title is treated as "unknown" — reject it, since we
     * can't confirm the match.
     */
    fun isMatch(
        matchedTitle: String?,
        matchedArtist: String?,
        queryTitle: String,
        queryArtist: String
    ): Boolean {
        val mt = matchedTitle?.trim().orEmpty()
        if (mt.isBlank()) return false

        // Title key equality is the strongest signal — "On My Way"
        // and "On My Way (feat. Sabrina)" collapse to the same key.
        val matchedKey = SearchRanking.titleKey(mt)
        val queryKey = SearchRanking.titleKey(queryTitle)
        if (matchedKey.isNotBlank() && matchedKey == queryKey) return true

        // Fall back to normalized similarity.
        val titleSim = SearchRanking.similarity(mt, queryTitle)
        if (titleSim < TITLE_THRESHOLD) return false

        // Artist check: blank artist in the hit means we can't verify —
        // accept, since the title was a strong enough signal already.
        val ma = matchedArtist?.trim().orEmpty()
        if (ma.isBlank()) return true

        val artistSim = SearchRanking.similarity(ma, queryArtist)
        if (artistSim >= ARTIST_THRESHOLD) return true

        // Substring check catches "TheFatRat" vs "TheFatRat Music" and
        // similar suffix noise.
        val aLower = ma.lowercase()
        val qLower = queryArtist.lowercase()
        return aLower.contains(qLower) || qLower.contains(aLower)
    }
}
