package com.waveyy.app.di

import com.waveyy.app.data.lyrics.GeniusProvider
import com.waveyy.app.data.lyrics.LrclibProvider
import com.waveyy.app.data.lyrics.LyricsOvhProvider
import com.waveyy.app.data.lyrics.LyricsProvider
import com.waveyy.app.data.lyrics.MusixmatchProvider
import com.waveyy.app.data.lyrics.NetEaseProvider
import com.waveyy.app.data.lyrics.QqMusicProvider
import com.waveyy.app.data.lyrics.TekstowoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Lyrics-race graph: aggregates the seven providers for the orchestrator. */
@Module
@InstallIn(SingletonComponent::class)
object LyricsModule {

    /** All providers in priority order (orchestrator filters by `enabled`). */
    @Provides
    @Singleton
    fun provideLyricsProviders(
        lrclib: LrclibProvider,
        netease: NetEaseProvider,
        qqmusic: QqMusicProvider,
        lyricsovh: LyricsOvhProvider,
        tekstowo: TekstowoProvider,
        musixmatch: MusixmatchProvider,
        genius: GeniusProvider
    ): List<LyricsProvider> {
        return listOf(lrclib, netease, qqmusic, lyricsovh, tekstowo, musixmatch, genius)
    }
}
