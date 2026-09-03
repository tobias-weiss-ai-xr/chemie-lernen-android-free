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
}
