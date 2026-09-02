package org.chemie_lernen_org.app.web

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
}
