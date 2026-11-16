package dev.grenas.hello3d.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.grenas.hello3d.data.RainIntensity
import dev.grenas.hello3d.ui.theme.ThemePack
import kotlin.math.ceil
import kotlin.random.Random

/**
 * Falling glyph columns behind the 3D viewport, drawn with a single native [Paint] for speed.
 * The alphabet, colours and density come from the theme pack and the intensity setting. The frame
 * loop runs only while the host is resumed.
 *
 * The rain moves in whole rows, so most frames on a fast display change nothing: the canvas is only
 * invalidated when a head crosses a row or a glyph mutates. It draws into its own layer, so those
 * redraws do not re-record the backdrop around it, and the draw loop allocates nothing per glyph.
 */
@Composable
fun MatrixRain(pack: ThemePack, intensity: RainIntensity, modifier: Modifier = Modifier) {
    if (intensity == RainIntensity.Off) return

    val density = LocalDensity.current
    val cell = with(density) { 18.dp.toPx() }
    val rain = remember(pack.glyphAlphabet, intensity) { RainField(pack.glyphAlphabet, intensity, Random(42)) }
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
    }
    var frame by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(rain, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    val changed = last == 0L || rain.step((now - last) / 1_000_000_000f)
                    last = now
                    if (changed) frame++
                }
            }
        }
    }

    val head = pack.accentAlt.toArgb()
    val trail = pack.accent.toArgb()
    Canvas(modifier = modifier.graphicsLayer()) {
        @Suppress("UNUSED_EXPRESSION") frame // read so the canvas redraws when the rain changed
        val columns = ceil(size.width / cell).toInt()
        val rows = ceil(size.height / cell).toInt() + 1
        rain.resize(columns, rows)
        paint.textSize = cell * 0.82f
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            rain.forEachGlyph { column, glyphs, row, brightness, isHead ->
                paint.color = withAlpha(if (isHead) head else trail, brightness)
                native.drawText(glyphs, row, 1, column * cell + cell / 2f, row * cell + cell * 0.85f, paint)
            }
        }
    }
}

/** [argb] with its alpha scaled by [factor]. Plain integer maths: no colour objects in the draw loop. */
private fun withAlpha(argb: Int, factor: Float): Int {
    val alpha = ((argb ushr 24) * factor).toInt().coerceIn(0, 255)
    return (argb and 0x00FFFFFF) or (alpha shl 24)
}

/** Simulation state for the rain, kept outside composition and mutated once per frame. */
private class RainField(private val alphabet: String, intensity: RainIntensity, private val random: Random) {

    private val fillRatio = if (intensity == RainIntensity.Full) 0.9f else 0.45f
    private val baseAlpha = if (intensity == RainIntensity.Full) 0.7f else 0.34f

    private var columns = 0
    private var rows = 0
    private var heads = FloatArray(0)      // head row (fractional)
    private var speeds = FloatArray(0)     // rows per second
    private var lengths = IntArray(0)      // trail length in rows
    private var active = BooleanArray(0)
    private var glyphs = Array(0) { CharArray(0) }
    private var mutateTimer = 0f

    fun resize(newColumns: Int, newRows: Int) {
        if (newColumns == columns && newRows == rows) return
        columns = newColumns
        rows = newRows
        heads = FloatArray(columns) { -random.nextFloat() * rows }
        speeds = FloatArray(columns) { 4f + random.nextFloat() * 9f }
        lengths = IntArray(columns) { 6 + random.nextInt(14) }
        active = BooleanArray(columns) { random.nextFloat() < fillRatio }
        glyphs = Array(columns) { CharArray(rows) { randomGlyph() } }
    }

    /** Advances the simulation. Returns true when something visible changed and a redraw is due. */
    fun step(dt: Float): Boolean {
        if (columns == 0) return false
        var changed = false
        val clamped = dt.coerceIn(0f, 0.05f)
        for (c in 0 until columns) {
            if (!active[c]) {
                if (random.nextFloat() < clamped * 0.15f) active[c] = true else continue
            }
            val before = heads[c].toInt()
            heads[c] += speeds[c] * clamped
            if (heads[c] - lengths[c] > rows) {
                heads[c] = -random.nextFloat() * 6f
                speeds[c] = 4f + random.nextFloat() * 9f
                lengths[c] = 6 + random.nextInt(14)
                active[c] = random.nextFloat() < fillRatio
                changed = true
            } else if (heads[c].toInt() != before) {
                changed = true
            }
        }
        mutateTimer += clamped
        if (mutateTimer > 0.09f) {
            mutateTimer = 0f
            repeat(columns) {
                val c = random.nextInt(columns)
                val r = random.nextInt(rows)
                glyphs[c][r] = randomGlyph()
            }
            changed = true
        }
        return changed
    }

    /** Visits every lit cell. [block] receives the column's glyph array and the row to read from it. */
    inline fun forEachGlyph(block: (column: Int, glyphs: CharArray, row: Int, brightness: Float, isHead: Boolean) -> Unit) {
        for (c in 0 until columns) {
            if (!active[c]) continue
            val head = heads[c].toInt()
            val length = lengths[c]
            val column = glyphs[c]
            for (i in 0 until length) {
                val row = head - i
                if (row < 0 || row >= rows) continue
                val fade = 1f - i.toFloat() / length
                block(c, column, row, baseAlpha * fade * fade + (if (i == 0) 0.3f else 0f), i == 0)
            }
        }
    }

    private fun randomGlyph(): Char = alphabet[random.nextInt(alphabet.length)]
}
