package com.waveyy.app.di

import android.content.Context
import androidx.room.Room
import com.waveyy.app.data.local.WaveyyDatabase
import com.waveyy.app.data.local.dao.HistoryDao
import com.waveyy.app.data.local.dao.LyricsDao
import com.waveyy.app.data.local.dao.PlayLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Database graph. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** App database. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WaveyyDatabase {
        return Room.databaseBuilder(context, WaveyyDatabase::class.java, "waveyy.db").build()
    }

    /** Lyrics cache access. */
    @Provides
    fun provideLyricsDao(database: WaveyyDatabase): LyricsDao = database.lyricsDao()

    /** Listening-history access. */
    @Provides
    fun provideHistoryDao(database: WaveyyDatabase): HistoryDao = database.historyDao()

    /** Play-log access. */
    @Provides
    fun providePlayLogDao(database: WaveyyDatabase): PlayLogDao = database.playLogDao()
}
