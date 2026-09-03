package ai.chemistry_learning_org.app.kg

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class Graph3DLayoutTest {

    private fun node(name: String, relations: Int = 2) =
        KgEntity(name, "konzept", null, relations, emptyList())

    private fun sampleInput(): Graph3DLayout.Input {
        val nodes = listOf(
            node("Wasser", 10), node("Wasserstoff", 8), node("Sauerstoff", 9),
            node("CO2", 6), node("Methan", 5), node("Salz", 4), node("Eisen", 3),
        )
        val edges = listOf(
            0 to 1, 0 to 2, 1 to 2, 0 to 3, 3 to 4, 0 to 5, 6 to 2,
        )
        return Graph3DLayout.Input(nodes, edges)
    }

    @Test
    fun `every node gets a position`() {
        val input = sampleInput()
        val positions = Graph3DLayout.compute(input)
        assertThat(positions).hasSize(input.nodes.size)
    }

    @Test
    fun `all positions are finite`() {
        val positions = Graph3DLayout.compute(sampleInput())
        for (p in positions) {
            assertThat(p.x.isFinite()).isTrue()
            assertThat(p.y.isFinite()).isTrue()
            assertThat(p.z.isFinite()).isTrue()
        }
    }

    @Test
    fun `layout is deterministic`() {
        val a = Graph3DLayout.compute(sampleInput())
        val b = Graph3DLayout.compute(sampleInput())
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `connected nodes are closer than the average random pair`() {
        val input = sampleInput()
        val positions = Graph3DLayout.compute(input)
        val edges = input.edges
        val edgeDistances = edges.map { (a, b) -> (positions[b] - positions[a]).length() }
        val allPairs = mutableListOf<Double>()
        for (i in positions.indices) {
            for (j in i + 1 until positions.size) {
                allPairs += (positions[j] - positions[i]).length()
            }
        }
        val avgEdge = edgeDistances.average()
        val avgAll = allPairs.average()
        assertThat(avgEdge).isLessThan(avgAll)
    }

    @Test
    fun `graph does not collapse into a point`() {
        val positions = Graph3DLayout.compute(sampleInput())
        val center = positions.reduce { acc, v -> acc + v } * (1.0 / positions.size)
        val radii = positions.map { (it - center).length() }
        assertThat(radii.average()).isGreaterThan(1.0)
    }

    @Test
    fun `heavier nodes feel stronger repulsion - layout stays spread`() {
        val heavy = Graph3DLayout.Input(
            listOf(node("A", 50), node("B", 50), node("C", 50)),
            edges = emptyList(),
        )
        val positions = Graph3DLayout.compute(heavy)
        val spread = (positions[1] - positions[0]).length()
        assertThat(spread).isGreaterThan(10.0)
    }

    @Test
    fun `empty and single-node graphs are handled`() {
        assertThat(Graph3DLayout.compute(Graph3DLayout.Input(emptyList(), emptyList()))).isEmpty()
        assertThat(Graph3DLayout.compute(Graph3DLayout.Input(listOf(node("Solo")), emptyList())))
            .isEqualTo(listOf(Graph3DLayout.Vec3.ZERO))
    }

    @Test
    fun `initial positions are on a sphere and unique`() {
        val positions = Graph3DLayout.initialPositions(50)
        val radii = positions.map { it.length() }
        for (r in radii) {
            assertThat(abs(r - 120.0)).isLessThan(0.001)
        }
        assertThat(positions.toSet()).hasSize(50)
    }
}
