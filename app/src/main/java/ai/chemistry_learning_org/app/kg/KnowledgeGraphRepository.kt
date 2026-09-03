package ai.chemistry_learning_org.app.kg

import ai.chemistry_learning_org.app.web.SiteUrls
import java.io.File

/**
 * Loads the full knowledge graph from the platform kg-data API with
 * pagination and keeps the last successful snapshot in [cacheFile] for
 * offline use.
 *
 * Network access is injected as [fetchJson] (URL -> body, throws on
 * failure) so the whole load/fallback logic is pure and unit-testable.
 */
class KnowledgeGraphRepository(
    private val fetchJson: suspend (String) -> String,
    private val cacheFile: File?,
    private val maxPages: Int = 5,
) {

    sealed interface LoadResult {
        /** Fresh data from the network. */
        data class Success(val snapshot: KgSnapshot) : LoadResult

        /** Network failed, but a cached snapshot was served. */
        data class Offline(val snapshot: KgSnapshot) : LoadResult

        /** Network failed and nothing is cached. */
        data object Unavailable : LoadResult
    }

    suspend fun load(): LoadResult {
        return try {
            val snapshot = fetchAllPages()
            if (snapshot.entities.isEmpty()) {
                fallbackOrUnavailable()
            } else {
                cacheFile?.let { writeCache(it, snapshot) }
                LoadResult.Success(snapshot)
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            fallbackOrUnavailable()
        }
    }

    private suspend fun fallbackOrUnavailable(): LoadResult {
        val cached = readCache()
        return if (cached != null) LoadResult.Offline(cached) else LoadResult.Unavailable
    }

    private suspend fun fetchAllPages(): KgSnapshot {
        val pages = mutableListOf<KgPage>()
        var offset = 0
        repeat(maxPages) {
            val page = KgDataParser.parse(fetchJson(apiUrl(offset)))
            pages += page
            val fetched = page.entities.size
            val next = page.offset + fetched
            if (fetched == 0 || next >= page.total) return KgSnapshot.fromPages(pages)
            offset = next
        }
        return KgSnapshot.fromPages(pages)
    }

    private fun readCache(): KgSnapshot? {
        val file = cacheFile ?: return null
        if (!file.exists()) return null
        return try {
            val snapshot = cacheFormat().fromString(file.readText())
            if (snapshot.entities.isEmpty()) null else snapshot
        } catch (t: Throwable) {
            null
        }
    }

    private fun writeCache(file: File, snapshot: KgSnapshot) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(cacheFormat().toString(snapshot))
        } catch (t: Throwable) {
            // cache write failures are non-fatal
        }
    }

    private fun cacheFormat(): CacheFormat = CacheFormat

    companion object {
        fun apiUrl(offset: Int, limit: Int = 200): String = SiteUrls.apiKgData(offset, limit)
    }
}

/** Cache serialization: single JSON page containing all merged data. */
private object CacheFormat {
    fun toString(snapshot: KgSnapshot): String {
        val root = org.json.JSONObject()
        root.put("total", snapshot.entities.size)
        root.put("offset", 0)
        root.put("limit", snapshot.entities.size)
        root.put(
            "entities",
            org.json.JSONArray().apply {
                snapshot.entities.forEach { e ->
                    put(
                        org.json.JSONObject()
                            .put("name", e.name)
                            .put("category", e.category ?: "")
                            .put("description", e.description ?: "")
                            .put("relationCount", e.relationCount)
                            .put(
                                "relatedEntities",
                                org.json.JSONArray().apply {
                                    e.related.forEach { r ->
                                        put(
                                            org.json.JSONObject()
                                                .put("name", r.name)
                                                .put("relType", r.relType)
                                                .put("category", r.category ?: ""),
                                        )
                                    }
                                },
                            ),
                    )
                }
            },
        )
        root.put(
            "articles",
            org.json.JSONArray().apply {
                snapshot.articles.forEach { a ->
                    put(
                        org.json.JSONObject()
                            .put("title", a.title)
                            .put("url", a.url)
                            .put("type", "page")
                            .put("entities", org.json.JSONArray(a.entities)),
                    )
                }
            },
        )
        return root.toString()
    }

    fun fromString(text: String): KgSnapshot {
        val page = KgDataParser.parse(text)
        return KgSnapshot(page.entities, page.articles)
    }
}
