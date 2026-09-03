package ai.chemistry_learning_org.app.kg

import ai.chemistry_learning_org.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KgCategoriesTest {

    private fun entity(name: String, category: String?, related: Int = 3) = KgEntity(
        name = name,
        category = category,
        description = if (related > 1) "beschreibt $name" else null,
        relationCount = related,
        related = (1..related).map { KgRelation("R$it", "RELATED_TO", "konzept") },
    )

    @Test
    fun `all api categories have a color and a label`() {
        val fallbackColor = KgCategories.meta("unbekannt").colorHex
        for (category in KgCategories.chipOrder) {
            assertThat(KgCategories.colorHex(category)).isNotEqualTo(fallbackColor)
            assertThat(KgCategories.labelRes(category)).isNotEqualTo(R.string.cat_other)
        }
    }

    @Test
    fun `categories from the live sample are all covered`() {
        // stoff konzept reaktion methode person quelle (aus kg-sample.json)
        for (category in listOf("stoff", "konzept", "reaktion", "methode", "person", "quelle")) {
            assertThat(KgCategories.meta(category).labelRes).isNotEqualTo(R.string.cat_other)
        }
    }

    @Test
    fun `unknown null and case-variants fall back gracefully`() {
        assertThat(KgCategories.meta(null)).isEqualTo(KgCategories.meta("gibtsnicht"))
        assertThat(KgCategories.meta(" STOFF ")).isEqualTo(KgCategories.meta("stoff"))
        assertThat(KgCategories.colorHex(null)).isGreaterThan(0)
    }

    @Test
    fun `chip order has no duplicates`() {
        assertThat(KgCategories.chipOrder.toSet()).hasSize(KgCategories.chipOrder.size)
    }

    @Test
    fun `buildCenter collects articles mentioning the entity`() {
        val articles = listOf(
            KgArticle("Wasserartikel", "https://chemie-lernen.org/wasser/", listOf("wasser", "h2o")),
            KgArticle("Anderer", "https://chemie-lernen.org/x/", listOf("methan")),
        )
        val center = KgCategories.buildCenter(entity("Wasser", "stoff"), articles)
        assertThat(center.articles).hasSize(1)
        assertThat(center.articles[0].title).isEqualTo("Wasserartikel")
        assertThat(center.related).hasSize(3)
    }

    @Test
    fun `buildCenter is deterministic`() {
        val articles = listOf(KgArticle("A", "https://chemie-lernen.org/a/", listOf("w")))
        val e = entity("W", "konzept")
        val c1 = KgCategories.buildCenter(e, articles)
        val c2 = KgCategories.buildCenter(e, articles)
        assertThat(c1.related.map { it.name }).isEqualTo(c2.related.map { it.name })
    }
}
