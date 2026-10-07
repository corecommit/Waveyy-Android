package com.waveyy.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Vertically scrolling column. Ports `components/ui/scroll-area.jsx`.
 *
 * @param modifier layout modifier.
 * @param content column content.
 */
@Composable
fun WaveyyScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        content = content
    )
}

/**
 * Two-pane adaptive row. Ports `components/ui/resizable.jsx`: the web's
 * draggable split becomes a proportional two-pane layout (detail pane
 * hidden on narrow widths by the caller).
 *
 * @param start leading pane content.
 * @param end trailing pane content.
 * @param modifier layout modifier.
 * @param startWeight relative weight of the leading pane.
 */
@Composable
fun WaveyySplitRow(
    start: @Composable () -> Unit,
    end: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    startWeight: Float = 1f
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.weight(startWeight)
        ) {
            start()
        }
        Box(
            modifier = Modifier.weight(1f)
        ) {
            end()
        }
    }
}
