package dev.grenas.hello3d.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.android.filament.MaterialInstance
import io.github.sceneview.SceneScope
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.node.GeometryNode

/** Compose-observable storage for the sentence lines; [SentenceScript.play] writes into it. */
class LineBoard(lineCount: Int) : LineSink {
    val lines: SnapshotStateList<LineState> = mutableStateListOf(*Array(lineCount) { LineState() })
    override fun get(line: Int): LineState = lines[line]
    override fun set(line: Int, state: LineState) { lines[line] = state }
    fun reset() { for (i in lines.indices) lines[i] = LineState() }
}

/**
 * One line of 3D text laid out from [GlyphAtlas] advances and centred on its own width. Each visible
 * cell is a [GlyphNode]; a blinking cursor block follows the last visible glyph while typing.
 */
@Composable
fun SceneScope.TextLine3D(
    state: LineState,
    atlas: GlyphAtlas,
    plainMaterial: MaterialInstance,
    emphasisMaterial: MaterialInstance,
    cursorMaterial: MaterialInstance,
    unitScale: Float,
    position: Position,
) {
    val advances = state.cells.map { atlas.advance(it.char) }
    val total = advances.sum()
    val start = -total / 2f

    Node(position = position, scale = Scale(unitScale)) {
        var pen = start
        state.cells.forEachIndexed { index, cell ->
            val x = pen
            pen += advances[index]
            key(index) {
                val shown = cell.scramble ?: cell.char
                if (cell.visible && atlas.hasGeometry(shown)) {
                    GlyphNode(
                        atlas = atlas,
                        char = shown,
                        material = if (cell.emphasis) emphasisMaterial else plainMaterial,
                        position = Position(x = x),
                    )
                }
            }
        }
        if (state.cursor) {
            val revealed = state.cells.indexOfLast { it.visible } + 1
            val cursorX = start + advances.take(revealed).sum() + atlas.em * 0.08f
            CubeNode(
                size = Size(atlas.em * 0.09f, atlas.capHeight, atlas.em * 0.12f),
                position = Position(x = cursorX, y = atlas.capHeight / 2f),
                materialInstance = cursorMaterial,
                apply = {
                    onFrame = { nanos -> isVisible = (nanos / 420_000_000L) % 2L == 0L }
                },
            )
        }
    }
}

private const val POP_SECONDS = 0.28f

/** A single extruded glyph. Pops in on first appearance; swaps its mesh in place when [char] changes. */
@Composable
private fun SceneScope.GlyphNode(
    atlas: GlyphAtlas,
    char: Char,
    material: MaterialInstance,
    position: Position,
) {
    val node = remember(engine) {
        val mesh = atlas.mesh(char)
        val geometry = Geometry.Builder()
            .vertices(mesh.vertices)
            .indices(mesh.indices)
            .build(engine)
        GeometryNode(engine = engine, geometry = geometry, materialInstance = material).apply {
            scale = Scale(0.001f)
            val clock = FrameClock()
            onFrame = { nanos ->
                val t = clock.seconds(nanos)
                if (t >= POP_SECONDS) {
                    scale = Scale(1f)
                    onFrame = null
                } else {
                    scale = Scale(easeOutBack(t / POP_SECONDS).coerceAtLeast(0.001f))
                }
            }
        }
    }
    val shownChar = remember { mutableStateOf(char) }
    SideEffect {
        if (shownChar.value != char) {
            val mesh = atlas.mesh(char)
            if (mesh.vertices.isNotEmpty()) node.updateGeometry(mesh.vertices, listOf(mesh.indices))
            shownChar.value = char
        }
    }
    val shownMaterial = remember { mutableStateOf(material) }
    SideEffect {
        if (shownMaterial.value != material) {
            node.setMaterialInstanceAt(0, material)
            shownMaterial.value = material
        }
    }
    DisposableEffect(node, position.x, position.y, position.z) { node.position = position; onDispose {} }
    NodeLifecycle(node, content = null)
}

/** Overshooting ease: 0 → ~1.1 → 1. */
private fun easeOutBack(p: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val q = p - 1f
    return 1f + c3 * q * q * q + c1 * q * q
}

/** Seconds since the first frame it was asked about. Shared by the scene's frame-driven animations. */
internal class FrameClock {
    private var start = -1L
    fun seconds(nanos: Long): Float {
        if (start < 0) start = nanos
        return (nanos - start) / 1_000_000_000f
    }
}
