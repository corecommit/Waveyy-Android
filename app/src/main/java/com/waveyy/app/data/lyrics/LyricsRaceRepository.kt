package com.waveyy.app.data.lyrics

import com.waveyy.app.data.local.WaveyyDatabase
import com.waveyy.app.data.local.dao.LyricsCacheDao
import com.waveyy.app.data.local.LyricsCacheEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Race + cache facade. Winners persist 7 days per song id; losers are
 * never stored (a retry re-races). Named `LyricsRaceRepository` (rather
 * than a second `LyricsRepository`) so the legacy proxy-cache repository
 * in `data.repository` stays unambiguous. Takes the database (not the
 * DAO) so the annotation-processed DAO type never enters Dagger's
 * constructor validation.
 */
@Singleton
class LyricsRaceRepository @Inject constructor(
    private val orchestrator: LyricsRaceOrchestrator,
    private val lrclib: LrclibProvider,
    database: WaveyyDatabase
) {
    private val dao: LyricsCacheDao = database.lyricsCacheDao()

    /**
     * Returns the cached winner when fresh (unless [bypassCache]), else
     * races and caches the winner. Null when nobody has lyrics.
     */
    suspend fun getLyrics(
        songId: String,
        artist: String,
        title: String,
        duration: Long,
        bypassCache: Boolean = false,
        onlyProvider: String? = null
    ): LyricsRaceOrchestrator.Scored? {
        if (!bypassCache) {
            dao.get(songId, System.currentTimeMillis() - CACHE_TTL_MILLIS)?.let {
                return it.toScored()
            }
        }
        val winner = orchestrator.race(artist, title, duration, onlyProvider = onlyProvider)
            ?: return null
        // Store normalized artist/title so equivalent songs (featuring
        // chains stripped, primary artist extracted) hit the same cache
        // row via the getByTitleArtist fallback lookup.
        val cacheArtist = LyricsQueryNormalizer.primaryArtist(artist)
            .ifBlank { artist }
        val cacheTitle = LyricsQueryNormalizer.cleanTitle(title)
            .ifBlank { title }
        dao.upsert(winner.toEntity(songId, cacheArtist, cacheTitle))
        Timber.d(
            "LyricsRace won by %s (score=%.1f, %dms)",
            winner.provider, winner.score, winner.latencyMs
        )
        return winner
    }

    /**
     * Fast cache-only lookup: returns cached lyrics for [songId] when fresh,
     * else a fallback hit keyed on (title, artist) to survive id drift,
     * else null. Never races.
     */
    suspend fun getCachedFast(
        songId: String,
        title: String,
        artist: String
    ): LyricsRaceOrchestrator.Scored? {
        val cutoff = System.currentTimeMillis() - CACHE_TTL_MILLIS
        dao.get(songId, cutoff)?.let { return it.toScored() }
        // Try the normalized (artist, title) pair first — cache writes use
        // normalized values, so a raw lookup would miss.
        val normalizedArtist = LyricsQueryNormalizer.primaryArtist(artist)
            .ifBlank { artist }
        val normalizedTitle = LyricsQueryNormalizer.cleanTitle(title)
            .ifBlank { title }
        dao.getByTitleArtist(normalizedTitle, normalizedArtist, cutoff)?.let {
            return it.toScored()
        }
        // Fall back to the raw pair in case an older cache row predates
        // the normalization change.
        dao.getByTitleArtist(title, artist, cutoff)?.let { return it.toScored() }
        return null
    }

    /**
     * Phrase search across lyrics providers. Currently uses LRCLIB
     * (free, keyless, indexed). Returns the raw match list — callers
     * resolve each to a JioSaavn song by title+artist.
     */
    suspend fun searchByLyricsPhrase(phrase: String): List<LyricsTextMatch> {
        return runCatching { lrclib.searchByLyrics(phrase) }
            .getOrElse {
                Timber.tag("LyricsRace").d(
                    "phrase search failed: ${it.message}"
                )
                emptyList()
            }
    }

    /** Clears the race cache (Settings button). */
    suspend fun clearCache() {
        dao.clearAll()
    }

    private fun LyricsRaceOrchestrator.Scored.toEntity(
        songId: String,
        artist: String,
        title: String
    ): LyricsCacheEntity {
        val (kind, payload) = when (result) {
            is LyricsResult.Synced -> "synced" to result.lrc
            is LyricsResult.Plain -> "plain" to result.text
            is LyricsResult.Instrumental -> "instrumental" to ""
        }
        return LyricsCacheEntity(
            songId = songId,
            artist = artist,
            title = title,
            kind = kind,
            payload = payload,
            provider = provider,
            score = score,
            ts = System.currentTimeMillis()
        )
    }

    private fun LyricsCacheEntity.toScored(): LyricsRaceOrchestrator.Scored {
        val result = when (kind) {
            "synced" -> LyricsResult.Synced(payload)
            "instrumental" -> LyricsResult.Instrumental
            else -> LyricsResult.Plain(payload)
        }
        return LyricsRaceOrchestrator.Scored(result, provider, score, 0L)
    }

    companion object {
        /** 30-day winner TTL. */
        const val CACHE_TTL_MILLIS = 30L * 24L * 3600_000L
    }
}
