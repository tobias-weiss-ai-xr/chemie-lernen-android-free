package ai.chemistry_learning_org.app.kg

import kotlin.math.sqrt

/**
 * Deterministic force-directed 3D layout for the knowledge graph.
 *
 * Pure JVM — no Android deps. The simulation runs to a fixed iteration
 * budget (no animation loop): repulsion between all node pairs, springs
 * along graph edges, gravity toward the origin. The result is a static
 * position map the UI can rotate/zoom cheaply.
 */
object Graph3DLayout {

    data class Vec3(val x: Double, val y: Double, val z: Double) {
        operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
        operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
        operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
        fun length(): Double = sqrt(x * x + y * y + z * z)

        companion object {
            val ZERO = Vec3(0.0, 0.0, 0.0)
        }
    }

    data class Input(
        val nodes: List<KgEntity>,
        val edges: List<Pair<Int, Int>>, // index pairs into nodes
    )

    data class Config(
        val iterations: Int = 220,
        val repulsion: Double = 2600.0,
        val springLength: Double = 34.0,
        val springStrength: Double = 0.045,
        val gravity: Double = 0.028,
        val damping: Double = 0.86,
        val dt: Double = 0.016,
        /** Planar mode: forces act in the x/y plane, z is forced to 0. */
        val planar: Boolean = false,
    )

    /** Deterministic initial placement: Fibonacci sphere (3D) or disc (planar). */
    fun initialPositions(count: Int, radius: Double = 120.0, planar: Boolean = false): List<Vec3> {
        require(count >= 0)
        if (count == 0) return emptyList()
        if (planar) {
            // deterministic sunflower (Vogel) disc
            val golden = Math.PI * (3.0 - sqrt(5.0))
            return (0 until count).map { i ->
                val r = radius * sqrt((i + 0.5) / count)
                val theta = golden * i
                Vec3(Math.cos(theta) * r, Math.sin(theta) * r, 0.0)
            }
        }
        val golden = Math.PI * (3.0 - sqrt(5.0))
        return (0 until count).map { i ->
            val y = if (count == 1) 0.0 else 1.0 - (2.0 * i) / (count - 1)
            val r = sqrt((1.0 - y * y).coerceAtLeast(0.0))
            val theta = golden * i
            Vec3(Math.cos(theta) * r * radius, y * radius, Math.sin(theta) * r * radius)
        }
    }

    fun compute(input: Input, config: Config = Config()): List<Vec3> {
        val n = input.nodes.size
        if (n == 0) return emptyList()
        if (n == 1) return listOf(Vec3.ZERO)

        val pos = initialPositions(n, planar = config.planar).toMutableList()
        val vel = MutableList(n) { Vec3.ZERO }
        val weights = DoubleArray(n) { sqrt(input.nodes[it].relationCount.coerceAtLeast(1).toDouble()) }

        repeat(config.iterations) {
            val forces = MutableList(n) { Vec3.ZERO }

            // pairwise repulsion (O(n^2) — fine for ~600 nodes x fixed budget)
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    val d = pos[j] - pos[i]
                    val dist = d.length().coerceAtLeast(1.0)
                    val strength = config.repulsion * weights[i] * weights[j] / (dist * dist)
                    val unit = d * (1.0 / dist)
                    forces[i] -= unit * strength
                    forces[j] += unit * strength
                }
            }

            // edge springs
            for ((a, b) in input.edges) {
                if (a == b || a !in 0 until n || b !in 0 until n) continue
                val d = pos[b] - pos[a]
                val dist = d.length().coerceAtLeast(1.0)
                val f = (dist - config.springLength) * config.springStrength
                val unit = d * (1.0 / dist)
                forces[a] += unit * f
                forces[b] -= unit * f
            }

            // gravity + integrate
            for (i in 0 until n) {
                val toCenter = Vec3.ZERO - pos[i]
                val f = forces[i] + toCenter * (config.gravity * weights[i])
                vel[i] = (vel[i] + f * config.dt) * config.damping
                pos[i] += vel[i] * config.dt
                if (config.planar) pos[i] = Vec3(pos[i].x, pos[i].y, 0.0)
            }
        }
        return pos
    }
}
