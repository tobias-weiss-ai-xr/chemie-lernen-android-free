package ai.chemistry_learning_org.app.ui.navigation

import java.util.Base64

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ai.chemistry_learning_org.R
import ai.chemistry_learning_org.app.ui.home.HomeScreen
import ai.chemistry_learning_org.app.ui.topics.TopicsScreen
import ai.chemistry_learning_org.app.ui.calculators.CalculatorsScreen
import ai.chemistry_learning_org.app.ui.videos.VideosScreen
import ai.chemistry_learning_org.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val TOPICS = "topics"
    const val CALCULATORS = "calculators"
    const val VIDEOS = "videos"
    const val SETTINGS = "settings"
    const val WEBVIEW = "webview/{url}/{title}"

    /**
     * Builds the webview route path. URL and title are base64url-encoded so
     * the path keeps exactly three segments ("webview", url, title) — raw
     * slashes in URLs previously broke NavController pattern matching and
     * crashed the app (IllegalArgumentException: Navigation destination ...
     * cannot be found). Base64url (A-Z a-z 0-9 - _) contains no '/', '%',
     * or spaces, so Navigation's own percent-decoding cannot corrupt it.
     */
    fun webview(url: String, title: String): String {
        val enc = { s: String ->
            Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray(Charsets.UTF_8))
        }
        return "webview/${enc(url)}/${enc(title)}"
    }

    /** Inverse of [webview] — decode the two path arguments. */
    fun fromWebview(encodedUrl: String, encodedTitle: String): Pair<String, String> {
        val dec = { s: String ->
            String(Base64.getUrlDecoder().decode(s), Charsets.UTF_8)
        }
        return dec(encodedUrl) to dec(encodedTitle)
    }
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
                    label = { Text(stringResource(R.string.nav_home)) },
                    selected = false,
                    onClick = { navController.navigate(Routes.HOME) { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_topics)) },
                    selected = false,
                    onClick = { navController.navigate(Routes.TOPICS) { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_calculators)) },
                    selected = false,
                    onClick = { navController.navigate(Routes.CALCULATORS) { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_videos)) },
                    selected = false,
                    onClick = { navController.navigate(Routes.VIDEOS) { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_more)) },
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
                val rawUrl = backStackEntry.arguments?.getString("url").orEmpty()
                val rawTitle = backStackEntry.arguments?.getString("title").orEmpty()
                val (url, decodedTitle) = Routes.fromWebview(rawUrl, rawTitle)
                val title = decodedTitle.ifEmpty { stringResource(R.string.app_name) }
                ai.chemistry_learning_org.app.ui.webview.WebViewScreen(
                    url = url,
                    title = title,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
