package dev.grenas.hello3d.scene

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import dev.grenas.hello3d.data.Profile
import dev.grenas.hello3d.ui.theme.GlyphTypeface
import dev.grenas.hello3d.ui.theme.MaterialFinish
import dev.grenas.hello3d.ui.theme.ThemePack
import io.github.sceneview.FrameRatePolicy
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneScope
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.material.setColor
import io.github.sceneview.material.setMetallic
import io.github.sceneview.material.setReflectance
import io.github.sceneview.material.setRoughness
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.math.colorOf
import io.github.sceneview.math.toLinearSpace
import io.github.sceneview.node.GeometryNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberFillLightNode
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** World-space layout of the typographic block. */
private const val HERO_WIDTH = 2.0f
private const val LINE_WIDTH = 2.2f
private const val LINE_CAP_HEIGHT = 0.19f
private const val HERO_Y = 1.05f
private val LINE_BASELINES = floatArrayOf(0.12f, -0.33f, -0.78f)

/** The language logo trails the language name: its edge length and the gap before it, in world units. */
private const val BADGE_SIZE = 0.3f
private const val BADGE_GAP = 0.09f

private const val THEME_FADE_MILLIS = 650
private val LABEL_FINISH = MaterialFinish(0.6f, 0.5f, 0.5f)

/**
 * The developer-profile hero: extruded "HELLO WORLD" in themed metal, three lines of 3D glyph text
 * typed and scrambled by [SentenceScript], the official logo of the language on screen, an emissive
 * halo ring, orbiting sparks and a contact shadow. Rendered by Google Filament through SceneView's
 * Compose DSL on a transparent surface so the Compose backdrop (gradient, matrix rain, glow) shows
 * through.
 *
 * [pulse] is read from the render loop rather than passed as a value, so the tap animation never
 * recomposes the scene. [onReady] fires once the first frame that includes the hero words is up.
 */
