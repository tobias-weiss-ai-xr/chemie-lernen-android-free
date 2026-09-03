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

    // ------------------------------------------------------------------
    // Regression tests: real URLs contain slashes and titles contain
    // spaces/umlauts. The route is a single path pattern
    // ("webview/{url}/{title}"), so the BUILT path must stay within that
    // segment structure — otherwise NavController cannot match it and the
    // app crashes (IllegalArgumentException: Navigation destination ...).
    // Reported crash: webview/https://chemie-lernen.org/themenbereiche/...
    // ------------------------------------------------------------------

    @Test
    fun `webview path matches its own route pattern for real urls`() {
        val path = Routes.webview(
            "https://chemie-lernen.org/themenbereiche/einfuehrung-chemie",
            "Einführung in die Chemie",
        )

        val patternSegments = Routes.WEBVIEW.split('/').size
        val pathSegments = path.split('/').size
        assertThat(pathSegments).isEqualTo(patternSegments)
    }

    @Test
    fun `webview url and title arguments contain no raw slashes`() {
        val path = Routes.webview(
            "https://chemie-lernen.org/rechner/molare-masse",
            "Stöchiometrie & Co",
        )

        val args = path.removePrefix("webview/")
        // both arguments together must form exactly two path segments
        assertThat(args.split('/')).hasSize(2)
    }

    @Test
    fun `webview arguments roundtrip url and title`() {
        val url = "https://chemie-lernen.org/themenbereiche/elektrochemie?x=1&y=2#anker"
        val title = "Elektrochemie & Redox (100%)"

        val path = Routes.webview(url, title)
        val args = path.removePrefix("webview/").split('/', limit = 2)
        val (backUrl, backTitle) = Routes.fromWebview(args[0], args[1])

        assertThat(backUrl).isEqualTo(url)
        assertThat(backTitle).isEqualTo(title)
    }

    @Test
    fun `webview roundtrip survives percent and plus characters`() {
        val url = "https://chemie-lernen.org/suche?q=H2SO4%20konz"
        val title = "Lösung 50% +Überschuss"

        val path = Routes.webview(url, title)
        val args = path.removePrefix("webview/").split('/', limit = 2)
        val (backUrl, backTitle) = Routes.fromWebview(args[0], args[1])

        assertThat(backUrl).isEqualTo(url)
        assertThat(backTitle).isEqualTo(title)
    }

    @Test
    fun `webview roundtrip handles empty title`() {
        val url = "https://chemie-lernen.org/"

        val path = Routes.webview(url, "")
        val args = path.removePrefix("webview/").split('/', limit = 2)
        val (backUrl, backTitle) = Routes.fromWebview(args[0], args[1])

        assertThat(backUrl).isEqualTo(url)
        assertThat(backTitle).isEmpty()
    }
}
