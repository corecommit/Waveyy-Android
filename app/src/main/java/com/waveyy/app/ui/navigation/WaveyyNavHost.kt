package com.waveyy.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.waveyy.app.BuildConfig
import com.waveyy.app.ui.auth.LoginScreen
import com.waveyy.app.ui.auth.SignupScreen
import com.waveyy.app.ui.debug.ComponentGalleryScreen
import com.waveyy.app.ui.debug.ThemeGalleryScreen
import com.waveyy.app.ui.home.HomeScreen
import com.waveyy.app.ui.placeholder.PlaceholderScreen

/**
 * Root navigation host with two nested graphs:
 * - `root_graph`: home, search/:id, album/:id, artist/:id, playlists,
 *   playlists/:id, profile, together, connections, login, signup.
 * - `player_graph`: full player `[id]`, mirroring `app/(player)/[id]/page.jsx`.
 *
 * Routes without a dedicated screen yet render [PlaceholderScreen] and are
 * replaced phase by phase (Phases 6-10).
 */
@Composable
fun WaveyyNavHost() {
    val navController: NavHostController = rememberNavController()
    NavHost(navController = navController, startDestination = Graph.ROOT) {
        navigation(startDestination = Routes.HOME, route = Graph.ROOT) {
            composable(Routes.HOME) { HomeScreen() }
            composable(Routes.SEARCH) { PlaceholderScreen("Search") }
            composable(Routes.ALBUM) { PlaceholderScreen("Album") }
            composable(Routes.ARTIST) { PlaceholderScreen("Artist") }
            composable(Routes.PLAYLISTS) { PlaceholderScreen("Playlists") }
            composable(Routes.PLAYLIST_DETAIL) { PlaceholderScreen("Playlist detail") }
            composable(Routes.PROFILE) { PlaceholderScreen("Profile") }
            composable(Routes.TOGETHER) { PlaceholderScreen("Together") }
            composable(Routes.CONNECTIONS) { PlaceholderScreen("Connections") }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onSignedIn = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onSignup = { navController.navigate(Routes.SIGNUP) }
                )
            }
            composable(Routes.SIGNUP) {
                SignupScreen(
                    onSignedIn = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SIGNUP) { inclusive = true }
                        }
                    },
                    onLogin = { navController.popBackStack() }
                )
            }
            if (BuildConfig.DEBUG) {
                composable(Routes.THEME_GALLERY) { ThemeGalleryScreen() }
                composable(DebugRoutes.COMPONENT_GALLERY) { ComponentGalleryScreen() }
            }
        }
        navigation(startDestination = Routes.PLAYER, route = Graph.PLAYER) {
            composable(Routes.PLAYER) { PlaceholderScreen("Player") }
        }
    }
}

/** Nested graph route names. */
object Graph {
    /** Main content graph. */
    const val ROOT = "root_graph"

    /** Full-screen player graph. */
    const val PLAYER = "player_graph"
}

/** Debug-only routes (registered in debug builds only). */
object DebugRoutes {
    /** Component gallery route. */
    const val COMPONENT_GALLERY = "debug/component-gallery"
}
