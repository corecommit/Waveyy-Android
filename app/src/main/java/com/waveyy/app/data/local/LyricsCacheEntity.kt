package com.waveyy.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Race-winner cache row: one entry per song id, 7-day TTL. Separate from
 * `lyrics_cache` (14-day proxy cache) so the two systems never collide.
 */
@Entity(tableName = "lyrics_race_cache")
data class LyricsCacheEntity(
    @PrimaryKey val songId: String,
    val artist: String,
    val title: String,
    /** `synced`, `plain`, or `instrumental`. */
    val kind: String,
    val payload: String,
    val provider: String,
    val score: Double,
    val ts: Long
)
