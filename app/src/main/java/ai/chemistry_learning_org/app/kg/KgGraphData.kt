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

    /**
     * Small neighborhood graph for one term: the term itself plus all its
     * directly related entities (1 hop). Edges: center -> each neighbor,
     * plus edges among the neighbors (from their own relation lists) when
     * both endpoints are already part of the neighborhood. Unknown relation
     * names are skipped. Returns null for unknown centers.
     */
    fun buildEgo(graph: Graph, centerName: String): Graph? {
        val centerIdx = graph.index[centerName.lowercase()] ?: return null
        val center = graph.nodes[centerIdx]

        val localIndex = HashMap<String, Int>()
        localIndex[center.name.lowercase()] = 0
        val nodes = ArrayList<KgEntity>()
        nodes += center

        for (rel in center.related) {
            val globalIdx = graph.index[rel.name.lowercase()] ?: continue
            if (globalIdx == centerIdx) continue
            val entity = graph.nodes[globalIdx]
            if (localIndex.putIfAbsent(entity.name.lowercase(), nodes.size) == null) {
                nodes += entity
            }
        }

        val seen = HashSet<Long>()
        val edges = ArrayList<Pair<Int, Int>>()
        fun edge(a: Int, b: Int) {
            if (a == b) return
            val lo = minOf(a, b).toLong()
            val hi = maxOf(a, b).toLong()
            if (seen.add((lo shl 32) or hi)) edges += a to b
        }
        fun localOf(name: String): Int? = localIndex[name.lowercase()]

        // center spokes
        for (rel in center.related) {
            val b = localOf(rel.name) ?: continue
            edge(0, b)
        }
        // edges among neighbors (only between nodes already in the set)
        for (i in 1 until nodes.size) {
            for (rel in nodes[i].related) {
                val j = localOf(rel.name) ?: continue
                edge(i, j)
            }
        }
        return Graph(nodes, edges, localIndex, graph.articles)
    }
}
