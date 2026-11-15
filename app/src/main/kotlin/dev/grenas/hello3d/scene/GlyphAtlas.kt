package dev.grenas.hello3d.scene

import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Caches one extruded mesh per character (CPU data only) so 3D text lines can be laid out by pen
 * advance and glyph nodes can swap characters cheaply during a scramble.
 *
 * Meshes are anchored at the pen origin on the baseline ([ExtrudedText.Anchor.Baseline]) and
 * measured in font units ([ExtrudedText.EM] per em). A space has no vertices but a valid advance.
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
}
