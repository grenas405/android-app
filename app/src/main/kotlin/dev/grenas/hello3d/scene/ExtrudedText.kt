package dev.grenas.hello3d.scene

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.graphics.path.PathSegment
import androidx.graphics.path.iterator
import com.google.android.filament.Engine
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.Shape
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot

/**
 * Turns a string into a watertight, extruded 3D mesh built from the font's real glyph outlines.
 *
 * Pipeline:
 *  1. Ask the Android text engine for the glyph outlines ([Paint.getTextPath]).
 *  2. Walk the path segment by segment with androidx `PathIterator`, keeping straight lines exact
 *     and flattening quadratic / cubic curves adaptively, so corners stay crisp.
 *  3. Classify closed contours into outer rings and holes by containment depth (the counters
 *     inside O, D, R, B, …).
 *  4. Triangulate front and back caps with the ear-clipping triangulator SceneView ships, then
 *     stitch side walls with hard-edged normals so the metal catches light along the bevel.
 */
object ExtrudedText {

    data class Spec(
        val text: String,
        val typeface: Typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD),
        /** Depth of the extrusion, in em units. */
        val depth: Float = 0.26f,
        /** Extra tracking between glyphs, in em units. */
        val letterSpacing: Float = 0.08f,
        /** Flattening step along curves, in font units (the text is rasterised at [EM] units). */
        val curveStep: Float = 1.25f,
        /** Where the mesh origin sits: centred on the glyph bounds, or at the pen origin/baseline. */
        val anchor: Anchor = Anchor.Center,
    )

    enum class Anchor {
        /** Origin at the centre of the ink bounds; good for a single hero word. */
        Center,
        /** Origin at the pen start on the baseline; glyphs can then be laid out by [Mesh.advance]. */
        Baseline,
    }

    class Mesh(
        val vertices: List<Geometry.Vertex>,
        val indices: List<Int>,
        /** Ink width / height of the glyph run in font units. */
        val width: Float,
        val height: Float,
        /** Pen advance of the run in font units (includes letter spacing), for laying out runs. */
        val advance: Float,
    )

    /** Font size the outlines are generated at; mesh coordinates are in these font units. */
    const val EM = 100f

    /** Pen advance of [text] in font units, measured with the same paint [build] uses. */
    fun advance(spec: Spec, text: String): Float = paintFor(spec).measureText(text)

    private fun paintFor(spec: Spec) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = spec.typeface
        textSize = EM
        letterSpacing = spec.letterSpacing
    }

    fun buildGeometry(engine: Engine, spec: Spec): Pair<Geometry, Mesh> {
        val mesh = build(spec)
        val geometry = Geometry.Builder()
            .vertices(mesh.vertices)
            .indices(mesh.indices)
            .build(engine)
        return geometry to mesh
    }

    fun build(spec: Spec): Mesh {
        val paint = paintFor(spec)
        val path = Path()
        paint.getTextPath(spec.text, 0, spec.text.length, 0f, 0f, path)
        val bounds = RectF().also { path.computeBounds(it, true) }
        val advance = paint.measureText(spec.text)
        val cx = if (spec.anchor == Anchor.Center) bounds.centerX() else 0f
        val cy = if (spec.anchor == Anchor.Center) bounds.centerY() else 0f

        // Font space is y-down; the scene is y-up. Centre the run on the origin while flipping.
        val contours = flatten(path, spec.curveStep)
            .map { contour -> contour.map { Float2(it.x - cx, -(it.y - cy)) } }
            .map { dedupe(it) }
            .filter { it.size >= 3 && abs(signedArea(it)) > 1e-3f }

        val vertices = ArrayList<Geometry.Vertex>()
        val indices = ArrayList<Int>()
        // Mesh units are font units (1 em = FONT_SIZE); the node is scaled to world size later.
        val halfDepth = spec.depth * EM / 2f

        for ((outer, holes) in classify(contours)) {
            emitCaps(outer, holes, halfDepth, vertices, indices)
            emitWalls(outer, halfDepth, vertices, indices)
            holes.forEach { emitWalls(it, halfDepth, vertices, indices) }
        }

        return Mesh(vertices, indices, bounds.width(), bounds.height(), advance)
    }

    // ── Outline flattening ────────────────────────────────────────────────────────────────────────

    private fun flatten(path: Path, step: Float): List<List<Float2>> {
        val contours = mutableListOf<List<Float2>>()
        var current = ArrayList<Float2>()

        fun finish() {
            if (current.size >= 3) contours += current
            current = ArrayList()
        }

        for (segment in path.iterator()) {
            val p = segment.points
            when (segment.type) {
                PathSegment.Type.Move -> {
                    finish()
                    current.add(Float2(p[0].x, p[0].y))
                }
                PathSegment.Type.Line -> current.add(Float2(p[1].x, p[1].y))
                PathSegment.Type.Quadratic -> {
                    val a = Float2(p[0].x, p[0].y); val b = Float2(p[1].x, p[1].y); val c = Float2(p[2].x, p[2].y)
                    val n = subdivisions(step, a, b, c)
                    for (i in 1..n) {
                        val t = i.toFloat() / n
                        val u = 1f - t
                        current.add(Float2(
                            u * u * a.x + 2f * u * t * b.x + t * t * c.x,
                            u * u * a.y + 2f * u * t * b.y + t * t * c.y
                        ))
                    }
                }
                PathSegment.Type.Cubic -> {
                    val a = Float2(p[0].x, p[0].y); val b = Float2(p[1].x, p[1].y)
                    val c = Float2(p[2].x, p[2].y); val d = Float2(p[3].x, p[3].y)
                    val n = subdivisions(step, a, b, c, d)
                    for (i in 1..n) {
                        val t = i.toFloat() / n
                        val u = 1f - t
                        current.add(Float2(
                            u * u * u * a.x + 3f * u * u * t * b.x + 3f * u * t * t * c.x + t * t * t * d.x,
                            u * u * u * a.y + 3f * u * u * t * b.y + 3f * u * t * t * c.y + t * t * t * d.y
                        ))
                    }
                }
                // Conics are converted to quadratics by the iterator's default evaluation mode.
                PathSegment.Type.Conic -> Unit
                PathSegment.Type.Close -> finish()
                PathSegment.Type.Done -> finish()
            }
        }
        finish()
        return contours
    }

    private fun subdivisions(step: Float, vararg control: Float2): Int {
        var length = 0f
        for (i in 1 until control.size) length += distance(control[i - 1], control[i])
        return ceil(length / step).toInt().coerceIn(2, 32)
    }

    private fun dedupe(points: List<Float2>): List<Float2> {
        val out = ArrayList<Float2>(points.size)
        for (p in points) if (out.isEmpty() || distance(out.last(), p) > 1e-3f) out += p
        if (out.size > 2 && distance(out.first(), out.last()) <= 1e-3f) out.removeAt(out.lastIndex)
        return out
    }

    // ── Ring classification ───────────────────────────────────────────────────────────────────────

    private class Ring(val points: List<Float2>, val area: Float)

    /** Groups contours into (outer ring, holes). Even nesting depth = solid, odd = hole. */
    private fun classify(contours: List<List<Float2>>): List<Pair<List<Float2>, List<List<Float2>>>> {
        val rings = contours.map { Ring(it, signedArea(it)) }
        val parents = rings.map { ring ->
            rings.filter { other ->
                other !== ring && abs(other.area) > abs(ring.area) && contains(other.points, ring.points[0])
            }
        }
        val holesOf = HashMap<Ring, MutableList<List<Float2>>>()
        val outers = ArrayList<Ring>()
        rings.forEachIndexed { i, ring ->
            if (parents[i].size % 2 == 0) {
                outers += ring
                holesOf.getOrPut(ring) { mutableListOf() }
            }
        }
        rings.forEachIndexed { i, ring ->
            if (parents[i].size % 2 == 1) {
                val parent = parents[i].minByOrNull { abs(it.area) } ?: return@forEachIndexed
                holesOf.getOrPut(parent) { mutableListOf() } += ring.points
            }
        }
        return outers.map { outer ->
            ensureWinding(outer.points, counterClockwise = true) to
                (holesOf[outer] ?: emptyList()).map { ensureWinding(it, counterClockwise = false) }
        }
    }

    private fun ensureWinding(points: List<Float2>, counterClockwise: Boolean): List<Float2> {
        val ccw = signedArea(points) > 0f
        return if (ccw == counterClockwise) points else points.asReversed()
    }

    private fun contains(polygon: List<Float2>, p: Float2): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val a = polygon[i]; val b = polygon[j]
            if ((a.y > p.y) != (b.y > p.y)) {
                val x = (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x
                if (p.x < x) inside = !inside
            }
            j = i
        }
        return inside
    }

    // ── Mesh emission ─────────────────────────────────────────────────────────────────────────────

    private fun emitCaps(
        outer: List<Float2>, holes: List<List<Float2>>, hz: Float,
        vertices: MutableList<Geometry.Vertex>, indices: MutableList<Int>,
    ) {
        val flat = ArrayList<Float2>(outer)
        val holeStarts = ArrayList<Int>()
        for (hole in holes) { holeStarts += flat.size; flat += hole }

        val triangles = Shape.getPolygonIndices(flat, holeStarts).flatten()
        if (triangles.isEmpty()) return

        val front = vertices.size
        val frontNormal = Direction(0f, 0f, 1f)
        flat.forEach { vertices += Geometry.Vertex(Position(it.x, it.y, hz), frontNormal) }
        val back = vertices.size
        val backNormal = Direction(0f, 0f, -1f)
        flat.forEach { vertices += Geometry.Vertex(Position(it.x, it.y, -hz), backNormal) }

        var t = 0
        while (t + 2 < triangles.size) {
            val a = triangles[t]; val b = triangles[t + 1]; val c = triangles[t + 2]
            val ccw = signedArea(flat[a], flat[b], flat[c]) > 0f
            if (ccw) {
                indices += front + a; indices += front + b; indices += front + c
                indices += back + a; indices += back + c; indices += back + b
            } else {
                indices += front + a; indices += front + c; indices += front + b
                indices += back + a; indices += back + b; indices += back + c
            }
            t += 3
        }
    }

    /** Walls expect outer rings CCW and holes CW so the outward normal formula is uniform. */
    private fun emitWalls(
        ring: List<Float2>, hz: Float,
        vertices: MutableList<Geometry.Vertex>, indices: MutableList<Int>,
    ) {
        for (i in ring.indices) {
            val p0 = ring[i]
            val p1 = ring[(i + 1) % ring.size]
            val dx = p1.x - p0.x
            val dy = p1.y - p0.y
            if (hypot(dx, dy) < 1e-4f) continue
            val n = normalize(Float3(dy, -dx, 0f))
            val base = vertices.size
            vertices += Geometry.Vertex(Position(p0.x, p0.y, hz), n)   // a
            vertices += Geometry.Vertex(Position(p1.x, p1.y, hz), n)   // b
            vertices += Geometry.Vertex(Position(p1.x, p1.y, -hz), n)  // c
            vertices += Geometry.Vertex(Position(p0.x, p0.y, -hz), n)  // d
            indices += base; indices += base + 3; indices += base + 2
            indices += base; indices += base + 2; indices += base + 1
        }
    }

    // ── Small geometry helpers ────────────────────────────────────────────────────────────────────

    private fun distance(a: Float2, b: Float2) = hypot(a.x - b.x, a.y - b.y)

    private fun signedArea(points: List<Float2>): Float {
        var area = 0f
        var j = points.lastIndex
        for (i in points.indices) {
            area += (points[j].x * points[i].y) - (points[i].x * points[j].y)
            j = i
        }
        return area / 2f
    }

    private fun signedArea(a: Float2, b: Float2, c: Float2) =
        ((b.x - a.x) * (c.y - a.y) - (c.x - a.x) * (b.y - a.y)) / 2f
}
