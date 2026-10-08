package com.waveyy.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.waveyy.app.data.local.WaveyyDatabase
import com.waveyy.app.data.local.dao.HistoryDao
import com.waveyy.app.data.local.dao.LyricsCacheDao
import com.waveyy.app.data.local.dao.LyricsDao
import com.waveyy.app.data.local.dao.PlayLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** v1 -> v2: race-winner cache table (existing tables untouched). */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `lyrics_race_cache` (" +
                "`songId` TEXT NOT NULL, `artist` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`kind` TEXT NOT NULL, `payload` TEXT NOT NULL, `provider` TEXT NOT NULL, " +
                "`score` REAL NOT NULL, `ts` INTEGER NOT NULL, PRIMARY KEY(`songId`))"
        )
    }
}

/** Database graph. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** App database. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WaveyyDatabase {
        return Room.databaseBuilder(context, WaveyyDatabase::class.java, "waveyy.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    /** Lyrics cache access. */
    @Provides
    fun provideLyricsDao(database: WaveyyDatabase): LyricsDao = database.lyricsDao()

    /** Race-winner cache access. */
    @Provides
    fun provideLyricsCacheDao(database: WaveyyDatabase): LyricsCacheDao = database.lyricsCacheDao()

    /** Listening-history access. */
    @Provides
    fun provideHistoryDao(database: WaveyyDatabase): HistoryDao = database.historyDao()

    /** Play-log access. */
    @Provides
    fun providePlayLogDao(database: WaveyyDatabase): PlayLogDao = database.playLogDao()
}
