package com.waveyy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.waveyy.app.data.local.PlayLogEntity

/** Append-only play log with a fixed-size cap. */
@Dao
interface PlayLogDao {

    /** Appends a play event. */
    @Insert
    suspend fun insert(entity: PlayLogEntity)

    /** Recent events, newest first. */
    @Query("SELECT * FROM play_log ORDER BY playedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<PlayLogEntity>

    /** Keeps only the newest [size] rows. */
    @Query("DELETE FROM play_log WHERE id NOT IN (SELECT id FROM play_log ORDER BY id DESC LIMIT :size)")
    suspend fun trimTo(size: Int)
}
