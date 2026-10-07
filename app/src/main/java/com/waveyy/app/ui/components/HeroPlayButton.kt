package com.waveyy.app.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Hero play button for artist/album headers. Ports
 * `components/artist/hero-play-button.jsx` (auth gating is a no-op on
 * Android too — guests may play).
 *
 * @param songId seed song id passed to [onPlay].
 * @param onPlay play handler.
 * @param modifier layout modifier.
 * @param label button label.
 */
@Composable
fun HeroPlayButton(
    songId: String,
    onPlay: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Play"
) {
    Button(onClick = { onPlay(songId) }, modifier = modifier) {
        Text(label)
    }
}
