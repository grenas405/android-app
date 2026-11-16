package dev.grenas.hello3d.scene

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import dev.grenas.hello3d.data.LanguageLogos
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.node.ImageNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Official language logos prepared for the 3D scene. Each logo is drawn, unmodified, on a light
 * rounded plate: several marks (Rust, Solidity, Crystal, Bash) are black or near-black and would
 * vanish against the dark themes otherwise.
 */
object LogoBadges {

    /** Edge of the square badge bitmap, in pixels. Matches the bundled logo artwork. */
    private const val SIZE = 256
    private const val PLATE_COLOR = 0xFFE8ECF2.toInt()

    /** Decoded badges by drawable id. Small and bounded, so textures can be re-used across visits. */
    private val cache = LruCache<Int, Bitmap>(24)

    /**
     * Decodes the badge for every language in [languages] that has a logo, off the main thread.
     * Keys are the trimmed language names, as the sentence shows them.
     */
    suspend fun load(context: Context, languages: List<String>): Map<String, Bitmap> =
        withContext(Dispatchers.Default) {
            buildMap {
                for (language in languages) {
                    val name = language.trim()
                    val resId = LanguageLogos.forLanguage(name) ?: continue
                    val badge = cache.get(resId) ?: render(context, resId)?.also { cache.put(resId, it) }
                    if (badge != null) put(name, badge)
                }
            }
        }

    private fun render(context: Context, resId: Int): Bitmap? {
        val logo = BitmapFactory.decodeResource(context.resources, resId) ?: return null
        val badge = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(badge)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        // A transparent 2 px rim keeps the plate edge anti-aliased once the quad is filtered.
        val plate = RectF(2f, 2f, SIZE - 2f, SIZE - 2f)
        paint.color = PLATE_COLOR
        canvas.drawRoundRect(plate, SIZE * 0.22f, SIZE * 0.22f, paint)
        val inset = SIZE * 0.13f
        canvas.drawBitmap(logo, null, RectF(inset, inset, SIZE - inset, SIZE - inset), paint)
        logo.recycle()
        return badge
    }
}

private const val BADGE_POP_SECONDS = 0.34f

/**
 * The logo of the language currently on screen: a camera-facing textured quad that pops in each
 * time [bitmap] changes and hides while it is null (a language without a logo, or none yet).
 *
 * One node and one texture serve every language. A change re-uploads the 256 px texture instead
 * of building a node, a material and a texture per language. [seed] only sizes that texture.
 */
@Composable
fun SceneScope.LanguageBadge(seed: Bitmap, bitmap: Bitmap?, size: Float, position: Position) {
    val node = remember(engine, materialLoader) {
        ImageNode(
            materialLoader = materialLoader,
            bitmap = seed,
            size = Size(size, size),
            normal = Direction(z = 1f),
        ).apply { isVisible = false }
    }
    DisposableEffect(node, bitmap) {
        if (bitmap == null) {
            node.isVisible = false
            node.onFrame = null
        } else {
            if (node.bitmap !== bitmap) node.bitmap = bitmap
            node.scale = Scale(0.001f)
            node.isVisible = true
            val clock = FrameClock()
            node.onFrame = { nanos ->
                val t = clock.seconds(nanos)
                if (t >= BADGE_POP_SECONDS) {
                    node.scale = Scale(1f)
                    node.onFrame = null
                } else {
                    node.scale = Scale(easeOutBack(t / BADGE_POP_SECONDS).coerceAtLeast(0.001f))
                }
            }
        }
        onDispose {}
    }
    DisposableEffect(node, position.x, position.y, position.z) { node.position = position; onDispose {} }
    NodeLifecycle(node, content = null)
}
