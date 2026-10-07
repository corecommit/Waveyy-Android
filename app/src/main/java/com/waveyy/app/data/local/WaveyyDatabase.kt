package com.waveyy.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.waveyy.app.data.local.dao.HistoryDao
import com.waveyy.app.data.local.dao.LyricsDao
import com.waveyy.app.data.local.dao.PlayLogDao

/** App database: lyrics cache, listening history, play log. */
@Database(
    entities = [LyricsEntity::class, HistoryEntity::class, PlayLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WaveyyDatabase : RoomDatabase() {
    /** Lyrics cache access. */
    abstract fun lyricsDao(): LyricsDao

    /** Listening-history access. */
    abstract fun historyDao(): HistoryDao

    /** Play-log access. */
    abstract fun playLogDao(): PlayLogDao
}
