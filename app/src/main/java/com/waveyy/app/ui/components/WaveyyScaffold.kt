package com.waveyy.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowWidthSizeClass

/**
 * One destination in the adaptive shell.
 *
 * @param route nav route selected by this item.
 * @param label visible label.
 * @param icon icon slot; [selectedIcon] used when selected.
 */
data class ShellDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
    val selectedIcon: @Composable (() -> Unit)? = null
)

/**
 * Adaptive app shell. Uses [NavigationRail] on expanded widths and
 * [NavigationBar] on compact/medium widths, mirroring
 * `components/page/sidebar-nav.jsx` + `components/page/mobile-bottom-nav.jsx`.
 *
 * @param destinations rail/bar items.
 * @param currentRoute currently selected route.
 * @param onDestination selected-route callback.
 * @param content screen content (receives content padding implicitly via Scaffold).
 */
@Composable
fun WaveyyScaffold(
    destinations: List<ShellDestination>,
    currentRoute: String?,
    onDestination: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val widthClass = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
    val expanded = widthClass == WindowWidthSizeClass.EXPANDED
    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (!expanded) {
                NavigationBar {
                    destinations.forEach { destination ->
                        ShellBarItem(destination, currentRoute, onDestination, this)
                    }
                }
            }
        }
    ) { _ ->
        if (expanded) {
            androidx.compose.foundation.layout.Row {
                NavigationRail {
                    destinations.forEach { destination ->
                        val selected = destination.route == currentRoute
                        NavigationRailItem(
                            selected = selected,
                            onClick = { onDestination(destination.route) },
                            icon = { (if (selected) destination.selectedIcon else null) ?: destination.icon() },
                            label = { Text(destination.label) }
                        )
                    }
                }
                content()
            }
        } else {
            content()
        }
    }
}

/**
 * One bottom-bar item.
 */
@Composable
private fun RowScope.ShellBarItem(
    destination: ShellDestination,
    currentRoute: String?,
    onDestination: (String) -> Unit,
    scope: RowScope
) {
    val selected = destination.route == currentRoute
    with(scope) {
        NavigationBarItem(
            selected = selected,
            onClick = { onDestination(destination.route) },
            icon = { (if (selected) destination.selectedIcon else null) ?: destination.icon() },
            label = { Text(destination.label) }
        )
    }
}
