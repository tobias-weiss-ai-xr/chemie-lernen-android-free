package ai.chemistry_learning_org.app.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Unit tests for the navigation route definitions. */
class RoutesTest {

    @Test
    fun `all route constants are distinct`() {
        val routes = listOf(
            Routes.HOME,
            Routes.TOPICS,
            Routes.CALCULATORS,
            Routes.VIDEOS,
            Routes.SETTINGS,
            Routes.WEBVIEW,
        )
        assertThat(routes.toSet()).hasSize(routes.size)
    }

    @Test
    fun `webview route builds a valid path with url and title placeholders`() {
        val path = Routes.webview("https://chemie-lernen.org/rechner", "Rechner")

        assertThat(path).startsWith("webview/")
        // The route template must contain placeholders so NavController can parse args.
        assertThat(Routes.WEBVIEW).contains("{url}")
        assertThat(Routes.WEBVIEW).contains("{title}")
    }

    @Test
    fun `home is the start destination`() {
        // Start destination is hardcoded in ChemieNavHost; keep a guard here so a
        // refactor of the route names does not silently break the entry point.
        assertThat(Routes.HOME).isEqualTo("home")
    }
}
