package com.waveyy.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * DataStore graph placeholder. `UserPrefs` is `@Inject`-constructible and
 * `@Singleton` scoped, so Hilt provides it with no binding method — a
 * `provideUserPrefs(prefs: UserPrefs)` binding would be a dependency cycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule
