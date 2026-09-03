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

    private val allUrls: List<String> by lazy {
        srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> urlRegex.findAll(file.readText()).map { it.value } }
            .toList()
    }

    @Test
    fun `sources contain url literals at all (sanity so the scan can not rot)`() {
        assertThat(allUrls).isNotEmpty()
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
    fun `url literals are canonical https to the platform`() {
        val hosts = allUrls.map { it.removePrefix("https://").substringBefore('/') }
        assertThat(hosts.toSet()).containsExactly(allowedHost)
    }
}
