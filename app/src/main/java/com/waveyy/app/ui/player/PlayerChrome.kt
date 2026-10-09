package com.waveyy.app.ui.player

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Mini-player pill height (64.dp) plus its bottom margin. */
private val MINI_PLAYER_RESERVE = 72.dp

/** Nav pill height (64.dp) plus its bottom margin. */
private val NAV_PILL_RESERVE = 80.dp

/**
 * Bottom space scrollable lists must reserve so the last row scrolls fully
 * above the mini-player + nav pill while music is loaded. Zero when idle.
 * Reacts only to media presence, never to position ticks (no recomposition
 * churn while playing).
 */
@Composable
fun rememberChromeBottomReserve(): Dp {
    val connection = sharedPlayerViewModel().connection
    val presence = remember(connection) {
        connection.state
            .map { it.mediaId != null }
            .distinctUntilChanged()
    }
    val active by presence.collectAsState(initial = false)
    val density = LocalDensity.current
    val navBottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    // Always reserve nav-pill space; add mini-player space only when playing.
    return NAV_PILL_RESERVE + navBottom + if (active) MINI_PLAYER_RESERVE else 0.dp
}
