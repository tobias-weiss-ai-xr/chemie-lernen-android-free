package ai.chemistry_learning_org.app.kg

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KgGraphDataTest {

    private fun entity(name: String, vararg related: String) = KgEntity(
        name = name,
        category = "konzept",
        description = null,
        relationCount = related.size,
        related = related.map { KgRelation(it, "RELATED_TO", "stoff") },
    )

    @Test
    fun `builds index and edges between known nodes`() {
        val snapshot = KgSnapshot(
            entities = listOf(entity("Wasser", "wasserstoff", "sauerstoff"), entity("Wasserstoff"), entity("Sauerstoff")),
            articles = emptyList(),
        )
        val graph = KgGraphData.build(snapshot)
        assertThat(graph.nodes).hasSize(3)
        assertThat(graph.index).containsEntry("wasser", 0)
        assertThat(graph.index).containsEntry("wasserstoff", 1)
        assertThat(graph.edges).containsExactly(0 to 1, 0 to 2)
    }

    @Test
    fun `duplicates relations to undirected edges - mutual references stay single`() {
        val snapshot = KgSnapshot(
            entities = listOf(
                entity("A", "B"),
                entity("B", "A"),
                entity("C"),
            ),
            articles = emptyList(),
        )
        val graph = KgGraphData.build(snapshot)
        assertThat(graph.edges).containsExactly(0 to 1)
    }

    @Test
    fun `relations to unknown entities are ignored`() {
        val snapshot = KgSnapshot(
            entities = listOf(entity("A", "nirgendwo")),
            articles = emptyList(),
        )
        assertThat(KgGraphData.build(snapshot).edges).isEmpty()
    }

    @Test
    fun `self references are ignored`() {
        val snapshot = KgSnapshot(entities = listOf(entity("A", "A")), articles = emptyList())
        assertThat(KgGraphData.build(snapshot).edges).isEmpty()
    }

    @Test
    fun `first duplicate name wins in index`() {
        val snapshot = KgSnapshot(
            entities = listOf(entity("X"), entity("X")),
            articles = emptyList(),
        )
        val graph = KgGraphData.build(snapshot)
        assertThat(graph.index["x"]).isEqualTo(0)
    }

    @Test
    fun `build dedupes entity nodes with the same name`() {
        // The API/pagination can return the same entity twice; the node list
        // must match the index (first occurrence wins) so the picker does not
        // show duplicate rows and LazyColumn keys stay unique.
        val snapshot = KgSnapshot(
            entities = listOf(
                entity("Wasser", "sauerstoff"),
                entity("Wasser", "sauerstoff"),
                entity("Sauerstoff", "wasser"),
            ),
            articles = emptyList(),
        )
        val graph = KgGraphData.build(snapshot)
        assertThat(graph.nodes).hasSize(2)
        assertThat(graph.nodes.map { it.name }).containsExactly("Wasser", "Sauerstoff").inOrder()
        assertThat(graph.index).hasSize(2)
        // Edges still resolve through the deduped index (Wasser<->Sauerstoff).
        assertThat(graph.edges).containsExactly(0 to 1)
    }

    @Test
    fun `buildEgo - center plus one-hop neighbors with cross edges`() {
        val snapshot = KgSnapshot(
            entities = listOf(
                entity("Wasser", "wasserstoff", "sauerstoff", "unbekannt"),
                entity("Wasserstoff", "wasser"),
                entity("Sauerstoff"),
                entity("Fremd"),
            ),
            articles = listOf(KgArticle("t", "u", emptyList())),
        )
        val full = KgGraphData.build(snapshot)
        val ego = KgGraphData.buildEgo(full, "Wasser")!!
        assertThat(ego.nodes.map { it.name }).containsExactly("Wasser", "Wasserstoff", "Sauerstoff").inOrder()
        assertThat(ego.index).containsEntry("wasser", 0)
        assertThat(ego.index).containsEntry("sauerstoff", 2)
        // Zentrum-Speichen; Kreuzkante Wasserstoff->Wasser wird dedupliziert (0-1 existiert); "unbekannt"/"Fremd" nicht drin
        assertThat(ego.edges).containsExactly(0 to 1, 0 to 2).inOrder()
        assertThat(ego.articles).hasSize(1)
    }

    @Test
    fun `buildEgo - dedupes mutual edges and self references`() {
        val snapshot = KgSnapshot(
            entities = listOf(entity("A", "b"), entity("B", "a", "a")),
            articles = emptyList(),
        )
        val ego = KgGraphData.buildEgo(KgGraphData.build(snapshot), "A")!!
        assertThat(ego.edges).containsExactly(0 to 1)
    }

    @Test
    fun `buildEgo - unknown center returns null`() {
        val full = KgGraphData.build(KgSnapshot(entities = listOf(entity("A")), articles = emptyList()))
        assertThat(KgGraphData.buildEgo(full, "Nein")).isNull()
    }

    @Test
    fun `planar layout keeps z at zero and spreads nodes`() {
        val nodes = (0 until 30).map { entity("N$it", if (it < 29) "n${it + 1}" else "n0") }
        val input = Graph3DLayout.Input(nodes, (0 until 30).map { it to (it + 1) % 30 })
        val positions = Graph3DLayout.compute(input, Graph3DLayout.Config(iterations = 120, planar = true))
        assertThat(positions).hasSize(30)
        positions.forEach { assertThat(it.z).isEqualTo(0.0) }
        // Streuung: minimale Paardistanz deutlich > 0
        var minD = Double.MAX_VALUE
        for (i in positions.indices) for (j in i + 1 until positions.size) {
            minD = minOf(minD, (positions[j] - positions[i]).length())
        }
        assertThat(minD).isGreaterThan(10.0)
    }

    @Test
    fun `planar initial positions are deterministic and disc-shaped`() {
        val a = Graph3DLayout.initialPositions(12, planar = true)
        val b = Graph3DLayout.initialPositions(12, planar = true)
        assertThat(a).isEqualTo(b)
        a.forEach { assertThat(it.z).isEqualTo(0.0) }
    }
}
