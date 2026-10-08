package com.waveyy.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Floating pill destinations (Home / Playlists / Together, per web nav). */
enum class PillDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home),
    PLAYLISTS("playlists", "Library", Icons.Filled.LibraryMusic),
    TOGETHER("together", "Together", Icons.Filled.Group);
}

/**
 * Floating centered pill navigation. Selected item is a primary pill with
 * icon + label; unselected items are icon-only in onSurfaceVariant.
 * Replaces the full-width bottom bar, mirroring the web's mobile nav
 * items (Home / Playlists / Together).
 *
 * @param currentRoute currently selected route prefix, or null.
 * @param onDestination navigation handler.
 * @param modifier layout modifier.
 */
@Composable
fun FloatingNavPill(
    currentRoute: String?,
    onDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PillDestination.entries.forEach { destination ->
                val selected = currentRoute?.startsWith(destination.route) == true
                val background by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    animationSpec = tween(200),
                    label = "pillBg"
                )
                val content by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(200),
                    label = "pillFg"
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(background)
                        .clickable { onDestination(destination.route) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(imageVector = destination.icon, contentDescription = null, tint = content)
                    if (selected) {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = content
                        )
                    }
                }
            }
        }
    }
}
