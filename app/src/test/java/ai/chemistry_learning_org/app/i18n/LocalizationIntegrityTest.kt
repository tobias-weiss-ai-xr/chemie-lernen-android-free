package ai.chemistry_learning_org.app.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pure-JVM integrity tests for the app's string resources.
 *
 * Guarantees the localization contract: English is the DEFAULT bucket
 * (`values/`), German is the secondary bucket (`values-de/`). These tests
 * fail the build when translations drift (missing keys, German leaking into
 * the default bucket, empty values).
 */
class LocalizationIntegrityTest {

    private fun readStrings(bucket: String): Map<String, String> {
        val file = File("src/main/res/$bucket/strings.xml")
        assertThat(file.exists()).isTrue()
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
        val nodes = doc.getElementsByTagName("string")
        val map = mutableMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes.getNamedItem("name").nodeValue
            map[name] = node.textContent.trim()
        }
        return map
    }

    private val en = readStrings("values")
    private val de = readStrings("values-de")

    @Test
    fun `default bucket is non-trivially large`() {
        assertThat(en.keys.size).isAtLeast(50)
    }

    @Test
    fun `german bucket translates every default key`() {
        val missing = en.keys - de.keys
        assertThat(missing).isEmpty()
    }

    @Test
    fun `german bucket has no orphan keys`() {
        val orphans = de.keys - en.keys
        assertThat(orphans).isEmpty()
    }

    @Test
    fun `default values contain no german umlauts (names allowed)`() {
        // "Weiß" is the author's proper name and is intentionally kept.
        val offenders = en.filterValues { v ->
            v.contains(Regex("[äöüÄÖÜß]")) && !v.contains("Weiß")
        }
        assertThat(offenders).isEmpty()
    }

    @Test
    fun `app name is localized`() {
        assertThat(en.getValue("app_name")).isEqualTo("Chemistry Learning")
        assertThat(de.getValue("app_name")).isEqualTo("Chemie Lernen")
        assertThat(en.getValue("app_name")).isNotEqualTo(de.getValue("app_name"))
    }

    @Test
    fun `no empty string values in any bucket`() {
        val emptyEn = en.filterValues { it.isEmpty() }
        val emptyDe = de.filterValues { it.isEmpty() }
        assertThat(emptyEn).isEmpty()
        assertThat(emptyDe).isEmpty()
    }

    @Test
    fun `german values are actually different from english where meaningfully translatable`() {
        // Short labels like "Home" may legitimately match; but the overall
        // translation rate must show the German bucket is real, not a copy.
        val differing = en.keys.count { key -> en.getValue(key) != de.getValue(key) }
        val differingRatio = differing.toDouble() / en.keys.size
        assertThat(differingRatio).isAtLeast(0.5)
    }

    // ------------------------------------------------------------------
    // Formatting and hygiene
    // ------------------------------------------------------------------

    @Test
    fun `format placeholders match between english and german`() {
        // A missing/swapped %1$s in one locale crashes String.format at
        // runtime for that language only — hard to spot in review.
        val placeholder = Regex("%\\d+\$s|%\\d+\$d|%\\d+\$[df]|%s|%d")
        for (key in en.keys) {
            val enArgs = placeholder.findAll(en.getValue(key)).map { it.value }.sorted().toList()
            val deArgs = placeholder.findAll(de.getValue(key) ).map { it.value }.sorted().toList()
            assertThat(deArgs).isEqualTo(enArgs)
        }
    }

    @Test
    fun `no unfinished placeholders leak into strings`() {
        for ((bucket, map) in listOf("values" to en, "values-de" to de)) {
            val offenders = map.filterValues { v ->
                Regex("(?i)\\b(TODO|FIXME|XXX|PLACEHOLDER)\\b").containsMatchIn(v)
            }
            assertThat(offenders).isEmpty()
        }
    }

    @Test
    fun `string values have no surrounding whitespace`() {
        // the parser already trims; guard against xml-embedded newlines
        // that survive entity-only resources
        for ((bucket, map) in listOf("values" to en, "values-de" to de)) {
            val offenders = map.filterValues { v -> v != v.trim() || v.contains("\n") }
            assertThat(offenders).isEmpty()
        }
    }

    @Test
    fun `apostrophes are escaped for aapt`() {
        // unescaped ' breaks the Android resource compiler
        for ((bucket, map) in listOf("values" to en, "values-de" to de)) {
            val offenders = map.filterValues { v -> Regex("(?<!\\\\)' ").containsMatchIn("$v ") }
            assertThat(offenders).isEmpty()
        }
    }
}
