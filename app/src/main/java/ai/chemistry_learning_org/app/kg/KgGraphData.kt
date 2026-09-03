package ai.chemistry_learning_org.app.kg

/**
 * Prepares a [KgSnapshot] for 3D layout: node list + deduplicated undirected
 * edges (only between nodes that both exist in the snapshot). Pure JVM.
 */
object KgGraphData {

    data class Graph(
        val nodes: List<KgEntity>,
        val edges: List<Pair<Int, Int>>,
        /** lowercase node name -> index */
        val index: Map<String, Int>,
        val articles: List<KgArticle>,
    )

    fun build(snapshot: KgSnapshot): Graph {
        val nodes = snapshot.entities
        val index = HashMap<String, Int>(nodes.size)
        nodes.forEachIndexed { i, e ->
            index.putIfAbsent(e.name.lowercase(), i)
        }
        val seen = HashSet<Long>()
        val edges = ArrayList<Pair<Int, Int>>()
        nodes.forEachIndexed { i, entity ->
            for (rel in entity.related) {
                val j = index[rel.name.lowercase()] ?: continue
                if (j == i) continue
                val a = minOf(i, j).toLong()
                val b = maxOf(i, j).toLong()
                val key = (a shl 32) or b
                if (seen.add(key)) edges += i to j
            }
        }
        return Graph(nodes, edges, index, snapshot.articles)
    }
}
