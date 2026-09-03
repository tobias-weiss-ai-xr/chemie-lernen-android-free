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
}
