package com.waveyy.app.di

import com.waveyy.app.data.local.datastore.UserPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** DataStore graph. `UserPrefs` is itself injectable; this binds it explicitly. */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /** Exposes the username-map store. */
    @Provides
    @Singleton
    fun provideUserPrefs(prefs: UserPrefs): UserPrefs = prefs
}
