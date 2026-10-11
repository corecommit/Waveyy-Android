package com.waveyy.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point. Hilt generates the dependency graph from here;
 * Timber is planted for debug builds only. Also configures Coil's singleton
 * ImageLoader with an aggressive cache and crossfade so artwork loading is
 * resilient on flaky mobile connections.
 */
@HiltAndroidApp
class WaveyyApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .logger(if (BuildConfig.DEBUG) DebugLogger() else null)
            .build()
    }
}
