package ai.chemistry_learning_org.app.kg

import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure 3D→2D projection for the graph view: yaw/pitch rotation followed
 * by perspective division. Fully unit-testable.
 */
object Graph3DProjector {

    data class Orientation(val yaw: Double, val pitch: Double, val zoom: Double, val fov: Double = 700.0)

    data class ScreenPoint(
        val x: Float,
        val y: Float,
        /** normalized depth in [0,1] — 1 = closest to the viewer */
        val depth: Float,
        /** perspective scale factor (multiply radii with it) */
        val scale: Float,
    )

    fun rotate(v: Graph3DLayout.Vec3, o: Orientation): Graph3DLayout.Vec3 {
        val cy = cos(o.yaw); val sy = sin(o.yaw)
        val x1 = v.x * cy + v.z * sy
        val z1 = -v.x * sy + v.z * cy
        val cp = cos(o.pitch); val sp = sin(o.pitch)
        val y2 = v.y * cp - z1 * sp
        val z2 = v.y * sp + z1 * cp
        return Graph3DLayout.Vec3(x1, y2, z2)
    }

    /** Projects an already-rotated point into [0,width]x[0,height]. */
    fun projectRotated(rotated: Graph3DLayout.Vec3, o: Orientation, width: Float, height: Float): ScreenPoint {
        // Clamp the denominator so a node at/behind the camera plane (z <= -fov)
        // cannot produce infinity or a negative scale. Only bites for extreme layouts.
        val denom = (o.fov + rotated.z).coerceAtLeast(o.fov * 0.01)
        val s = o.fov / denom // z toward viewer is negative
        val cx = width / 2.0
        val cy = height / 2.0
        val x = (rotated.x * s * o.zoom + cx).toFloat()
        val y = (rotated.y * s * o.zoom + cy).toFloat()
        val depth = (1.0 - (rotated.z / 600.0)).coerceIn(0.0, 1.0)
        return ScreenPoint(x, y, depth.toFloat(), s.toFloat())
    }

    /** Rotates and projects in one step. */
    fun project(v: Graph3DLayout.Vec3, o: Orientation, width: Float, height: Float): ScreenPoint =
        projectRotated(rotate(v, o), o, width, height)
}
