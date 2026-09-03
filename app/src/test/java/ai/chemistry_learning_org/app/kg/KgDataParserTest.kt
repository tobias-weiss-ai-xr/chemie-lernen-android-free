package ai.chemistry_learning_org.app.kg

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Parser tests against a REAL captured API response
 * (kg-sample.json = https://chemie-lernen.org/api/kg-data?limit=50)
 * plus synthetic edge cases (null fields, missing arrays, garbage).
 */
class KgDataParserTest {

    private val sample: String by lazy {
        javaClass.getResourceAsStream("/kg-sample.json")!!
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    @Test
    fun `parses the real api sample`() {
        val page = KgDataParser.parse(sample)
        assertThat(page.entities).isNotEmpty()
        assertThat(page.articles).isNotEmpty()
        assertThat(page.total).isEqualTo(545)
        assertThat(page.limit).isEqualTo(50)
        assertThat(page.offset).isEqualTo(0)
    }

    @Test
    fun `real entities carry name category and related list`() {
        val page = KgDataParser.parse(sample)
        val withRelations = page.entities.filter { it.relationCount > 1 }
        assertThat(withRelations).isNotEmpty()
        val first = withRelations.first()
        assertThat(first.category).isNotNull()
        assertThat(first.related).isNotEmpty()
        val rel = first.related.first()
        assertThat(rel.name).isNotEmpty()
    }

    @Test
    fun `real articles map entities to platform urls`() {
        val page = KgDataParser.parse(sample)
        val withEntities = page.articles.filter { it.entities.isNotEmpty() }
        assertThat(withEntities).isNotEmpty()
        val article = withEntities.first()
        assertThat(article.url).startsWith("https://chemie-lernen.org/")
        assertThat(article.title).isNotEmpty()
    }

    @Test
    fun `null descriptions are normalized to null not empty string`() {
        val page = KgDataParser.parse(sample)
        val nullDesc = page.entities.firstOrNull { it.description == null }
        assertThat(nullDesc).isNotNull()
    }

    @Test
    fun `handles truncated json by throwing (repo treats as network failure)`() {
        val cut = sample.substring(0, sample.length / 2)
        val thrown = runCatching { KgDataParser.parse(cut) }
        assertThat(thrown.isFailure).isTrue()
    }

    @Test
    fun `empty json object yields empty page`() {
        val page = KgDataParser.parse("{}")
        assertThat(page.entities).isEmpty()
        assertThat(page.articles).isEmpty()
        assertThat(page.total).isEqualTo(0)
    }

    @Test
    fun `entities with empty names are dropped`() {
        val json = """
            {"entities":[
              {"name":"  ","category":"stoff"},
              {"name":"Wasser","category":"stoff","relationCount":2,
               "relatedEntities":[{"name":"","relType":"X"},{"name":"H2","relType":"PART_OF"}]}
            ]}
        """.trimIndent()
        val page = KgDataParser.parse(json)
        assertThat(page.entities).hasSize(1)
        assertThat(page.entities[0].name).isEqualTo("Wasser")
        // empty-name relations are dropped, too
        assertThat(page.entities[0].related).hasSize(1)
        assertThat(page.entities[0].related[0].name).isEqualTo("H2")
    }

    @Test
    fun `articles missing title or url are dropped`() {
        val json = """
            {"articles":[
              {"title":"Ohne URL","url":""},
              {"title":"","url":"https://chemie-lernen.org/x/"},
              {"title":"Ganz","url":"https://chemie-lernen.org/ganz/","entities":["A"," "]}
            ]}
        """.trimIndent()
        val page = KgDataParser.parse(json)
        assertThat(page.articles).hasSize(1)
        assertThat(page.articles[0].entities).containsExactly("A")
    }

    @Test
    fun `category is trimmed but casing preserved (KgCategories lowercases for lookup)`() {
        val json = """{"entities":[{"name":"X","category":" Stoff "}]}"""
        val page = KgDataParser.parse(json)
        assertThat(page.entities[0].category).isEqualTo("Stoff")
        // category meta lookup is case-insensitive
        assertThat(KgCategories.meta(page.entities[0].category).labelRes)
            .isEqualTo(KgCategories.meta("stoff").labelRes)
    }
}
