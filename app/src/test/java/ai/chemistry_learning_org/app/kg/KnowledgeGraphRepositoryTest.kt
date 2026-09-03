package ai.chemistry_learning_org.app.kg

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Repository logic with a fake fetcher: pagination merging, cache
 * fallback on network failure, and offline-first behavior.
 */
class KnowledgeGraphRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val cacheFile: File get() = File(tmp.root, "kg-cache.json")

    private fun pageJson(offset: Int, limit: Int, total: Int, names: List<String>): String {
        val entities = names.joinToString(",") {
            """{"name":"$it","category":"stoff","relationCount":1,"relatedEntities":[]}"""
        }
        return """{"entities":[$entities],"articles":[],"total":$total,"offset":$offset,"limit":$limit}"""
    }

    @Test
    fun `fetches all pages until total is reached`() = runTest {
        val requested = mutableListOf<String>()
        val repo = KnowledgeGraphRepository(fetchJson = { url ->
            requested += url
            when (requested.size) {
                1 -> pageJson(0, 2, 5, listOf("A", "B"))
                2 -> pageJson(2, 2, 5, listOf("C", "D"))
                else -> pageJson(4, 2, 5, listOf("E"))
            }
        }, cacheFile = cacheFile)

        val result = repo.load()
        assertThat(result).isInstanceOf(KnowledgeGraphRepository.LoadResult.Success::class.java)
        val snapshot = (result as KnowledgeGraphRepository.LoadResult.Success).snapshot
        assertThat(snapshot.entities.map { it.name }).containsExactly("A", "B", "C", "D", "E")
        assertThat(requested).containsExactly(
            KnowledgeGraphRepository.apiUrl(0),
            KnowledgeGraphRepository.apiUrl(2),
            KnowledgeGraphRepository.apiUrl(4),
        )
    }

    @Test
    fun `stops when a page returns no entities even if total lies`() = runTest {
        val repo = KnowledgeGraphRepository(fetchJson = { _ ->
            """{"entities":[],"articles":[],"total":999,"offset":0,"limit":200}"""
        }, cacheFile = cacheFile)

        val result = repo.load()
        assertThat(result).isInstanceOf(KnowledgeGraphRepository.LoadResult.Unavailable::class.java)
    }

    @Test
    fun `network failure falls back to the cache`() = runTest {
        val goodRepo = KnowledgeGraphRepository(fetchJson = { _ ->
            pageJson(0, 200, 2, listOf("Wasser", "Salz"))
        }, cacheFile = cacheFile)
        goodRepo.load()

        assertThat(cacheFile.exists()).isTrue()
        val offlineRepo = KnowledgeGraphRepository(fetchJson = { _ ->
            error("network down")
        }, cacheFile = cacheFile)

        val result = offlineRepo.load()
        assertThat(result).isInstanceOf(KnowledgeGraphRepository.LoadResult.Offline::class.java)
        val snapshot = (result as KnowledgeGraphRepository.LoadResult.Offline).snapshot
        assertThat(snapshot.entities.map { it.name }).containsExactly("Wasser", "Salz")
    }

    @Test
    fun `network failure without cache is unavailable`() = runTest {
        val repo = KnowledgeGraphRepository(fetchJson = { _ ->
            error("airplane mode")
        }, cacheFile = cacheFile)
        assertThat(repo.load()).isEqualTo(KnowledgeGraphRepository.LoadResult.Unavailable)
    }

    @Test
    fun `corrupted cache is ignored not crashing`() = runTest {
        cacheFile.writeText("{ truncated garbage")
        val repo = KnowledgeGraphRepository(fetchJson = { _ ->
            error("offline")
        }, cacheFile = cacheFile)
        assertThat(repo.load()).isEqualTo(KnowledgeGraphRepository.LoadResult.Unavailable)
    }

    @Test
    fun `no cache file configured - unavailable on failure`() = runTest {
        val repo = KnowledgeGraphRepository(fetchJson = { _ ->
            error("offline")
        }, cacheFile = null)
        assertThat(repo.load()).isEqualTo(KnowledgeGraphRepository.LoadResult.Unavailable)
    }

    @Test
    fun `api url contains offset and limit`() {
        assertThat(KnowledgeGraphRepository.apiUrl(400, 200))
            .isEqualTo("https://chemie-lernen.org/api/kg-data?offset=400&limit=200")
    }
}
