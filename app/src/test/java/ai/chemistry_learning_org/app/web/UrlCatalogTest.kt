package ai.chemistry_learning_org.app.web

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Static defense-in-depth: WebUrlPolicy whitelists hosts at WebView
 * runtime, but a hardcoded third-party URL in the sources would load
 * before the policy ever sees it (initial page, quick actions). Every
 * URL literal in the main sources must therefore point at the allowed
 * host, and nothing may use plain http.
 */
class UrlCatalogTest {

    private val srcRoot = File("src/main/java")

    private val urlRegex = Regex("""https?://[A-Za-z0-9.\-_]+(?:/[A-Za-z0-9./\-_]*)?""")
    private val allowedHost = "chemie-lernen.org"

    private val allKtFiles: List<File> by lazy {
        srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    }

    private val allUrls: List<String> by lazy {
        allKtFiles.flatMap { file -> urlRegex.findAll(file.readText()).map { it.value } }
            .toList()
    }

    @Test
    fun `platform host is defined in exactly one place (the web package)`() {
        // SiteUrls.BASE builds on WebUrlPolicy.ALLOWED_HOST; no other file
        // may hardcode the domain, or the whitelist and the links can drift.
        val filesWithHost = allKtFiles.filter { it.readText().contains(allowedHost) }
        assertThat(filesWithHost).isNotEmpty()
        for (file in filesWithHost) {
            assertThat(file.path.replace('\\', '/')).contains("/web/")
        }
    }

    @Test
    fun `no url literals outside the web package (all go through SiteUrls)`() {
        val offenders = allKtFiles
            .filter { !it.path.replace('\\', '/').contains("/web/") }
            .flatMap { file -> urlRegex.findAll(file.readText()).map { "${file.name}: ${it.value}" } }
        assertThat(offenders).isEmpty()
    }

    @Test
    fun `every url literal points at the allowed host`() {
        val offenders = allUrls.filter { url ->
            val host = url.removePrefix("https://").removePrefix("http://")
                .substringBefore('/').substringBefore(':')
            host != allowedHost && !host.endsWith(".$allowedHost")
        }
        assertThat(offenders).isEmpty()
    }

    @Test
    fun `no insecure http literals in main sources`() {
        val insecure = allUrls.filter { it.startsWith("http://") }
        assertThat(insecure).isEmpty()
    }

    @Test
    fun `any url literal that remains must be canonical https to the platform`() {
        // may legitimately be empty (everything routed through SiteUrls) —
        // but a literal, if present, must be exactly the allowed host
        val hosts = allUrls.map { it.removePrefix("https://").substringBefore('/') }
        assertThat(hosts.filter { it != allowedHost }).isEmpty()
    }
}
