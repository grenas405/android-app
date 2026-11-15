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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
                    if (last != 0L) rain.step((now - last) / 1_000_000_000f)
                    last = now
                    frame++
                }
            }
        }
    }

    val head = pack.accentAlt
    val trail = pack.accent
    Canvas(modifier = modifier) {
        @Suppress("UNUSED_EXPRESSION") frame // read so the canvas redraws every tick
        val columns = ceil(size.width / cell).toInt()
        val rows = ceil(size.height / cell).toInt() + 1
        rain.resize(columns, rows)
        paint.textSize = cell * 0.82f
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            rain.forEachGlyph { column, row, char, brightness, isHead ->
                val color = if (isHead) head else trail
                paint.color = color.copy(alpha = color.alpha * brightness).toArgb()
                native.drawText(
                    char.toString(),
                    column * cell + cell / 2f,
                    row * cell + cell * 0.85f,
                    paint,
                )
            }
        }
    }
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

    fun step(dt: Float) {
        if (columns == 0) return
        val clamped = dt.coerceIn(0f, 0.05f)
        for (c in 0 until columns) {
            if (!active[c]) {
                if (random.nextFloat() < clamped * 0.15f) active[c] = true else continue
            }
            heads[c] += speeds[c] * clamped
            if (heads[c] - lengths[c] > rows) {
                heads[c] = -random.nextFloat() * 6f
                speeds[c] = 4f + random.nextFloat() * 9f
                lengths[c] = 6 + random.nextInt(14)
                active[c] = random.nextFloat() < fillRatio
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
        }
    }

    inline fun forEachGlyph(block: (column: Int, row: Int, char: Char, brightness: Float, isHead: Boolean) -> Unit) {
        for (c in 0 until columns) {
            if (!active[c]) continue
            val head = heads[c].toInt()
            val length = lengths[c]
            for (i in 0 until length) {
                val row = head - i
                if (row < 0 || row >= rows) continue
                val fade = 1f - i.toFloat() / length
                block(c, row, glyphs[c][row], baseAlpha * fade * fade + (if (i == 0) 0.3f else 0f), i == 0)
            }
        }
    }

    private fun randomGlyph(): Char = alphabet[random.nextInt(alphabet.length)]
}
