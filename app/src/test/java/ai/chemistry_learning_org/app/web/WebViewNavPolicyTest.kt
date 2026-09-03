package ai.chemistry_learning_org.app.web

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the WebView navigation decision policy — the pure core
 * of WebViewClient.shouldOverrideUrlLoading. Three outcomes:
 *
 *  - ALLOW         load inside the in-app WebView (whitelisted host, https)
 *  - OPEN_EXTERNAL hand to the OS (mailto:, tel:, geo:, intent: — those
 *                  were silently swallowed before this policy existed)
 *  - BLOCK         ignore (foreign https hosts, script/data/file URIs)
 */
class WebViewNavPolicyTest {

    private fun decide(uri: String?) = WebViewNavPolicy.decide(uri)

    // ---------------- ALLOW ----------------

    @Test
    fun `allows the platform start page`() {
        assertThat(decide("https://chemie-lernen.org/")).isEqualTo(WebViewNavPolicy.Decision.ALLOW)
    }

    @Test
    fun `allows platform pages and subdomains`() {
        assertThat(decide("https://chemie-lernen.org/themenbereiche/energetik/"))
            .isEqualTo(WebViewNavPolicy.Decision.ALLOW)
        assertThat(decide("https://www.chemie-lernen.org/impressum/"))
            .isEqualTo(WebViewNavPolicy.Decision.ALLOW)
    }

    @Test
    fun `allows query strings and fragments on the platform`() {
        assertThat(decide("https://chemie-lernen.org/suche?q=ph+werte&seite=2#erg"))
            .isEqualTo(WebViewNavPolicy.Decision.ALLOW)
    }

    // ---------------- OPEN_EXTERNAL ----------------

    @Test
    fun `hands mailto links to the operating system`() {
        assertThat(decide("mailto:info@chemie-lernen.org"))
            .isEqualTo(WebViewNavPolicy.Decision.OPEN_EXTERNAL)
    }

    @Test
    fun `hands tel and geo intents to the operating system`() {
        assertThat(decide("tel:+491234567")).isEqualTo(WebViewNavPolicy.Decision.OPEN_EXTERNAL)
        assertThat(decide("geo:52.52,13.405")).isEqualTo(WebViewNavPolicy.Decision.OPEN_EXTERNAL)
    }

    @Test
    fun `hands android intent uris to the operating system`() {
        assertThat(decide("intent://open#Intent;scheme=https;end"))
            .isEqualTo(WebViewNavPolicy.Decision.OPEN_EXTERNAL)
    }

    // ---------------- BLOCK ----------------

    @Test
    fun `blocks foreign https hosts`() {
        assertThat(decide("https://youtube.com/watch?v=x")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
        assertThat(decide("https://evil.com/chemie-lernen.org")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
    }

    @Test
    fun `blocks userinfo spoofing`() {
        // host of this URI is evil.com, NOT the allowed domain
        assertThat(decide("https://chemie-lernen.org@evil.com/"))
            .isEqualTo(WebViewNavPolicy.Decision.BLOCK)
    }

    @Test
    fun `blocks insecure http`() {
        assertThat(decide("http://chemie-lernen.org/")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
    }

    @Test
    fun `blocks executable and local schemes`() {
        assertThat(decide("javascript:alert(1)")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
        assertThat(decide("data:text/html;base64,PHNjcmlwdD4=")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
        assertThat(decide("file:///sdcard/index.html")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
        assertThat(decide("about:blank")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
    }

    @Test
    fun `blocks url without host (fail closed)`() {
        assertThat(decide("https://")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
    }

    @Test
    fun `blocks null blank and relative input (fail closed)`() {
        assertThat(decide(null)).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
        assertThat(decide("")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
        assertThat(decide("   ")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
        assertThat(decide("/relative/path")).isEqualTo(WebViewNavPolicy.Decision.BLOCK)
    }

    @Test
    fun `whitespace-padded urls are handled`() {
        assertThat(decide(" https://chemie-lernen.org/ ")).isEqualTo(WebViewNavPolicy.Decision.ALLOW)
    }
}
