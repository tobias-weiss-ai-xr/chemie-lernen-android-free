package ai.chemistry_learning_org.app.web

import ai.chemistry_learning_org.app.ui.calculators.calculators
import ai.chemistry_learning_org.app.ui.topics.topics
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Contract tests for the central site-URL builder. Before this object
 * existed, "https://chemie-lernen.org..." was hardcoded in six call sites
 * — a typo in one of them would navigate straight into the WebUrlPolicy
 * block (or, worse, if the host ever changed, into a dead end).
 */
class SiteUrlsTest {

    // ---------------- base URL ----------------

    @Test
    fun `base url is https on the whitelisted host`() {
        assertThat(SiteUrls.BASE).isEqualTo("https://chemie-lernen.org")
        val host = SiteUrls.BASE.removePrefix("https://")
        assertThat(host).isEqualTo(WebUrlPolicy.ALLOWED_HOST)
    }

    @Test
    fun `every built url passes the whitelist`() {
        val candidates = listOf(
            "", "/", "/themenbereiche/energetik/", "/lernvideos/",
            "/impressum/", "/datenschutz/",
        )
        for (path in candidates) {
            val url = SiteUrls.resolve(path)
            assertThat(WebUrlPolicy.isAllowedHost(url.removePrefix("https://").substringBefore('/')))
                .isTrue()
            assertThat(url).startsWith("https://")
        }
    }

    // ---------------- resolve(path) ----------------

    @Test
    fun `resolve tolerates missing and duplicated leading slashes`() {
        assertThat(SiteUrls.resolve("impressum/"))
            .isEqualTo("https://chemie-lernen.org/impressum/")
        assertThat(SiteUrls.resolve("//impressum/"))
            .isEqualTo("https://chemie-lernen.org/impressum/")
    }

    @Test
    fun `resolve of empty path yields the bare base url`() {
        assertThat(SiteUrls.resolve("")).isEqualTo("https://chemie-lernen.org")
        assertThat(SiteUrls.resolve("/")).isEqualTo("https://chemie-lernen.org")
    }

    @Test
    fun `resolve never produces double slashes in the path`() {
        val url = SiteUrls.resolve("/themenbereiche/einfuehrung-chemie/")
        assertThat(url.removePrefix("https://")).doesNotContain("//")
    }

    // ---------------- convenience urls ----------------

    @Test
    fun `kg api url is built on the allowed host with pagination params`() {
        assertThat(SiteUrls.apiKgData(400, 200))
            .isEqualTo("https://chemie-lernen.org/api/kg-data?offset=400&limit=200")
        assertThat(SiteUrls.apiKgData(0))
            .isEqualTo("https://chemie-lernen.org/api/kg-data?offset=0&limit=200")
    }

    @Test
    fun `fixed page urls are correct`() {
        assertThat(SiteUrls.home()).isEqualTo("https://chemie-lernen.org")
        assertThat(SiteUrls.videos()).isEqualTo("https://chemie-lernen.org/lernvideos/")
        assertThat(SiteUrls.privacy()).isEqualTo("https://chemie-lernen.org/datenschutz/")
        assertThat(SiteUrls.imprint()).isEqualTo("https://chemie-lernen.org/impressum/")
    }

    // ---------------- integration with the real data lists ----------------

    @Test
    fun `every topic url resolves to a whitelisted https url with its path preserved`() {
        for (topic in topics) {
            val url = SiteUrls.resolve(topic.url)
            assertThat(url).startsWith(SiteUrls.BASE)
            assertThat(url).endsWith(topic.url)
        }
    }

    @Test
    fun `every calculator url resolves to a whitelisted https url with its path preserved`() {
        for (calculator in calculators) {
            val url = SiteUrls.resolve(calculator.url)
            assertThat(url).startsWith(SiteUrls.BASE)
            assertThat(url).endsWith(calculator.url)
        }
    }
}
