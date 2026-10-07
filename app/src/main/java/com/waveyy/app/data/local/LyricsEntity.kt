package com.waveyy.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lyrics cache row. TTL is 14 days, matching `lib/lyrics-client.js`
 * (`LYRICS_CACHE_TTL_MS`). Keyed exactly like the web client:
 * `lyrics:<name>:<artist>:<album>:<duration>` lowercased.
 */
@Entity(tableName = "lyrics_cache")
data class LyricsEntity(
    @PrimaryKey val cacheKey: String,
    val synced: String,
    val plain: String,
    val ts: Long
)
