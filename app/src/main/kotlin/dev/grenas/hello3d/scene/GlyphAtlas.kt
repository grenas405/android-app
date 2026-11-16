package dev.grenas.hello3d.scene

import android.graphics.Typeface
import com.google.android.filament.Engine
import dev.grenas.hello3d.ui.theme.GlyphTypeface
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.node.GeometryNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** The bold system typeface a theme's 3D glyphs are extruded from. */
fun GlyphTypeface.toTypeface(): Typeface = when (this) {
    GlyphTypeface.Mono -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    GlyphTypeface.Sans -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

/**
 * Caches one extruded mesh per character (CPU data only) so 3D text lines can be laid out by pen
 * advance and glyph nodes can swap characters cheaply during a scramble.
 *
 * Meshes are anchored at the pen origin on the baseline ([ExtrudedText.Anchor.Baseline]) and
 * measured in font units ([ExtrudedText.EM] per em). A space has no vertices but a valid advance.
 *
 * Atlases are shared per typeface for the life of the process ([of]): tessellating a glyph is the
 * expensive part, and it does not need repeating when the scene is rebuilt after a visit to
 * Settings or a theme change.
 */
class GlyphAtlas(
    typeface: Typeface,
    depth: Float = 0.2f,
    letterSpacing: Float = 0.04f,
) {
    private val spec = ExtrudedText.Spec(
        text = "",
        typeface = typeface,
        depth = depth,
        letterSpacing = letterSpacing,
        // Sentence glyphs are a few dozen pixels tall, so curves need far fewer segments than the
        // hero words. This cuts their vertex count (and the ear-clipping time) several times over.
        curveStep = 5f,
        anchor = ExtrudedText.Anchor.Baseline,
    )
    private val meshes = ConcurrentHashMap<Char, ExtrudedText.Mesh>()

    val em: Float = ExtrudedText.EM

    /** Height of a capital H in font units; used to size the cursor and vertical spacing. */
    val capHeight: Float by lazy { mesh('H').height.takeIf { it > 0f } ?: em * 0.7f }

    val lineHeight: Float get() = em * 1.25f

    fun mesh(char: Char): ExtrudedText.Mesh =
        meshes[char] ?: ExtrudedText.build(spec.copy(text = char.toString())).also { meshes.putIfAbsent(char, it) }

    fun advance(char: Char): Float = mesh(char).advance

    fun hasGeometry(char: Char): Boolean = mesh(char).vertices.isNotEmpty()

    fun lineWidth(text: String): Float = text.fold(0f) { total, c -> total + advance(c) }

    /** Builds the meshes for [chars] off the main thread so typing and scrambling never stall a frame. */
    suspend fun warmUp(chars: Iterable<Char>) = withContext(Dispatchers.Default) {
        chars.distinct().forEach { mesh(it) }
    }

    companion object {
        private val shared = ConcurrentHashMap<GlyphTypeface, GlyphAtlas>()

        fun of(typeface: GlyphTypeface): GlyphAtlas = shared.getOrPut(typeface) { GlyphAtlas(typeface.toTypeface()) }
    }
}

/**
 * GPU side of a [GlyphAtlas]: one Filament vertex/index buffer pair per character, uploaded the
 * first time the character is shown and then shared by every glyph node that draws it. A scramble
 * tick therefore only re-points nodes at existing buffers instead of uploading a mesh per glyph.
 *
 * SceneView frees a geometry when the last node bound to it goes away, so each cached geometry is
 * also bound to a hidden node that is never added to the scene. That keeps the buffers alive while
 * no glyph happens to use the character. [destroy] releases those hidden nodes; the buffers follow
 * as soon as the visible glyph nodes are gone. Main-thread only, like the rest of the scene graph.
 */
class GlyphGeometries(private val engine: Engine, val atlas: GlyphAtlas) {

    private class Entry(val geometry: Geometry, val keepAlive: GeometryNode)

    private val entries = HashMap<Char, Entry>()
    private var destroyed = false

    /** The shared geometry for [char], or null when it has no outline (a space, a missing glyph). */
    operator fun get(char: Char): Geometry? {
        entries[char]?.let { return it.geometry }
        val mesh = atlas.mesh(char)
        if (mesh.vertices.isEmpty()) return null
        val geometry = Geometry.Builder()
            .vertices(mesh.vertices)
            .indices(mesh.indices)
            .build(engine)
        // A straggler asking after destroy() gets an uncached geometry that dies with its node.
        if (!destroyed) entries[char] = Entry(geometry, GeometryNode(engine = engine, geometry = geometry))
        return geometry
    }

    fun require(char: Char): Geometry = checkNotNull(get(char)) { "No outline for '$char'" }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        entries.values.forEach { it.keepAlive.destroy() }
        entries.clear()
    }
}
