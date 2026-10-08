package com.waveyy.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.waveyy.app.util.AccentExtractor

/**
 * Dynamic accent derived from artwork: extracts the dominant vibrant color
 * for [imageUrl] (cached per [songId]), animates transitions over 500ms,
 * and falls back to [MaterialTheme.colorScheme.primary] when extraction
 * fails or the URL is blank.
 *
 * @param imageUrl artwork URL, or null.
 * @param songId cache key (stable across recompositions).
 */
@Composable
fun rememberAlbumAccent(imageUrl: String?, songId: String?): Color {
    val fallback = MaterialTheme.colorScheme.primary
    var extracted by remember(songId) { mutableStateOf<Color?>(null) }
    LaunchedEffect(imageUrl, songId) {
        extracted = null
        if (!imageUrl.isNullOrBlank() && !songId.isNullOrBlank()) {
            AccentExtractor.vibrantRgb(imageUrl, songId)?.let { rgb ->
                extracted = Color(rgb)
            }
        }
    }
    val target = extracted ?: fallback
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 500),
        label = "albumAccent"
    )
    return animated
}
