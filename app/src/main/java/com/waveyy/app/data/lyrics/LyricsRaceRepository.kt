package com.waveyy.app.data.lyrics

import com.waveyy.app.data.local.WaveyyDatabase
import com.waveyy.app.data.local.LyricsCacheDao
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
        dao.upsert(winner.toEntity(songId, artist, title))
        Timber.d(
            "LyricsRace won by %s (score=%.1f, %dms)",
            winner.provider, winner.score, winner.latencyMs
        )
        return winner
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
        /** 7-day winner TTL. */
        const val CACHE_TTL_MILLIS = 7L * 24L * 3600_000L
    }
}
