package ai.chemistry_learning_org.app.content

import ai.chemistry_learning_org.app.ui.calculators.calculators
import ai.chemistry_learning_org.app.ui.topics.topics
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the factual claims the app and the store listing make about
 * content ("12 topics", "10+ calculators"): the in-app data lists must
 * stay in sync with the promises in `strings.xml` and in the Play listing
 * texts. A new topic without a matching claim (or the reverse) fails here.
 */
class ContentConsistencyTest {

    // unit tests run with working dir = module dir (app/) → repo root is ..
    private val repoRoot = File("..").canonicalFile

    private fun strings(bucket: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File("src/main/res/$bucket/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate {
            nodes.item(it).attributes.getNamedItem("name").nodeValue to
                nodes.item(it).textContent.trim()
        }
    }

    // ------------------------------------------------------------------
    // Store claims <-> actual in-app data
    // ------------------------------------------------------------------

    @Test
    fun `the app ships exactly 12 topics (store claim)`() {
        assertThat(topics).hasSize(12)
    }

    @Test
    fun `the app ships at least 10 calculators (store claim 10+)`() {
        assertThat(calculators.size).isAtLeast(10)
    }

    @Test
    fun `home screen claim names the real topic count`() {
        for (bucket in listOf("values", "values-de")) {
            val claim = strings(bucket).getValue("home_action_topics_sub")
            val number = Regex("\\d+").find(claim)?.value?.toInt()
            assertThat(number).isEqualTo(topics.size)
        }
    }

    // ------------------------------------------------------------------
    // URL hygiene — these paths open in the whitelisted WebView; keep them
    // canonical so the site actually resolves them.
    // ------------------------------------------------------------------

    @Test
    fun `topic urls are unique and well-formed`() {
        val urls = topics.map { it.url }
        assertThat(urls.toSet()).hasSize(urls.size)
        for (url in urls) {
            assertThat(url).startsWith("/themenbereiche/")
            assertThat(url).endsWith("/")
            assertThat(url).matches("/[a-z0-9\\-/.]+")
        }
    }

    @Test
    fun `calculator urls are unique and well-formed`() {
        val urls = calculators.map { it.url }
        assertThat(urls.toSet()).hasSize(urls.size)
        for (url in urls) {
            assertThat(url).startsWith("/")
            assertThat(url).endsWith("/")
            assertThat(url).matches("/[a-z0-9\\-/.]+")
        }
    }

    @Test
    fun `topic and calculator urls do not overlap`() {
        val overlap = topics.map { it.url }.toSet() intersect calculators.map { it.url }.toSet()
        assertThat(overlap).isEmpty()
    }

    // ------------------------------------------------------------------
    // Store listing texts (repo files) — the same limits Play enforces.
    // ------------------------------------------------------------------

    @Test
    fun `play listing short descriptions stay within 80 chars`() {
        for (locale in listOf("de-DE", "en-US")) {
            val text = File(repoRoot, "play-store/listing/$locale/short-description.txt")
                .readText().trim()
            assertThat(text.length).isAtLeast(30)
            assertThat(text.length).isAtMost(80)
        }
    }

    @Test
    fun `play listing full descriptions stay within 4000 chars and are substantive`() {
        for (locale in listOf("de-DE", "en-US")) {
            val text = File(repoRoot, "play-store/listing/$locale/full-description.txt")
                .readText().trim()
            assertThat(text.length).isAtLeast(1200)
            assertThat(text.length).isAtMost(4000)
        }
    }

    @Test
    fun `play listings mention the platform but never the package id`() {
        for (locale in listOf("de-DE", "en-US")) {
            val text = File(repoRoot, "play-store/listing/$locale/full-description.txt").readText()
            assertThat(text).contains("chemie-lernen.org")
            assertThat(text).doesNotContain("ai.chemistry_learning_org")
        }
    }

    @Test
    fun `release notes exist and stay within the 500 char limit`() {
        for (locale in listOf("de-DE", "en-US")) {
            val text = File(repoRoot, "play-store/listing/$locale/release-notes.txt")
                .readText().trim()
            assertThat(text).isNotEmpty()
            assertThat(text.length).isAtMost(500)
        }
    }
}
