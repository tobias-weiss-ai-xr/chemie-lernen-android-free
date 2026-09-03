package ai.chemistry_learning_org.app.kg

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.sqrt

class Graph3DProjectorTest {

    private val orientation = Graph3DProjector.Orientation(yaw = 0.0, pitch = 0.0, zoom = 1.0)

    @Test
    fun `origin projects to screen center`() {
        val p = Graph3DProjector.project(Graph3DLayout.Vec3(0.0, 0.0, 0.0), orientation, 1000f, 800f)
        assertThat(p.x).isWithin(0.01f).of(500f)
        assertThat(p.y).isWithin(0.01f).of(400f)
        assertThat(p.depth).isWithin(0.001f).of(1f)
    }

    @Test
    fun `positive x moves right and positive y moves down (screen coords)`() {
        val right = Graph3DProjector.project(Graph3DLayout.Vec3(100.0, 0.0, 0.0), orientation, 1000f, 800f)
        val down = Graph3DProjector.project(Graph3DLayout.Vec3(0.0, 100.0, 0.0), orientation, 1000f, 800f)
        assertThat(right.x).isGreaterThan(500f)
        assertThat(down.y).isGreaterThan(400f)
    }

    @Test
    fun `closer to viewer means larger scale`() {
        val near = Graph3DProjector.project(Graph3DLayout.Vec3(0.0, 0.0, -200.0), orientation, 1000f, 800f)
        val far = Graph3DProjector.project(Graph3DLayout.Vec3(0.0, 0.0, 200.0), orientation, 1000f, 800f)
        assertThat(near.scale).isGreaterThan(far.scale)
        assertThat(near.depth).isGreaterThan(far.depth)
    }

    @Test
    fun `yaw rotation moves x toward z`() {
        val o = orientation.copy(yaw = Math.PI / 2)
        val rotated = Graph3DProjector.rotate(Graph3DLayout.Vec3(100.0, 0.0, 0.0), o)
        assertThat(rotated.x).isWithin(0.001).of(0.0)
        assertThat(rotated.z).isWithin(0.001).of(-100.0)
    }

    @Test
    fun `rotation preserves the radius (rigid transform)`() {
        val v = Graph3DLayout.Vec3(60.0, 45.0, 30.0)
        val o = Graph3DProjector.Orientation(yaw = 0.7, pitch = -0.4, zoom = 1.0)
        val r = Graph3DProjector.rotate(v, o)
        assertThat(r.length()).isWithin(0.001).of(v.length())
    }

    @Test
    fun `zoom scales the projected distance from center`() {
        val v = Graph3DLayout.Vec3(100.0, 0.0, 0.0)
        val p1 = Graph3DProjector.project(v, orientation, 1000f, 800f)
        val p2 = Graph3DProjector.project(v, orientation.copy(zoom = 2.0), 1000f, 800f)
        val d1 = p1.x - 500f
        val d2 = p2.x - 500f
        assertThat(d2).isWithin(0.01f).of(d1 * 2f)
    }

    @Test
    fun `projection handles extreme zoom without crashing`() {
        val o = orientation.copy(zoom = 40.0, fov = 1.0)
        val p = Graph3DProjector.project(Graph3DLayout.Vec3(100.0, 0.0, 599.0), o, 1000f, 800f)
        assertThat(p.x.isFinite()).isTrue()
        assertThat(p.y.isFinite()).isTrue()
        assertThat(p.depth).isAtLeast(0f)
        assertThat(p.depth).isAtMost(1f)
    }
}
