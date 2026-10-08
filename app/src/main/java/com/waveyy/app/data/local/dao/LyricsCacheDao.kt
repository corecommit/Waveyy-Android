package com.waveyy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.waveyy.app.data.local.LyricsCacheEntity

/** Race-winner cache access with TTL pruning. */
@Dao
interface LyricsCacheDao {

    /** Returns the cached winner for [songId] newer than [minTs], or null. */
    @Query("SELECT * FROM lyrics_race_cache WHERE songId = :songId AND ts >= :minTs LIMIT 1")
    suspend fun get(songId: String, minTs: Long): LyricsCacheEntity?

    /** Inserts or replaces a winner entry. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricsCacheEntity)

    /** Drops entries older than [cutoff] (epoch millis). */
    @Query("DELETE FROM lyrics_race_cache WHERE ts < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)

    /** Clears the whole race cache (Settings button). */
    @Query("DELETE FROM lyrics_race_cache")
    suspend fun clearAll()
}
