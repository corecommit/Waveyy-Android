package com.waveyy.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.waveyy.app.domain.model.Song
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
import com.waveyy.app.ui.player.FullPlayerScreen
import com.waveyy.app.ui.player.MiniPlayer
import com.waveyy.app.ui.player.PlayerViewModel
import com.waveyy.app.ui.components.WaveyyBottomBar
import com.waveyy.app.ui.components.WaveyyTopBar
import com.waveyy.app.ui.search.SearchScreen
import com.waveyy.app.ui.settings.SettingsScreen
import com.waveyy.app.ui.together.TogetherScreen
import kotlinx.coroutines.launch

/**
 * Root navigation host with two nested graphs plus the app chrome:
 * [WaveyyTopBar] (logo, search, account) above, [MiniPlayer] and
 * [WaveyyBottomBar] below on every root-graph screen. The full player
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
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun playSongs(songs: List<Song>, index: Int) {
        scope.launch { playerVm.playSongs(songs, index) }
    }

    fun goHome() {
        navController.navigate(Routes.HOME) {
            popUpTo(Graph.ROOT) { inclusive = false }
            launchSingleTop = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isPlayer) {
            WaveyyTopBar(
                query = searchQuery,
                onQuery = { searchQuery = it },
                onSubmitSearch = { query ->
                    if (query.isNotBlank()) navController.navigate(Routes.withQuery(query))
                },
                onHome = { goHome() },
                onProfile = { navController.navigate(Routes.PROFILE) },
                onLibrary = { navController.navigate(Routes.PLAYLISTS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onSignIn = { navController.navigate(Routes.LOGIN) }
            )
        }
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
                        onPlaySong = ::playSongs,
                        onAlbum = { id -> navController.navigate(Routes.withId(Routes.ALBUM, id)) },
                        onArtist = { id -> navController.navigate(Routes.withId(Routes.ARTIST, id)) }
                    )
                }
                composable(Routes.SEARCH) {
                    SearchScreen(
                        onPlaySong = ::playSongs,
                        onAlbum = { id -> navController.navigate(Routes.withId(Routes.ALBUM, id)) },
                        onArtist = { id -> navController.navigate(Routes.withId(Routes.ARTIST, id)) }
                    )
                }
                composable(Routes.ALBUM) {
                    AlbumScreen(onPlaySong = ::playSongs)
                }
                composable(Routes.ARTIST) {
                    ArtistScreen(
                        onPlaySong = ::playSongs,
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
                    FullPlayerScreen(viewModel = playerVm)
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
            WaveyyBottomBar(
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