@Composable
fun ProfileScene(
    engine: Engine,
    pack: ThemePack,
    profile: Profile,
    quality: RenderQuality,
    pulse: () -> Float,
    onReady: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val view = rememberView(engine)
    val environment = rememberEnvironment(environmentLoader, isOpaque = false)

    val cameraNode = rememberCameraNode(engine)
    val cameraManipulator = rememberCameraManipulator(
        orbitHomePosition = Position(0f, 0.2f, 6.3f),
        targetPosition = Position(0f, -0.1f, 0f),
    )
    val mainLight = rememberMainLightNode(engine) {
        intensity = 70_000f
        lightDirection = Direction(-0.55f, -0.8f, -0.4f)
    }
    val fillLight = rememberFillLightNode(engine)

    // Theme colours ease between packs. The animated values are only ever read inside snapshotFlow,
    // so a fade updates the Filament materials in place without recomposing the scene every frame.
    val accent = animateColorAsState(pack.accent, tween(THEME_FADE_MILLIS), label = "accent")
    val accentAlt = animateColorAsState(pack.accentAlt, tween(THEME_FADE_MILLIS), label = "accentAlt")
    val heroColor = animateColorAsState(pack.heroColor, tween(THEME_FADE_MILLIS), label = "hero")
    val labelInk = pack.backdrop.first()

    val chrome = remember(materialLoader) { materialLoader.createColorInstance(color = pack.heroColor) }
    val accentMetal = remember(materialLoader) { materialLoader.createColorInstance(color = pack.accent) }
    val label = remember(materialLoader) { materialLoader.createColorInstance(color = lerp(pack.heroColor, labelInk, 0.5f)) }
    val halo = remember(materialLoader) { materialLoader.createColorInstance(color = pack.accent, metallic = 0.3f, roughness = 0.55f) }
    val spark = remember(materialLoader) { materialLoader.createColorInstance(color = Color.White, metallic = 0f, roughness = 1f) }
    val cursor = remember(materialLoader) { materialLoader.createColorInstance(color = pack.accentAlt, metallic = 0f, roughness = 1f) }
    LaunchedEffect(chrome, label, pack.heroFinish, labelInk) {
        chrome.applyFinish(pack.heroFinish)
        label.applyFinish(LABEL_FINISH)
        snapshotFlow { heroColor.value }.collect { hero ->
            chrome.setColor(colorOf(hero))
            label.setColor(colorOf(lerp(hero, labelInk, 0.5f)))
        }
    }
    LaunchedEffect(accentMetal, halo, spark, cursor, pack.nameFinish) {
        accentMetal.applyFinish(pack.nameFinish)
        snapshotFlow { accent.value to accentAlt.value }.collect { (main, alt) ->
            accentMetal.setColor(colorOf(main))
            halo.setColor(colorOf(main)); halo.setEmissive(main, intensity = 3f)
            spark.setEmissive(lerp(main, Color.White, 0.45f), intensity = 7f)
            cursor.setColor(colorOf(alt)); cursor.setEmissive(alt, intensity = 5f)
        }
    }

    // Type: the hero words are one mesh each; the sentence uses a per-character atlas. Both are
    // tessellated off the main thread and cached for the life of the process. While a new typeface
    // is being built the previous hero words stay on screen.
    val heroMeshes by produceState(HeroMeshes.cached(pack.typeface), pack.typeface) {
        value = HeroMeshes.of(pack.typeface)
    }
    val hero = remember(engine, heroMeshes) { heroMeshes?.let { HeroGeometry(engine, it) } }
    val atlas = remember(pack.typeface) { GlyphAtlas.of(pack.typeface) }
    val glyphs = remember(engine, atlas) { GlyphGeometries(engine, atlas) }
    DisposableEffect(glyphs) { onDispose { glyphs.destroy() } }
    val board = remember(atlas) { LineBoard(SentenceScript.LINE_COUNT) }
    val script = remember(profile.name, profile.location, profile.languages) { SentenceScript.build(profile) }
    var lineUnits by remember(atlas) { mutableStateOf(FloatArray(SentenceScript.LINE_COUNT)) }
    var badges by remember { mutableStateOf(emptyMap<String, Bitmap>()) }

    val context = LocalContext.current.applicationContext
    LaunchedEffect(atlas, board, script, pack.glyphAlphabet) {
        board.reset()
        atlas.warmUp(SentenceScript.charactersFor(profile) + pack.glyphAlphabet.toList())
        val loaded = LogoBadges.load(context, profile.languages)
        badges = loaded
        lineUnits = lineUnitsFor(atlas, profile, reserveBadge = loaded.isNotEmpty())
        SentenceScript.play(script, board, pack.glyphAlphabet)
    }

    val sparks = remember { Spark.swarm(count = 18, seed = 7) }
    val heroReady = rememberUpdatedState(hero != null)
    val currentOnReady = rememberUpdatedState(onReady)
    val currentOnTap = rememberUpdatedState(onTap)
    val currentPulse = rememberUpdatedState(pulse)
    var readyReported by remember { mutableStateOf(false) }

    SceneView(
        modifier = modifier,
        surfaceType = SurfaceType.TextureSurface,
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        environmentLoader = environmentLoader,
        view = view,
        isOpaque = false,
        frameRatePolicy = FrameRatePolicy.Continuous(),
        renderQuality = quality,
        autoCenterContent = false,
        environment = environment,
        mainLightNode = mainLight,
        fillLightNode = fillLight,
        cameraNode = cameraNode,
        cameraManipulator = cameraManipulator,
        onGestureListener = rememberOnGestureListener(onSingleTapConfirmed = { _, _ -> currentOnTap.value() }),
        onFrame = {
            if (!readyReported && heroReady.value) {
                readyReported = true
                currentOnReady.value()
            }
        },
    ) {
        TypographicBlock(
            hero = hero, chrome = chrome, accentMetal = accentMetal, label = label, cursor = cursor,
            glyphs = glyphs, board = board, lineUnits = lineUnits, badges = badges,
            pulse = { currentPulse.value() },
        )
        HaloRing(halo, spark)
        sparks.forEachIndexed { index, s -> key(index) { OrbitingSpark(s, spark) } }
        ContactShadow(
            size = Size(4.8f, 4.8f, 1f),
            position = Position(0f, -1.4f, 0f),
            intensity = 0.5f,
        )
    }
}

/**
 * Per-line scale so the longest text each line can show fits [LINE_WIDTH] at most [LINE_CAP_HEIGHT]
 * tall. With [reserveBadge] the languages line leaves room for the logo that trails it.
 */
private fun lineUnitsFor(atlas: GlyphAtlas, profile: Profile, reserveBadge: Boolean): FloatArray {
    val base = LINE_CAP_HEIGHT / atlas.capHeight
    val name = SentenceScript.NAME_LABEL + profile.name.trim().ifBlank { SentenceScript.FALLBACK_NAME }
    val languages = profile.languages.map { SentenceScript.LANGUAGES_LABEL + it.trim() }.ifEmpty { listOf("") }
    val location = SentenceScript.LOCATION_LABEL + profile.location.trim()
    fun fit(texts: List<String>, available: Float = LINE_WIDTH): Float {
        val widest = texts.maxOf { atlas.lineWidth(it) }
        return if (widest <= 0f) base else min(base, available / widest)
    }
    val languagesWidth = if (reserveBadge) LINE_WIDTH - BADGE_GAP - BADGE_SIZE else LINE_WIDTH
    return floatArrayOf(fit(listOf(name)), fit(languages, languagesWidth), fit(listOf(location)))
}

