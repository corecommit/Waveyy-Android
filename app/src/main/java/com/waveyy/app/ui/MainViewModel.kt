package com.waveyy.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * Activity-scoped root logic: bottom-nav tab re-select events. Tapping the
 * tab you're already on must scroll its content to top — never re-navigate,
 * re-fetch, or rebuild. Screens collect [scrollToTop] filtered by their tab
 * route and animate their own list state.
 */
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val scrollRequests = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Tab routes requesting scroll-to-top (emitted by the bottom nav). */
    val scrollToTop: SharedFlow<String> = scrollRequests.asSharedFlow()

    /** Requests [tab]'s screen to animate back to the top. */
    fun requestScrollToTop(tab: String) {
        scrollRequests.tryEmit(tab)
    }
}

/**
 * Activity-scoped [MainViewModel] shared by the nav host and every tab
 * screen. Destination-level `hiltViewModel()` calls are scoped to nav
 * back-stack entries (a different instance per screen), which would break
 * the shared event channel. Tab screens must use this helper instead.
 */
@Composable
fun sharedMainViewModel(): MainViewModel {
    val activity = LocalActivity.current as? ComponentActivity
        ?: error("MainViewModel requires a ComponentActivity host")
    return hiltViewModel(activity)
}
