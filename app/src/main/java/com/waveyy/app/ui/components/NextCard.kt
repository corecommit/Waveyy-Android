package com.waveyy.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.waveyy.app.ui.theme.ThemePreviews
import com.waveyy.app.ui.theme.WaveyyTheme

/**
 * Up-next tile: compact text row for queues and recommendations. Ports
 * `components/cards/next.jsx`.
 *
 * @param name song title.
 * @param artist artist label.
 * @param songId song id passed to [onSelect].
 * @param onSelect selection handler.
 * @param modifier layout modifier.
 */
@Composable
fun NextCard(
    name: String,
    artist: String,
    songId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect(songId) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Preview of [NextCard]. */
@ThemePreviews
@Composable
private fun NextCardPreview() {
    WaveyyTheme {
        NextCard(name = "Next Wave", artist = "Ocean Band", songId = "s2", onSelect = {})
    }
}