/** CPU meshes of the two hero words for one typeface, built once per process off the main thread. */
private class HeroMeshes(val hello: ExtrudedText.Mesh, val world: ExtrudedText.Mesh) {
    companion object {
        private val shared = ConcurrentHashMap<GlyphTypeface, HeroMeshes>()

        fun cached(typeface: GlyphTypeface): HeroMeshes? = shared[typeface]

        suspend fun of(typeface: GlyphTypeface): HeroMeshes = shared[typeface] ?: withContext(Dispatchers.Default) {
            val face = typeface.toTypeface()
            val built = HeroMeshes(
                hello = ExtrudedText.build(ExtrudedText.Spec("HELLO", face)),
                world = ExtrudedText.build(ExtrudedText.Spec("WORLD", face)),
            )
            shared.putIfAbsent(typeface, built) ?: built
        }
    }
}

/** The hero words uploaded to one engine. The geometries are freed with the nodes that draw them. */
private class HeroGeometry(engine: Engine, val meshes: HeroMeshes) {
    val hello: Geometry = ExtrudedText.upload(engine, meshes.hello)
    val world: Geometry = ExtrudedText.upload(engine, meshes.world)
}

// ── Scene pieces ──────────────────────────────────────────────────────────────────────────────────

@Composable
private fun SceneScope.TypographicBlock(
    hero: HeroGeometry?,
    chrome: MaterialInstance,
    accentMetal: MaterialInstance,
    label: MaterialInstance,
    cursor: MaterialInstance,
    glyphs: GlyphGeometries,
    board: LineBoard,
    lineUnits: FloatArray,
    badges: Map<String, Bitmap>,
    pulse: () -> Float,
) {
    // The whole block shares one slow sway so the sentence and the hero read as a single object.
    // The tap pulse is applied here, in the render loop, instead of through recomposition.
    Node(
        apply = {
            val clock = FrameClock()
            var appliedPulse = 1f
            onFrame = { nanos ->
                val t = clock.seconds(nanos)
                rotation = Rotation(x = 2.5f * sin(t * 0.55f + 1.2f), y = 9f * sin(t * 0.31f), z = 0f)
                position = Position(0f, 0.04f * sin(t * 0.9f), 0f)
                val target = pulse()
                if (target != appliedPulse) {
                    appliedPulse = target
                    scale = Scale(target)
                }
            }
        },
    ) {
        if (hero != null) {
            val heroUnit = HERO_WIDTH / max(hero.meshes.hello.width, hero.meshes.world.width)
            val heroGap = max(hero.meshes.hello.height, hero.meshes.world.height) * heroUnit * 0.72f
            ExtrudedTextNode(hero.hello, chrome, position = Position(y = HERO_Y + heroGap), scale = Scale(heroUnit))
            ExtrudedTextNode(hero.world, accentMetal, position = Position(y = HERO_Y - heroGap), scale = Scale(heroUnit))
        }

        // The language on screen, and its logo if it has one. The logo trails the name, so the line
        // shifts left by half of what the logo occupies to keep the pair centred.
        val atlas = glyphs.atlas
        val languagesText = board.lines[SentenceScript.LINE_LANGUAGES].text
        val badge = badges[languagesText.removePrefix(SentenceScript.LANGUAGES_LABEL)]
        val trailing = if (badge != null) BADGE_GAP + BADGE_SIZE else 0f

        board.lines.forEachIndexed { index, line ->
            val unit = lineUnits.getOrNull(index)?.takeIf { it > 0f } ?: return@forEachIndexed
            key(index) {
                TextLine3D(
                    state = line,
                    glyphs = glyphs,
                    plainMaterial = label,
                    emphasisMaterial = accentMetal,
                    cursorMaterial = cursor,
                    unitScale = unit,
                    position = Position(
                        x = if (index == SentenceScript.LINE_LANGUAGES) -trailing / 2f else 0f,
                        y = LINE_BASELINES[index],
                    ),
                )
            }
        }

        val seed = remember(badges) { badges.values.firstOrNull() }
        val unit = lineUnits.getOrNull(SentenceScript.LINE_LANGUAGES) ?: 0f
        if (seed != null && unit > 0f) {
            val textWidth = remember(atlas, languagesText, unit) { atlas.lineWidth(languagesText) * unit }
            LanguageBadge(
                seed = seed,
                bitmap = badge,
                size = BADGE_SIZE,
                position = Position(
                    x = textWidth / 2f + BADGE_GAP / 2f,
                    y = LINE_BASELINES[SentenceScript.LINE_LANGUAGES] + atlas.capHeight * unit / 2f,
                    z = 0f,
                ),
            )
        }
    }
}

