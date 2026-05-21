package com.streamvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.*
import androidx.navigation.compose.*
import com.streamvault.ui.screens.*
import com.streamvault.ui.theme.StreamVaultTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String) {
    object Gallery : Screen("gallery")
    object Admin   : Screen("admin")
    object Player  : Screen("player/{videoId}") {
        fun createRoute(id: String) = "player/$id"
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamVaultTheme {
                StreamVaultNavHost()
            }
        }
    }
}

@Composable
fun StreamVaultNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Gallery.route,
        enterTransition = {
            fadeIn(tween(280)) + slideInHorizontally(tween(280, easing = EaseOutCubic)) { it / 6 }
        },
        exitTransition = {
            fadeOut(tween(200)) + slideOutHorizontally(tween(200, easing = EaseInCubic)) { -it / 6 }
        },
        popEnterTransition = {
            fadeIn(tween(280)) + slideInHorizontally(tween(280, easing = EaseOutCubic)) { -it / 6 }
        },
        popExitTransition = {
            fadeOut(tween(200)) + slideOutHorizontally(tween(200, easing = EaseInCubic)) { it / 6 }
        }
    ) {
        composable(Screen.Gallery.route) {
            GalleryScreen(
                onVideoClick = { id -> navController.navigate(Screen.Player.createRoute(id)) },
                onAdminClick = { navController.navigate(Screen.Admin.route) }
            )
        }

        composable(Screen.Admin.route) {
            AdminScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(navArgument("videoId") { type = NavType.StringType }),
            enterTransition = { fadeIn(tween(350)) },
            exitTransition  = { fadeOut(tween(250)) }
        ) { backStack ->
            val videoId = backStack.arguments?.getString("videoId") ?: return@composable
            PlayerScreen(videoId = videoId, onBack = { navController.popBackStack() })
        }
    }
}

