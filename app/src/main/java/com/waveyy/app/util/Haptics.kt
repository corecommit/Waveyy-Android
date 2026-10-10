package com.waveyy.app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waveyy.app.data.local.datastore.UserPrefs
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point so [rememberWaveyyHaptics] can reach [UserPrefs] without
 * forcing every call site to thread a ViewModel through. This keeps the
 * haptics toggle a single source of truth.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HapticsEntryPoint {
    /** The app's DataStore-backed prefs. */
    fun userPrefs(): UserPrefs
}

/**
 * Composable helper: reach for a haptic tick from any tap handler without
 * threading HapticFeedback or a ViewModel through parameters. Respects the
 * user's haptics-enabled toggle in Settings.
 *
 * Usage:
 *     val haptics = rememberWaveyyHaptics()
 *     SomeButton(onClick = { haptics.tick(); doThing() })
 */
@Composable
fun rememberWaveyyHaptics(): WaveyyHaptics {
    val hf = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember(context) {
        EntryPointAccessors
            .fromApplication(context, HapticsEntryPoint::class.java)
            .userPrefs()
    }
    val enabled by prefs.hapticsEnabledFlow()
        .collectAsStateWithLifecycle(initialValue = true)
    val enabledState = rememberUpdatedState(enabled)
    return remember(hf, prefs) { WaveyyHaptics(hf, enabledState) }
}

/** Wraps a platform [HapticFeedback] behind a Waveyy-styled surface. */
class WaveyyHaptics(
    private val hf: HapticFeedback,
    private val enabled: State<Boolean>
) {
    /**
     * Standard confirmation tick. No-ops when the user disabled haptics
     * in Settings. Uses LongPress — lighter feedback felt too faint on
     * test devices.
     */
    fun tick() {
        if (!enabled.value) return
        hf.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