@Composable
private fun SceneScope.HaloRing(halo: MaterialInstance, bead: MaterialInstance) {
    Node(rotation = Rotation(x = 16f, y = 0f, z = -10f)) {
        Node(apply = {
            val clock = FrameClock()
            onFrame = { nanos -> rotation = Rotation(0f, (clock.seconds(nanos) * 9f) % 360f, 0f) }
        }) {
            TorusNode(
                majorRadius = 2.3f,
                minorRadius = 0.014f,
                majorSegments = 192,
                minorSegments = 10,
                materialInstance = halo,
            )
            SphereNode(radius = 0.06f, position = Position(x = 2.3f), materialInstance = bead)
        }
    }
}

@Composable
private fun SceneScope.OrbitingSpark(spark: Spark, material: MaterialInstance) {
    Node(rotation = Rotation(x = spark.tiltX, y = 0f, z = spark.tiltZ)) {
        Node(apply = {
            val clock = FrameClock()
            onFrame = { nanos ->
                rotation = Rotation(0f, (spark.phase + spark.speed * clock.seconds(nanos)) % 360f, 0f)
            }
        }) {
            SphereNode(
                radius = spark.size,
                stacks = 10,
                slices = 14,
                position = Position(x = spark.radius),
                materialInstance = material,
            )
        }
    }
}

/** A composable wrapper around [GeometryNode] for whole-word meshes produced by [ExtrudedText]. */
@Composable
private fun SceneScope.ExtrudedTextNode(
    geometry: Geometry,
    materialInstance: MaterialInstance,
    position: Position = Position(0f),
    rotation: Rotation = Rotation(0f),
    scale: Scale = Scale(1f),
) {
    val node = remember(engine, geometry) {
        GeometryNode(engine = engine, geometry = geometry, materialInstance = materialInstance)
    }
    val previousMaterial = remember { mutableStateOf(materialInstance) }
    SideEffect {
        if (materialInstance != previousMaterial.value) {
            node.setMaterialInstanceAt(0, materialInstance)
            previousMaterial.value = materialInstance
        }
    }
    DisposableEffect(node, position.x, position.y, position.z) { node.position = position; onDispose {} }
    DisposableEffect(node, rotation.x, rotation.y, rotation.z) { node.rotation = rotation; onDispose {} }
    DisposableEffect(node, scale.x, scale.y, scale.z) { node.scale = scale; onDispose {} }
    NodeLifecycle(node, content = null)
}

// ── Helpers ───────────────────────────────────────────────────────────────────────────────────────

private class Spark(
    val radius: Float,
    val tiltX: Float,
    val tiltZ: Float,
    val speed: Float,
    val phase: Float,
    val size: Float,
) {
    companion object {
        fun swarm(count: Int, seed: Int): List<Spark> {
            val random = Random(seed)
            return List(count) {
                Spark(
                    radius = 1.7f + random.nextFloat() * 1.5f,
                    tiltX = -40f + random.nextFloat() * 80f,
                    tiltZ = -35f + random.nextFloat() * 70f,
                    speed = (7f + random.nextFloat() * 16f) * (if (random.nextBoolean()) 1f else -1f),
                    phase = random.nextFloat() * 360f,
                    size = 0.016f + random.nextFloat() * 0.03f,
                )
            }
        }
    }
}

private fun MaterialInstance.applyFinish(finish: MaterialFinish) {
    setMetallic(finish.metallic)
    setRoughness(finish.roughness)
    setReflectance(finish.reflectance)
}

/** Sets Filament's `emissive` parameter (linear RGB × intensity, exposure-weighted) when the material has one. */
private fun MaterialInstance.setEmissive(color: Color, intensity: Float) {
    if (!material.hasParameter("emissive")) return
    val linear = colorOf(color).toLinearSpace()
    // x/y/z are plain fields; the r/g/b aliases are inline accessors compiled for a newer JVM target.
    setParameter("emissive", linear.x * intensity, linear.y * intensity, linear.z * intensity, 1f)
}
