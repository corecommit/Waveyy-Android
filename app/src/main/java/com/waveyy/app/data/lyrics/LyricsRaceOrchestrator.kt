package com.waveyy.app.data.lyrics

import com.waveyy.app.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fires all enabled providers in parallel, waits up to [timeoutMs], and
 * picks the winner by quality + speed score. Slow providers are cancelled
 * once the deadline passes.
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
     * only that provider runs (preferred-provider mode).
     */
    suspend fun race(
        artist: String,
        title: String,
        duration: Long,
        timeoutMs: Long = 3000L,
        onlyProvider: String? = null
    ): Scored? = coroutineScope {
        val deferreds = providers
            .filter { it.enabled && (onlyProvider == null || it.name == onlyProvider) }
            .map { p ->
                async(io) {
                    val start = System.currentTimeMillis()
                    val r = runCatching { p.fetchLyrics(artist, title, duration) }.getOrNull()
                    val elapsed = System.currentTimeMillis() - start
                    r?.let { Scored(it, p.name, 0.0, elapsed) }
                }
            }

        // Collect ALL results within the shared window — one slow provider
        // must not block the collection of faster ones.
        val results: List<Scored?> = withTimeoutOrNull(timeoutMs) {
            deferreds.awaitAll()
        }.orEmpty()

        // Cancel any that didn't finish in time.
        deferreds.forEach { it.cancel() }

        val collected = results.filterNotNull()
        if (collected.isEmpty()) return@coroutineScope null
        collected.map { it.copy(score = computeScore(it)) }.maxByOrNull { it.score }
    }

    private fun computeScore(s: Scored): Double {
        val quality = when (s.result) {
            is LyricsResult.Synced -> 100.0
            is LyricsResult.Instrumental -> 60.0
            is LyricsResult.Plain -> 40.0
        }
        val speedBonus = (50.0 - (s.latencyMs / 30.0)).coerceAtLeast(0.0)
        val slowPenalty = if (s.latencyMs > 1000) -30.0 else 0.0
        val shortPenalty = when (s.result) {
            is LyricsResult.Plain -> if (s.result.text.length < 200) -20.0 else 0.0
            is LyricsResult.Synced -> if (s.result.lrc.length < 100) -20.0 else 0.0
            else -> 0.0
        }
        return quality + speedBonus + slowPenalty + shortPenalty
    }
}
