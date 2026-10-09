package com.waveyy.app.ui.player

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Activity-scoped [PlayerViewModel] shared by every screen.
 *
 * Destination-level `hiltViewModel()` calls are scoped to nav back-stack
 * entries (a different instance per screen), which would fragment the queue.
 * Screens needing the now-playing queue must use this helper instead.
 */
@Composable
fun sharedPlayerViewModel(): PlayerViewModel {
    val activity = LocalActivity.current ?: error("PlayerViewModel requires an Activity host")
    return hiltViewModel(activity)
}
