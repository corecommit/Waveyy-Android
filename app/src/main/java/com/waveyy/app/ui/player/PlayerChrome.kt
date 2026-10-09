package com.waveyy.app.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    // Mini-player + nav pill are Column siblings of the NavHost (see
    // WaveyyNavHost.kt), so the NavHost itself already shrinks to make
    // room. Only a small breathing-space gap is needed at the bottom of
    // scrollable lists.
    return if (active) 8.dp else 4.dp
}
