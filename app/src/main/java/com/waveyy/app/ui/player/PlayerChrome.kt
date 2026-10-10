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

/**
 * Bottom breathing-space gap at the end of scrollable lists (8.dp while
 * music is loaded, 4.dp when idle).
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
    val navBottom = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }
    // Bars are now an overlay on top of the NavHost, so scrollable lists
    // must reserve room for them. Nav pill (64) + bottom margin (16) +
    // system nav inset + a small breathing gap. Mini-player adds another
    // 76 (64 pill + 12 spacer) when a song is loaded.
    val navReserve = 64.dp + 16.dp + navBottom + 8.dp
    val miniReserve = if (active) 76.dp else 0.dp
    return navReserve + miniReserve
}
