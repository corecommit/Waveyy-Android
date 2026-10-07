package com.waveyy.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point. Hilt generates the dependency graph from here;
 * Timber is planted for debug builds only.
 */
@HiltAndroidApp
class WaveyyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
