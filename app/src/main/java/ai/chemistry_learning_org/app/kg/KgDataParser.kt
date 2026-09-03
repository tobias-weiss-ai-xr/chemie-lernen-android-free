package ai.chemistry_learning_org.app.kg

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses /api/kg-data responses. Pure JVM (framework org.json), fully
 * unit-tested — the repository only feeds it raw strings.
 */
object KgDataParser {

    fun parse(body: String): KgPage {
        val root = JSONObject(body)
        return KgPage(
            entities = parseEntities(root.optJSONArray("entities")),
            articles = parseArticles(root.optJSONArray("articles")),
            total = root.optInt("total", 0),
            offset = root.optInt("offset", 0),
            limit = root.optInt("limit", 0),
        )
    }

    private fun parseEntities(array: JSONArray?): List<KgEntity> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) return@mapNotNull null
            KgEntity(
                name = name,
                category = obj.optString("category", "").trim().ifEmpty { null },
                description = obj.optString("description", "").trim().ifEmpty { null },
                relationCount = obj.optInt("relationCount", 0),
                related = parseRelations(obj.optJSONArray("relatedEntities")),
            )
        }
    }

    private fun parseRelations(array: JSONArray?): List<KgRelation> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) return@mapNotNull null
            KgRelation(
                name = name,
                relType = obj.optString("relType", "").trim(),
                category = obj.optString("category", "").trim().ifEmpty { null },
            )
        }
    }

    private fun parseArticles(array: JSONArray?): List<KgArticle> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val title = obj.optString("title", "").trim()
            val url = obj.optString("url", "").trim()
            if (title.isEmpty() || url.isEmpty()) return@mapNotNull null
            val entityArray = obj.optJSONArray("entities")
            val entities = (0 until (entityArray?.length() ?: 0)).mapNotNull { i ->
                entityArray?.optString(i)?.trim()?.ifEmpty { null }
            }
            KgArticle(
                title = title,
                url = url,
                entities = entities,
            )
        }
    }
}
