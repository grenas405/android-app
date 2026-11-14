package dev.grenas.hello3d.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import io.github.sceneview.FrameRatePolicy
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneScope
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.material.setColor
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
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * The 3D hero: two lines of extruded, physically based metal type ("HELLO" in chrome, "WORLD" in
 * the live accent colour) floating over a contact shadow, circled by an emissive halo ring and a
 * swarm of glowing sparks. Rendered by Google Filament through SceneView's Compose DSL.
 *
 * The surface is a transparent TextureView so the Compose backdrop (gradient + accent glow) shows
 * through behind the geometry.
 */
@Composable
fun HelloWorldScene(
    engine: Engine,
    accent: Color,
    quality: RenderQuality,
    pulse: Float,
    onFirstFrame: () -> Unit,
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
        orbitHomePosition = Position(0f, 0.45f, 5.4f),
        targetPosition = Position(0f, 0f, 0f),
    )

    // Key light: a warm-neutral sun from the upper left; the fill light lifts the shadow side.
    val mainLight = rememberMainLightNode(engine) {
        intensity = 70_000f
        lightDirection = Direction(-0.55f, -0.8f, -0.4f)
    }
    val fillLight = rememberFillLightNode(engine)

    // Materials are created once per loader; colour changes are pushed in place.
    val chrome = remember(materialLoader) {
        materialLoader.createColorInstance(
            color = Color(0xFFEEF2F7), metallic = 1f, roughness = 0.34f, reflectance = 0.65f
        )
    }
    val accentMetal = remember(materialLoader) {
        materialLoader.createColorInstance(
            color = accent, metallic = 0.92f, roughness = 0.28f, reflectance = 0.85f
        )
    }
    val halo = remember(materialLoader) {
        materialLoader.createColorInstance(color = accent, metallic = 0.3f, roughness = 0.55f)
    }
    val spark = remember(materialLoader) {
        materialLoader.createColorInstance(color = Color.White, metallic = 0f, roughness = 1f)
    }
    LaunchedEffect(accent) {
        accentMetal.setColor(colorOf(accent))
        halo.setColor(colorOf(accent))
        halo.setEmissive(accent, intensity = 3f)
        spark.setEmissive(lerp(accent, Color.White, 0.45f), intensity = 7f)
    }

    // Glyph meshes are generated on the fly from the system font and shared across recompositions.
    val hello = remember(engine) { ExtrudedText.buildGeometry(engine, ExtrudedText.Spec("HELLO")) }
    val world = remember(engine) { ExtrudedText.buildGeometry(engine, ExtrudedText.Spec("WORLD")) }
    val sparks = remember { Spark.swarm(count = 18, seed = 7) }

    var firstFrameReported by remember { mutableStateOf(false) }

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
        onGestureListener = rememberOnGestureListener(onSingleTapConfirmed = { _, _ -> onTap() }),
        onFrame = {
            if (!firstFrameReported) {
                firstFrameReported = true
                onFirstFrame()
            }
        },
    ) {
        HeroType(hello, world, chrome, accentMetal, pulse)
        HaloRing(halo, spark)
        sparks.forEachIndexed { index, s ->
            key(index) { OrbitingSpark(s, spark) }
        }
        ContactShadow(
            size = Size(4.6f, 4.6f, 1f),
            position = Position(0f, -1.45f, 0f),
            intensity = 0.55f,
        )
    }
}

// ── Scene pieces ──────────────────────────────────────────────────────────────────────────────────

private const val TARGET_WIDTH = 2.15f

@Composable
private fun SceneScope.HeroType(
    hello: Pair<Geometry, ExtrudedText.Mesh>,
    world: Pair<Geometry, ExtrudedText.Mesh>,
    chrome: MaterialInstance,
    accentMetal: MaterialInstance,
    pulse: Float,
) {
    // Same scale for both lines so cap heights match; the wider word sets the fit.
    val unit = TARGET_WIDTH / max(hello.second.width, world.second.width)
    val lineGap = max(hello.second.height, world.second.height) * unit * 0.72f

    Node(
        scale = Scale(pulse),
        apply = {
            val clock = FrameClock()
            onFrame = { nanos ->
                val t = clock.seconds(nanos)
                rotation = Rotation(
                    x = 3.5f * sin(t * 0.55f + 1.2f),
                    y = 12f * sin(t * 0.31f),
                    z = 0f,
                )
                position = Position(0f, 0.05f * sin(t * 0.9f), 0f)
            }
        },
    ) {
        ExtrudedTextNode(hello.first, chrome, position = Position(y = lineGap), scale = Scale(unit))
        ExtrudedTextNode(world.first, accentMetal, position = Position(y = -lineGap), scale = Scale(unit))
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
                majorRadius = 2.05f,
                minorRadius = 0.014f,
                majorSegments = 192,
                minorSegments = 10,
                materialInstance = halo,
            )
            SphereNode(radius = 0.06f, position = Position(x = 2.05f), materialInstance = bead)
        }
    }
}

@Composable
private fun SceneScope.OrbitingSpark(spark: Spark, material: MaterialInstance) {
    // Outer node tilts the orbital plane; inner node spins. Nesting keeps the Euler order simple.
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

/** A composable wrapper around [GeometryNode] for meshes produced by [ExtrudedText]. */
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

private class FrameClock {
    private var start = -1L
    fun seconds(nanos: Long): Float {
        if (start < 0) start = nanos
        return (nanos - start) / 1_000_000_000f
    }
}

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
                    radius = 1.5f + random.nextFloat() * 1.5f,
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

/** Sets Filament's `emissive` parameter (linear RGB × intensity, exposure-weighted) when the material has one. */
private fun MaterialInstance.setEmissive(color: Color, intensity: Float) {
    if (!material.hasParameter("emissive")) return
    val linear = colorOf(color).toLinearSpace()
    // x/y/z are plain fields; the r/g/b aliases are inline accessors compiled for a newer JVM target.
    setParameter("emissive", linear.x * intensity, linear.y * intensity, linear.z * intensity, 1f)
}
