package com.waveyy.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline profile generator: exercises cold startup so release builds
 * ship AOT-compiled hot paths (target: cold start under 1.5s on a
 * mid-range device).
 *
 * This test only runs on a connected device/emulator API 28+:
 * `./gradlew :baselineprofile:collectBaselineProfile` (or the
 * `collect...VariantBaselineProfile` task). It never runs in CI. Copy the
 * generated `baseline-prof.txt` into
 * `app/src/main/baselineProfiles/` to ship it.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    /** Profile collection rule. */
    @get:Rule
    val rule = BaselineProfileRule()

    /** Cold-startup journey: launch home and wait for content. */
    @Test
    fun generate() = rule.collect(
        packageName = APP_PACKAGE,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Trending")), START_TIMEOUT_MILLIS)
    }

    companion object {
        private const val APP_PACKAGE = "com.waveyy.app.debug"
        private const val START_TIMEOUT_MILLIS = 10_000L
    }
}
