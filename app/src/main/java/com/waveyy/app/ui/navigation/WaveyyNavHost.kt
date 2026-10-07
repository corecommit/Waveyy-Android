package com.waveyy.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.waveyy.app.BuildConfig
import com.waveyy.app.ui.debug.ThemeGalleryScreen
import com.waveyy.app.ui.home.HomeScreen

/**
 * Root navigation host. Phase 1 exposes the placeholder [HomeScreen] only;
 * the full root/player graphs land in Phase 5.
 */
@Composable
fun WaveyyNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen()
        }
        if (BuildConfig.DEBUG) {
            composable(Routes.THEME_GALLERY) {
                ThemeGalleryScreen()
            }
        }
    }
}
