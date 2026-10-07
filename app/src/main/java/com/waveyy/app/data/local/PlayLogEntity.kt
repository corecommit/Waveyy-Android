package com.waveyy.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Play-log row. Capped at 300 rows, matching the web client's
 * `song-play-log` localStorage cap in `music-provider.jsx`.
 */
@Entity(tableName = "play_log")
data class PlayLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val songId: String,
    val playedAt: Long
)
