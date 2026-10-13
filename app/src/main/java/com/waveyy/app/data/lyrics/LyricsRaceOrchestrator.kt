package com.waveyy.app.data.lyrics

import com.waveyy.app.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fires providers in parallel for each query candidate, picks the winner
 * by a category-dominant score, and prefers LRCLIB results on ties.
 *
 * Strategy:
 *  1. For each [LyricsQueryNormalizer.LyricsQuery] candidate (best guess
 *     first), fire all enabled providers in parallel.
 *  2. Each provider call has its own soft timeout so one slow provider
 *     can't kill the collection.
 *  3. Score by quality category (Synced always beats Plain), then by
 *     LRCLIB bonus, then by speed.
 *  4. Return the highest-scoring result. If candidate[0] yields nothing,
 *     try candidate[1], then candidate[2].
 */
@Singleton
class LyricsRaceOrchestrator @Inject constructor(
    private val providers: List<@JvmSuppressWildcards LyricsProvider>,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    /** A provider answer with its score and latency. */
    data class Scored(
        val result: LyricsResult,
        val provider: String,
        val score: Double,
        val latencyMs: Long
    )

    /**
     * Races providers for ([artist], [title]). When [onlyProvider] is set,
     * only that provider runs (preferred-provider mode) and only the first
     * query candidate is tried.
     */
    suspend fun race(
        artist: String,
        title: String,
        duration: Long,
        timeoutMs: Long = 3000L,
        onlyProvider: String? = null
    ): Scored? {
        val candidates = LyricsQueryNormalizer.candidates(artist, title)
        if (candidates.isEmpty()) return null

        // In preferred-provider mode, only the best-guess query and no
        // fallback — the user asked for one specific provider and we
        // shouldn't hammer it with every variant.
        val toTry = if (onlyProvider != null) candidates.take(1) else candidates
        for ((index, query) in toTry.withIndex()) {
            val isFallback = index > 0
            val effectiveTimeout = if (isFallback) FALLBACK_TIMEOUT_MS else timeoutMs
            val result = raceWithCandidate(query, duration, effectiveTimeout, onlyProvider)
            if (result != null) {
                Timber.tag("LyricsRace").d(
                    "candidate %d hit: artist='%s' title='%s' provider=%s",
                    index, query.artist, query.title, result.provider
                )
                return result
            }
        }
        return null
    }

    private suspend fun raceWithCandidate(
        query: LyricsQueryNormalizer.LyricsQuery,
        duration: Long,
        timeoutMs: Long,
        onlyProvider: String?
    ): Scored? = coroutineScope {
        val eligible = providers.filter {
            it.enabled && (onlyProvider == null || it.name == onlyProvider)
        }
        if (eligible.isEmpty()) return@coroutineScope null

        // Each provider call has its own soft timeout so a slow provider
        // resolves to null instead of throwing — the old awaitAll() pattern
        // lost every result when a single provider stalled.
        val deferreds = eligible.map { p ->
            async(io) {
                withTimeoutOrNull(timeoutMs) {
                    val start = System.currentTimeMillis()
                    val r = runCatching { p.fetchLyrics(query.artist, query.title, duration) }
                        .getOrNull() ?: return@withTimeoutOrNull null
                    val elapsed = System.currentTimeMillis() - start
                    Scored(r, p.name, computeScore(r, p.name, elapsed), elapsed)
                }
            }
        }

        val collected = deferreds.awaitAll().filterNotNull()
        if (collected.isEmpty()) return@coroutineScope null
        collected.maxByOrNull { it.score }
    }

    /**
     * Category-dominant scoring: Synced always beats Plain regardless of
     * speed. LRCLIB wins ties (any quality level) by LRCLIB_BONUS points.
     * Speed is a secondary tiebreaker within the same category.
     */
    private fun computeScore(
        result: LyricsResult,
        providerName: String,
        latencyMs: Long
    ): Double {
        val quality = when (result) {
            is LyricsResult.Synced -> 1000.0
            is LyricsResult.Instrumental -> 500.0
            is LyricsResult.Plain -> 100.0
        }
        val speedBonus = (50.0 - (latencyMs / 30.0)).coerceIn(0.0, 50.0)
        val slowPenalty = if (latencyMs > 1500) -20.0 else 0.0
        val shortPenalty = when (result) {
            is LyricsResult.Plain -> if (result.text.length < 200) -20.0 else 0.0
            is LyricsResult.Synced -> if (result.lrc.length < 100) -20.0 else 0.0
            else -> 0.0
        }
        val lrclibBonus = if (providerName == "lrclib") LRCLIB_BONUS else 0.0
        return quality + speedBonus + slowPenalty + shortPenalty + lrclibBonus
    }

    companion object {
        /** Shorter timeout for fallback query candidates. */
        private const val FALLBACK_TIMEOUT_MS = 2000L

        /** Preference bonus applied to any LRCLIB result. */
        private const val LRCLIB_BONUS = 30.0
    }
}
