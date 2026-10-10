package com.waveyy.app.domain.usecase

import java.text.Normalizer

/**
 * Search ranking ported verbatim from
 * `app/(root)/search/_components/Search.jsx`: normalization, Levenshtein
 * distance, similarity, query variants, and the artist scoring ladder.
 */
object SearchRanking {

    /** Lowercase alphanumeric normalization (diacritics stripped). */
    fun normalizeText(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("[\\u0300-\\u036f]"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** Classic Levenshtein edit distance over normalized inputs. */
    fun editDistance(a: String?, b: String?): Int {
        val s = normalizeText(a)
        val t = normalizeText(b)
        val dp = Array(s.length + 1) { IntArray(t.length + 1) }
        for (i in 0..s.length) dp[i][0] = i
        for (j in 0..t.length) dp[0][j] = j
        for (i in 1..s.length) {
            for (j in 1..t.length) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s.length][t.length]
    }

    /** 0..1 similarity from edit distance over the longer string. */
    fun similarity(a: String?, b: String?): Double {
        val s = normalizeText(a)
        val t = normalizeText(b)
        if (s.isEmpty() || t.isEmpty()) return 0.0
        val maxLen = maxOf(s.length, t.length)
        if (maxLen == 0) return 0.0
        return 1.0 - editDistance(s, t).toDouble() / maxLen
    }

    /**
     * Query variants tried against the API (raw, normalized, `&`/`and`
     * swaps, whitespace-stripped).
     */
    fun queryVariants(rawQuery: String?): List<String> {
        val normalized = normalizeText(rawQuery)
        val variants = linkedSetOf(rawQuery?.trim().orEmpty(), normalized)
        if ("&" in normalized) variants += normalized.replace("&", "and")
        if (" and " in normalized) variants += normalized.replace(" and ", " ")
        if (" " in normalized) variants += normalized.replace(" ", "")
        return variants.filter { it.isNotEmpty() }
    }

    /**
     * Normalizes a song title for dedup purposes: strips feature credits,
     * remix/version/format suffixes, and trailing parentheticals so "Harvey",
     * "Harvey (Remix)", "Harvey - Slowed + Reverb", and "Harvey (feat. X)"
     * all collapse to the same key.
     *
     * Returns an empty string when the title has no usable content.
     */
    fun titleKey(name: String?): String {
        if (name.isNullOrBlank()) return ""
        // Lowercase, strip punctuation/diacritics.
        val normalized = normalizeText(name)
        // Cut at feature credits.
        val noFeature = normalized
            .substringBefore(" feat ")
            .substringBefore(" ft ")
            .substringBefore(" featuring ")
        // Drop known version/format words and anything after them.
        val versionWords = listOf(
            "remix", "remixed", "remaster", "remastered",
            "sped", "slowed", "reverb", "reverbed",
            "live", "acoustic", "instrumental", "unplugged",
            "cover", "version", "edit", "extended", "radio",
            "demo", "deluxe", "remastered",
            "audio", "official", "lyric", "lyrics", "video"
        )
        var result = noFeature
        for (word in versionWords) {
            val idx = result.indexOf(" $word")
            if (idx >= 0) {
                result = result.substring(0, idx)
                break
            }
        }
        return result.trim()
    }

    /**
     * Artist relevance score for a raw [query] (higher is better, 100 is an
     * exact variant match). Mirrors `getArtistScore` exactly, including the
     * >= 50 top-result threshold used by callers.
     */
    fun artistScore(name: String?, query: String?): Double {
        val normalizedName = normalizeText(name)
        val normalizedQuery = normalizeText(query)
        val variants = queryVariants(query)
        val queryWords = normalizedQuery.split(" ").filter { it.isNotEmpty() }
        val nameWords = normalizedName.split(" ").filter { it.isNotEmpty() }
        val intersection = queryWords.count { it in nameWords }
        val overlapBoost = if (queryWords.isNotEmpty()) {
            intersection.toDouble() / queryWords.size * 20.0
        } else {
            0.0
        }
        val wholeDistance = editDistance(normalizedName, normalizedQuery)
        val wholeSimilarity = similarity(normalizedName, normalizedQuery)
        val firstWordDistance = if (!queryWords.isNullOrEmpty() && nameWords.isNotEmpty()) {
            editDistance(queryWords[0], nameWords[0])
        } else {
            99
        }
        val firstWordSimilarity = if (queryWords.isNotEmpty() && nameWords.isNotEmpty()) {
            similarity(queryWords[0], nameWords[0])
        } else {
            0.0
        }
        val fuzzyTokenMatches = queryWords.count { word ->
            nameWords.any { similarity(word, it) >= 0.72 }
        }
        if (normalizedName.isEmpty()) return 0.0
        if (variants.any { normalizeText(it) == normalizedName }) return 100.0
        if (variants.any { normalizedName.startsWith(normalizeText(it)) }) return 85.0 + overlapBoost
        if (variants.any { normalizedName.contains(normalizeText(it)) }) return 65.0 + overlapBoost
        if (wholeDistance <= 2 && normalizedQuery.length > 4) return 80.0
        if (wholeSimilarity >= 0.72) return 78.0 + overlapBoost
        if (firstWordDistance <= 2 && intersection > 0) return 72.0
        if (firstWordSimilarity >= 0.72 && (intersection > 0 || fuzzyTokenMatches > 1)) {
            return 70.0 + overlapBoost
        }
        if (fuzzyTokenMatches >= maxOf(1, queryWords.size - 1)) return 62.0 + overlapBoost
        if (normalizedQuery.contains(normalizedName)) return 45.0 + overlapBoost
        if (intersection > 0 || fuzzyTokenMatches > 0) return 40.0 + overlapBoost
        return 0.0
    }

    /**
     * Combined relevance score for a song against a search query. Higher is
     * better. Exact title match is 100, prefix 85, contains 65, with a
     * fuzzy-similarity falloff below.
     *
     * Adds a token-based artist boost (fuzzy-matched so "marshmellow" scores
     * against "marshmello") and a small log-scaled popularity bonus so the
     * original track outranks covers / karaoke / tribute uploads that happen
     * to match the query literally.
     *
     * @param name song title.
     * @param artist display artist string.
     * @param query the raw query as typed by the user.
     * @param playCount optional play count for the popularity tiebreak
     *   (0 to skip). Pass 0 for tests or when unknown.
     */
    fun songRelevance(
        name: String?,
        artist: String?,
        query: String?,
        playCount: Long = 0L
    ): Double {
        if (name.isNullOrBlank() || query.isNullOrBlank()) return 0.0
        val n = normalizeText(name)
        val q = normalizeText(query)
        if (n.isEmpty() || q.isEmpty()) return 0.0

        val titleScore = when {
            n == q -> 110.0
            n.startsWith("$q ") -> 90.0
            n.contains(q) -> 70.0
            q.contains(n) && n.length >= 4 -> 55.0
            else -> {
                val sim = similarity(n, q)
                if (sim >= 0.72) sim * 60.0 else sim * 30.0
            }
        }

        val qWords = q.split(" ").filter { it.length > 1 }
        val nWords = n.split(" ").filter { it.length > 1 }
        val titleOverlap = qWords.count { qw -> nWords.any { it == qw } }
        val titleOverlapBoost = if (qWords.isNotEmpty()) {
            titleOverlap.toDouble() / qWords.size * 10.0
        } else {
            0.0
        }

        // Token-based, fuzzy artist boost: each query token that matches an
        // artist token (exact or >= 0.80 similarity) adds 6 points, capped
        // so it can't dominate title relevance.
        val a = normalizeText(artist.orEmpty())
        val aWords = a.split(" ").filter { it.length > 1 }
        val artistTokenMatches = qWords.count { qw ->
            aWords.any { aw -> aw == qw || similarity(aw, qw) >= 0.80 }
        }
        val artistBoost = (artistTokenMatches * 6.0).coerceAtMost(18.0)

        // Small popularity tiebreak: log10(playCount + 1) * 2.5, capped at
        // 15 points. Real tracks tend to have 10^6-10^9 plays; covers 10^3-10^5.
        val popularityBonus = if (playCount > 0L) {
            (kotlin.math.log10(playCount.toDouble() + 1.0) * 2.5).coerceAtMost(15.0)
        } else {
            0.0
        }

        return titleScore + titleOverlapBoost + artistBoost + popularityBonus
    }
}
