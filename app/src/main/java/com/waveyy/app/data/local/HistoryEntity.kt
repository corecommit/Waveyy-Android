package com.waveyy.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Listening-history row. Capped at 50 rows, matching the web client's
 * `song-history` localStorage cap in `music-provider.jsx`.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val songId: String,
    val name: String,
    val artist: String,
    val image: String,
    val playedAt: Long
)
