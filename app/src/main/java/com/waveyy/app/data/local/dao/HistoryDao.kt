package com.waveyy.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.waveyy.app.data.local.HistoryEntity

/** Listening-history access with a fixed-size cap. */
@Dao
interface HistoryDao {

    /** Recent history, newest first. */
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<HistoryEntity>

    /** Inserts or replaces a history row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    /** Keeps only the newest [size] rows. */
    @Query(
        "DELETE FROM history WHERE songId NOT IN " +
            "(SELECT songId FROM history ORDER BY playedAt DESC LIMIT :size)"
    )
    suspend fun trimTo(size: Int)

    /** All history rows (newest first, no cap). */
    @Query("SELECT * FROM history ORDER BY playedAt DESC")
    suspend fun all(): List<HistoryEntity>

    /** Deletes all history rows. */
    @Query("DELETE FROM history")
    suspend fun clearAll()
}
