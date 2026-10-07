package com.waveyy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.waveyy.app.data.local.LyricsEntity

/** Lyrics cache access with TTL pruning. */
@Dao
interface LyricsDao {

    /** Returns the cached entry for [cacheKey], or null. */
    @Query("SELECT * FROM lyrics_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun get(cacheKey: String): LyricsEntity?

    /** Inserts or replaces a cache entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: LyricsEntity)

    /** Drops entries older than [cutoff] (epoch millis). */
    @Query("DELETE FROM lyrics_cache WHERE ts < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)
}
