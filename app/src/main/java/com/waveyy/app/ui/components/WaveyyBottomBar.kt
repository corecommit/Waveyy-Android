package com.waveyy.app.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Bottom-bar destinations (Home / Search / Library / Together / Profile). */
enum class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home),
    SEARCH("search?query=", "Search", Icons.Filled.Search),
    LIBRARY("playlists", "Library", Icons.Filled.LibraryMusic),
    TOGETHER("together", "Together", Icons.Filled.Group),
    PROFILE("profile", "Profile", Icons.Filled.Person);
}

/**
 * Bottom navigation bar, visible on all root-graph screens. Ports
 * `components/page/mobile-bottom-nav.jsx` items (Home/Library/Together)
 * plus Search and Profile.
 *
 * @param currentRoute currently selected route prefix, or null.
 * @param onDestination navigation handler (receives the destination route).
 * @param modifier layout modifier.
 */
@Composable
fun WaveyyBottomBar(
    currentRoute: String?,
    onDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .height(80.dp)
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        BottomDestination.entries.forEach { destination ->
            val selected = currentRoute?.startsWith(destination.route) == true
            NavigationBarItem(
                selected = selected,
                onClick = { onDestination(destination.route) },
                icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
