package com.waveyy.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.waveyy.app.BuildConfig
import com.waveyy.app.ui.album.AlbumScreen
import com.waveyy.app.ui.artist.ArtistScreen
import com.waveyy.app.ui.auth.LoginScreen
import com.waveyy.app.ui.auth.SignupScreen
import com.waveyy.app.ui.debug.ComponentGalleryScreen
import com.waveyy.app.ui.debug.ThemeGalleryScreen
import com.waveyy.app.ui.home.HomeScreen
import com.waveyy.app.ui.library.PlaylistDetailScreen
import com.waveyy.app.ui.library.PlaylistsScreen
import com.waveyy.app.ui.placeholder.PlaceholderScreen
import com.waveyy.app.ui.player.BottomBarPlayer
import com.waveyy.app.ui.player.FullPlayerScreen
import com.waveyy.app.ui.player.PlayerViewModel
import com.waveyy.app.ui.search.SearchScreen

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
fun WaveyyNavHost(
    playerVm: PlayerViewModel = hiltViewModel()
) {
    val navController: NavHostController = rememberNavController()
    val playback by playerVm.connection.state.collectAsState()
    val queue by playerVm.queue.collectAsState()
    val currentSong = queue.firstOrNull { it.id == playback.mediaId }
    Column {
        NavHost(
            navController = navController,
            startDestination = Graph.ROOT,
            modifier = Modifier.weight(1f)
        ) {
        navigation(startDestination = Routes.HOME, route = Graph.ROOT) {
            composable(Routes.HOME) {
                HomeScreen(
                    onAlbum = { id -> navController.navigate(Routes.withId(Routes.ALBUM, id)) },
                    onArtist = { id -> navController.navigate(Routes.withId(Routes.ARTIST, id)) }
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onAlbum = { id -> navController.navigate(Routes.withId(Routes.ALBUM, id)) },
                    onArtist = { id -> navController.navigate(Routes.withId(Routes.ARTIST, id)) }
                )
            }
            composable(Routes.ALBUM) {
                AlbumScreen()
            }
            composable(Routes.ARTIST) {
                ArtistScreen(
                    onAlbum = { id -> navController.navigate(Routes.withId(Routes.ALBUM, id)) }
                )
            }
            composable(Routes.PLAYLISTS) {
                PlaylistsScreen(
                    onOpen = { id -> navController.navigate(Routes.withId(Routes.PLAYLIST_DETAIL, id)) }
                )
            }
            composable(Routes.PLAYLIST_DETAIL) { PlaylistDetailScreen() }
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
            composable(Routes.PLAYER) {
                FullPlayerScreen(viewModel = playerVm)
            }
        }
        }
        if (queue.isNotEmpty()) {
            BottomBarPlayer(
                connection = playerVm.connection,
                currentSong = currentSong,
                onExpand = {
                    val id = playback.mediaId ?: "current"
                    navController.navigate(Routes.withId(Routes.PLAYER, id))
                }
            )
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
