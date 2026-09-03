package ai.chemistry_learning_org.app.web

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Unit tests for the WebView host whitelist (security-relevant). */
class WebUrlPolicyTest {

    @Test
    fun `allows the root allowed host`() {
        assertThat(WebUrlPolicy.isAllowedHost("chemie-lernen.org")).isTrue()
    }

    @Test
    fun `allows https url host`() {
        assertThat(WebUrlPolicy.isAllowedHost("chemie-lernen.org")).isTrue()
    }

    @Test
    fun `allows www subdomain`() {
        assertThat(WebUrlPolicy.isAllowedHost("www.chemie-lernen.org")).isTrue()
    }

    @Test
    fun `allows other subdomains`() {
        assertThat(WebUrlPolicy.isAllowedHost("m.chemie-lernen.org")).isTrue()
        assertThat(WebUrlPolicy.isAllowedHost("learn.chemie-lernen.org")).isTrue()
    }

    @Test
    fun `blocks external hosts`() {
        assertThat(WebUrlPolicy.isAllowedHost("youtube.com")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("www.youtube.com")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("github.com")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("google.de")).isFalse()
    }

    @Test
    fun `blocks suffix-spoofed hosts`() {
        // Critical: a host that merely ends with the allowed domain must be blocked.
        assertThat(WebUrlPolicy.isAllowedHost("chemie-lernen.org.evil.com")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("evil.chemie-lernen.org.attacker.com")).isFalse()
    }

    @Test
    fun `blocks similar-looking hosts`() {
        assertThat(WebUrlPolicy.isAllowedHost("chemie-lernen.org.com")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("chemie-lernen.orgx")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("xchemie-lernen.org")).isFalse()
    }

    @Test
    fun `blocks null and blank hosts`() {
        assertThat(WebUrlPolicy.isAllowedHost(null)).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("   ")).isFalse()
    }

    @Test
    fun `matching is case-insensitive`() {
        assertThat(WebUrlPolicy.isAllowedHost("CHEMIE-LERNEN.ORG")).isTrue()
        assertThat(WebUrlPolicy.isAllowedHost("www.Chemie-Lernen.org")).isTrue()
    }

    // ------------------------------------------------------------------
    // Additional hardening cases (host is expected WITHOUT scheme, user
    // info, or port — WebView's Uri.host already strips those; the policy
    // must still fail CLOSED if such strings ever reach it).
    // ------------------------------------------------------------------

    @Test
    fun `blocks host with port suffix (fail closed)`() {
        assertThat(WebUrlPolicy.isAllowedHost("chemie-lernen.org:8080")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("chemie-lernen.org:80")).isFalse()
    }

    @Test
    fun `blocks trailing-dot fqdn (fail closed)`() {
        // "chemie-lernen.org." is DNS-equivalent, but the policy must not
        // accept any variation it has not explicitly vetted.
        assertThat(WebUrlPolicy.isAllowedHost("chemie-lernen.org.")).isFalse()
    }

    @Test
    fun `blocks full urls instead of bare hosts (fail closed)`() {
        assertThat(WebUrlPolicy.isAllowedHost("https://chemie-lernen.org")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("https://chemie-lernen.org/themen")).isFalse()
    }

    @Test
    fun `allows deep subdomains`() {
        assertThat(WebUrlPolicy.isAllowedHost("a.b.chemie-lernen.org")).isTrue()
        assertThat(WebUrlPolicy.isAllowedHost("x.y.z.www.chemie-lernen.org")).isTrue()
    }

    @Test
    fun `trims surrounding whitespace before matching`() {
        assertThat(WebUrlPolicy.isAllowedHost(" chemie-lernen.org ")).isTrue()
        assertThat(WebUrlPolicy.isAllowedHost("\tchemie-lernen.org\n")).isTrue()
    }

    @Test
    fun `blocks dash-spoofed hosts`() {
        assertThat(WebUrlPolicy.isAllowedHost("chemie-lernen.org.evil.com")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("chemie-lernen-org.com")).isFalse()
        assertThat(WebUrlPolicy.isAllowedHost("chemie.lernen.org")).isFalse()
    }
}
