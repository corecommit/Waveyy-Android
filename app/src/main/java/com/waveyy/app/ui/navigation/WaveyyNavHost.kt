package com.waveyy.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.waveyy.app.BuildConfig
import com.waveyy.app.playback.QueueSource
import com.waveyy.app.ui.album.AlbumScreen
import com.waveyy.app.ui.artist.ArtistScreen
import com.waveyy.app.ui.auth.LoginScreen
import com.waveyy.app.ui.auth.SignupScreen
import com.waveyy.app.ui.debug.ComponentGalleryScreen
import com.waveyy.app.ui.debug.ThemeGalleryScreen
import com.waveyy.app.ui.discover.DiscoverScreen
import com.waveyy.app.ui.home.HomeScreen
import com.waveyy.app.ui.library.PlaylistDetailScreen
import com.waveyy.app.ui.library.PlaylistsScreen
import com.waveyy.app.ui.placeholder.PlaceholderScreen
import com.waveyy.app.ui.player.FullPlayerScreen
import com.waveyy.app.ui.player.MiniPlayer
import com.waveyy.app.ui.player.PlayerViewModel
import com.waveyy.app.ui.components.FloatingNavPill
import com.waveyy.app.ui.search.SearchScreen
import com.waveyy.app.ui.settings.SettingsScreen
import com.waveyy.app.ui.together.TogetherScreen
import kotlinx.coroutines.launch

/**
 * Root navigation host with two nested graphs: [MiniPlayer] and the
 * floating pill below on every root-graph screen. The full player
 * route hides the chrome, mirroring the web's `(player)` layout.
 */
@Composable
fun WaveyyNavHost(
    playerVm: PlayerViewModel = hiltViewModel()
) {
    val navController: NavHostController = rememberNavController()
    val playback by playerVm.connection.state.collectAsState()
    val queue by playerVm.queue.collectAsState()
    val currentSong = queue.firstOrNull { it.id == playback.mediaId }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isPlayer = currentRoute?.startsWith("player") == true
    val scope = rememberCoroutineScope()

    fun playSource(source: QueueSource, index: Int) {
        scope.launch { playerVm.playSource(source, index) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Graph.ROOT,
            modifier = Modifier.weight(1f),
            enterTransition = {
                fadeIn(animationSpec = tween(TRANSITION_MILLIS)) +
                    slideInHorizontally(animationSpec = tween(TRANSITION_MILLIS)) { it / 3 }
            },
            exitTransition = { fadeOut(animationSpec = tween(TRANSITION_MILLIS)) },
            popEnterTransition = { fadeIn(animationSpec = tween(TRANSITION_MILLIS)) },
            popExitTransition = {
                fadeOut(animationSpec = tween(TRANSITION_MILLIS)) +
                    slideOutHorizontally(animationSpec = tween(TRANSITION_MILLIS)) { it / 3 }
            }
        ) {
            navigation(startDestination = Routes.HOME, route = Graph.ROOT) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onPlaySong = ::playSource,
                        onAlbum = { id -> navController.navigate(Routes.withId(Routes.ALBUM, id)) },
                        onArtist = { id -> navController.navigate(Routes.withId(Routes.ARTIST, id)) },
                        onSearch = { navController.navigate(Routes.withQuery("")) },
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                        onDiscover = { navController.navigate(Routes.DISCOVER) }
                    )
                }
                composable(Routes.DISCOVER) {
                    DiscoverScreen(onPlaySong = ::playSource)
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        onPlaySong = ::playSource,
                        onAlbum = { id -> navController.navigate(Routes.withId(Routes.ALBUM, id)) },
                        onArtist = { id -> navController.navigate(Routes.withId(Routes.ARTIST, id)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.ALBUM) {
                    AlbumScreen(onPlaySong = ::playSource)
                }
                composable(Routes.ARTIST) {
                    ArtistScreen(
                        onPlaySong = ::playSource,
                        onAlbum = { id -> navController.navigate(Routes.withId(Routes.ALBUM, id)) }
                    )
                }
                composable(Routes.PLAYLISTS) {
                    PlaylistsScreen(
                        onOpen = { id -> navController.navigate(Routes.withId(Routes.PLAYLIST_DETAIL, id)) }
                    )
                }
                composable(
                    route = Routes.PLAYLIST_DETAIL,
                    deepLinks = listOf(navDeepLink { uriPattern = "waveyy://playlist/{id}" })
                ) { PlaylistDetailScreen() }
                composable(Routes.PROFILE) { PlaceholderScreen("Profile") }
                composable(
                    route = Routes.TOGETHER,
                    arguments = listOf(
                        navArgument("roomId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    ),
                    deepLinks = listOf(navDeepLink { uriPattern = "waveyy://room/{roomId}" })
                ) { TogetherScreen() }
                composable(Routes.CONNECTIONS) { PlaceholderScreen("Connections") }
                composable(Routes.SETTINGS) {
                    SettingsScreen()
                }
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
                composable(
                    route = Routes.PLAYER,
                    deepLinks = listOf(navDeepLink { uriPattern = "waveyy://song/{id}" })
                ) {
                    FullPlayerScreen(
                    viewModel = playerVm,
                    onClose = { navController.popBackStack() }
                )
                }
            }
        }
        if (!isPlayer) {
            if (playback.mediaId != null) {
                MiniPlayer(
                    connection = playerVm.connection,
                    currentSong = currentSong,
                    onExpand = {
                        val id = playback.mediaId ?: "current"
                        navController.navigate(Routes.withId(Routes.PLAYER, id))
                    }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingNavPill(
                    currentRoute = currentRoute?.substringBefore("?"),
                    onDestination = { route ->
                        navController.navigate(route) {
                            popUpTo(Graph.ROOT) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
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

/** Shared screen-transition duration (M3 default specs). */
private const val TRANSITION_MILLIS = 300
