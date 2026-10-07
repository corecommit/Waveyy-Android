package com.waveyy.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waveyy.app.ui.components.AlbumCard
import com.waveyy.app.ui.components.ArtistCard
import com.waveyy.app.ui.components.HeroPlayButton
import com.waveyy.app.ui.components.NextCard
import com.waveyy.app.ui.components.SongCard
import com.waveyy.app.ui.components.WaveyyBadge
import com.waveyy.app.ui.components.WaveyyButton
import com.waveyy.app.ui.components.WaveyyCommandPalette
import com.waveyy.app.ui.components.WaveyyDestructiveButton
import com.waveyy.app.ui.components.WaveyyDialog
import com.waveyy.app.ui.components.WaveyyDropdownMenu
import com.waveyy.app.ui.components.WaveyyInput
import com.waveyy.app.ui.components.WaveyyMenuEntry
import com.waveyy.app.ui.components.WaveyyProgress
import com.waveyy.app.ui.components.WaveyySkeleton
import com.waveyy.app.ui.components.WaveyySlider
import com.waveyy.app.ui.components.WaveyySpinner
import com.waveyy.app.ui.components.WaveyyTextButton
import com.waveyy.app.ui.components.WaveyyTooltip
import com.waveyy.app.ui.theme.ThemePreviews
import com.waveyy.app.ui.theme.WaveyyTheme

/**
 * Debug-only gallery rendering every ported primitive and entity tile.
 * Registered as a debug-only nav route; never linked from release UI.
 */
@Composable
fun ComponentGalleryScreen() {
    var slider by remember { mutableFloatStateOf(0.4f) }
    var dialog by remember { mutableStateOf(false) }
    Scaffold { padding ->
        LazyColumn(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Buttons", style = MaterialTheme.typography.titleMedium)
                WaveyyButton(text = "Play", onClick = {})
                WaveyyDestructiveButton(text = "Delete", onClick = {})
                WaveyyTextButton(text = "More", onClick = {})
                HeroPlayButton(songId = "s1", onPlay = {})
            }
            item {
                Text("Input + command palette", style = MaterialTheme.typography.titleMedium)
                WaveyyInput(value = "", onValueChange = {}, placeholder = "Search")
                WaveyyCommandPalette(
                    query = "",
                    onQueryChange = {},
                    items = listOf("Alpha", "Beta"),
                    filter = { q, item -> item.contains(q, ignoreCase = true) },
                    onSelect = {}
                )
            }
            item {
                Text("Feedback", style = MaterialTheme.typography.titleMedium)
                WaveyyProgress(progress = 0.6f, modifier = Modifier.fillMaxWidth())
                WaveyySpinner()
                WaveyyBadge(text = "3")
                WaveyySlider(value = slider, onValueChange = { slider = it })
                WaveyyTooltip(text = "Hint") {
                    WaveyyTextButton(text = "Hold me", onClick = {})
                }
                WaveyySkeleton(modifier = Modifier.fillMaxWidth())
                WaveyyDropdownMenu(
                    entries = listOf(WaveyyMenuEntry("One", {})),
                    anchor = { open -> WaveyyTextButton(text = "Menu", onClick = open) }
                )
                WaveyyTextButton(text = "Show dialog", onClick = { dialog = true })
            }
            item {
                Text("Tiles", style = MaterialTheme.typography.titleMedium)
                SongCard(
                    title = "Midnight Waves",
                    artist = "Ocean Band",
                    imageUrl = "",
                    songId = "s1",
                    onPlay = {}
                )
                AlbumCard(
                    title = "Tidal Dreams",
                    artist = "Ocean Band",
                    imageUrl = "",
                    albumId = "a1",
                    onOpen = {}
                )
                ArtistCard(name = "Ocean Band", imageUrl = "", artistId = "a1", onOpen = {})
                NextCard(name = "Next Wave", artist = "Ocean Band", songId = "s2", onSelect = {})
            }
        }
    }
    if (dialog) {
        WaveyyDialog(
            title = "Delete?",
            text = "This is a debug preview.",
            confirmLabel = "Delete",
            onConfirm = { dialog = false },
            onDismiss = { dialog = false },
            dismissLabel = "Cancel"
        )
    }
}

/** Preview of the component gallery. */
@ThemePreviews
@Composable
private fun ComponentGalleryPreview() {
    WaveyyTheme {
        ComponentGalleryScreen()
    }
}
