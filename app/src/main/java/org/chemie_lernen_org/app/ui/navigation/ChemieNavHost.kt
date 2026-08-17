package org.chemie_lernen_org.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.chemie_lernen_org.app.ui.home.HomeScreen
import org.chemie_lernen_org.app.ui.topics.TopicsScreen
import org.chemie_lernen_org.app.ui.calculators.CalculatorsScreen
import org.chemie_lernen_org.app.ui.videos.VideosScreen
import org.chemie_lernen_org.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val TOPICS = "topics"
    const val CALCULATORS = "calculators"
    const val VIDEOS = "videos"
    const val SETTINGS = "settings"
    const val WEBVIEW = "webview/{url}/{title}"
    fun webview(url: String, title: String) = "webview/$url/$title"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemieNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = "Home",
                    selected = false,
                    onClick = { navController.navigate(Routes.HOME) { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    label = "Themen",
                    selected = false,
                    onClick = { navController.navigate(Routes.TOPICS) { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                    label = "Rechner",
                    selected = false,
                    onClick = { navController.navigate(Routes.CALCULATORS) { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                    label = "Videos",
                    selected = false,
                    onClick = { navController.navigate(Routes.VIDEOS) { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = "Mehr",
                    selected = false,
                    onClick = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenTopics = { navController.navigate(Routes.TOPICS) },
                    onOpenCalculators = { navController.navigate(Routes.CALCULATORS) },
                    onOpenVideos = { navController.navigate(Routes.VIDEOS) },
                    onOpenUrl = { url, title -> navController.navigate(Routes.webview(url, title)) },
                )
            }
            composable(Routes.TOPICS) {
                TopicsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUrl = { url, title -> navController.navigate(Routes.webview(url, title)) },
                )
            }
            composable(Routes.CALCULATORS) {
                CalculatorsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUrl = { url, title -> navController.navigate(Routes.webview(url, title)) },
                )
            }
            composable(Routes.VIDEOS) {
                VideosScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUrl = { url, title -> navController.navigate(Routes.webview(url, title)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUrl = { url, title -> navController.navigate(Routes.webview(url, title)) },
                )
            }
            composable(Routes.WEBVIEW) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: "Chemie Lernen"
                org.chemie_lernen_org.app.ui.webview.WebViewScreen(
                    url = url,
                    title = title,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
