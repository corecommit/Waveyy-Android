package com.waveyy.app.ui.update

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Activity-scoped [UpdateViewModel]. Both the global popup (mounted in
 * WaveyyNavHost) and the Settings screen read from this instance so the
 * "seen" state and popup visibility stay consistent.
 */
@Composable
fun sharedUpdateViewModel(): UpdateViewModel {
    val activity = LocalActivity.current as? ComponentActivity
        ?: error("UpdateViewModel requires a ComponentActivity host")
    return hiltViewModel(activity)
}
