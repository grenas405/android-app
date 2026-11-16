package dev.grenas.hello3d.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.android.filament.Engine
import dev.grenas.hello3d.data.Profile
import dev.grenas.hello3d.data.RenderPreset
import dev.grenas.hello3d.scene.ProfileScene
import dev.grenas.hello3d.scene.SentenceScript
import dev.grenas.hello3d.ui.components.Glass
import dev.grenas.hello3d.ui.components.LiveDot
import dev.grenas.hello3d.ui.components.Pill
import dev.grenas.hello3d.ui.components.SectionLabel
import dev.grenas.hello3d.ui.theme.ThemePack
import dev.grenas.hello3d.ui.theme.ThemePacks
import io.github.sceneview.RenderQuality
import kotlinx.coroutines.launch

/**
 * The profile: themed backdrop with matrix rain, the Filament viewport with the animated 3D sentence,
 * a header with the developer's name, and a light control panel for theme switching.
 */
@Composable
fun ProfileScreen(
    engine: Engine,
    profile: Profile,
    pack: ThemePack,
    onThemeSelected: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var ready by remember { mutableStateOf(false) }
    val pulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val onTap: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            pulse.snapTo(1f)
            pulse.animateTo(1.1f, tween(durationMillis = 130, easing = FastOutSlowInEasing))
            pulse.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessLow))
        }
    }
    val sentence = remember(profile) { SentenceScript.sentence(profile) }

    Box(modifier = Modifier.fillMaxSize().background(pack.backdrop.last())) {
        Backdrop(pack = pack)
        MatrixRain(pack = pack, intensity = profile.rainIntensity, modifier = Modifier.fillMaxSize())
        AccentGlow(accent = pack.accent)

        ProfileScene(
            engine = engine,
            pack = pack,
            profile = profile,
            quality = profile.quality.toRenderQuality(),
            // Read by the render loop, not here: a tap must not recompose the screen every frame.
            pulse = { pulse.value },
            onReady = { ready = true },
            onTap = onTap,
            modifier = Modifier.fillMaxSize().semantics { contentDescription = sentence },
        )

        Vignette(pack.backdrop.last())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Header(profile = profile, pack = pack, backend = engine.backend.name, onOpenSettings = onOpenSettings)
            ThemePanel(selected = pack, onThemeSelected = onThemeSelected)
        }

        WarmUpVeil(visible = !ready, pack = pack)
    }
}

private fun RenderPreset.toRenderQuality(): RenderQuality = when (this) {
    RenderPreset.Cinematic -> RenderQuality.Cinematic
    RenderPreset.Default -> RenderQuality.Default
    RenderPreset.Performance -> RenderQuality.Performance
}

// ── Backdrop & atmosphere ─────────────────────────────────────────────────────────────────────────

@Composable
private fun Backdrop(pack: ThemePack) {
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(pack.backdrop)))
}

@Composable
private fun AccentGlow(accent: Color) {
    val glow = rememberInfiniteTransition(label = "glow")
    val breathe = glow.animateFloat(
        initialValue = 0.82f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    // The gradients are built once per size and colour. Breathing only scales the cached circle, so
    // the animation costs a transform per frame instead of a new gradient shader per frame. The glow
    // has its own layer so that this per-frame redraw does not re-record the rest of the backdrop.
    Box(
        modifier = Modifier.fillMaxSize().graphicsLayer().drawWithCache {
            val center = Offset(size.width * 0.5f, size.height * 0.46f)
            val radius = size.minDimension * 0.78f
            val halo = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.05f), Color.Transparent),
                center = center, radius = radius,
            )
            val band = Brush.verticalGradient(
                colors = listOf(Color.Transparent, accent.copy(alpha = 0.05f), Color.Transparent),
                startY = size.height * 0.64f, endY = size.height * 0.88f,
            )
            onDrawBehind {
                scale(breathe.value, pivot = center) {
                    drawCircle(brush = halo, radius = radius, center = center)
                }
                drawRect(
                    brush = band,
                    topLeft = Offset(0f, size.height * 0.64f),
                    size = Size(size.width, size.height * 0.24f),
                )
            }
        }
    )
}

@Composable
private fun Vignette(ink: Color) {
    Box(
        modifier = Modifier.fillMaxSize().drawWithCache {
            val shade = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Transparent, ink.copy(alpha = 0.6f)),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.72f,
            )
            onDrawBehind { drawRect(shade) }
        }
    )
}

// ── Chrome ────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun Header(profile: Profile, pack: ThemePack, backend: String, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${pack.name.uppercase()}  ·  FILAMENT ${backend.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = pack.textMuted,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = profile.name.trim().ifBlank { SentenceScript.FALLBACK_NAME },
                style = MaterialTheme.typography.displaySmall,
                color = pack.textPrimary,
                maxLines = 1,
            )
            if (profile.location.isNotBlank()) {
                Text(
                    text = profile.location,
                    style = MaterialTheme.typography.labelMedium,
                    color = pack.textMuted,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Pill {
            LiveDot(pack.accent)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Open settings",
                    tint = pack.textPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ThemePanel(selected: ThemePack, onThemeSelected: (String) -> Unit) {
    Glass(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("THEME")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = selected.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = selected.textPrimary,
                    )
                }
                ThemePacks.all.forEachIndexed { index, pack ->
                    ThemeDot(pack = pack, selected = pack.id == selected.id) { onThemeSelected(pack.id) }
                    if (index != ThemePacks.all.lastIndex) Spacer(Modifier.width(10.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Drag to orbit  ·  Pinch to zoom  ·  Tap to pulse",
                style = MaterialTheme.typography.labelMedium,
                color = selected.textMuted.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun ThemeDot(pack: ThemePack, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) pack.textPrimary else Color.Transparent
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .border(width = 2.dp, color = ring, shape = CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(pack.accent, pack.accentAlt)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = "${pack.name} theme" }
    )
}

// ── Warm-up veil ──────────────────────────────────────────────────────────────────────────────────

@Composable
fun WarmUpVeil(visible: Boolean, pack: ThemePack) {
    AnimatedVisibility(visible = visible, exit = fadeOut(tween(650))) {
        val transition = rememberInfiniteTransition(label = "warmup")
        val angle = transition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
            label = "spin",
        )
        Box(
            modifier = Modifier.fillMaxSize().background(pack.backdrop.last()),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        // Layer property, so the spinner turns without recomposing.
                        .graphicsLayer { rotationZ = angle.value }
                        .drawBehind {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Color.Transparent, pack.accent)),
                                startAngle = 0f, sweepAngle = 300f, useCenter = false,
                                style = Stroke(width = 3.dp.toPx()),
                            )
                        }
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "WARMING UP THE RENDERER",
                    style = MaterialTheme.typography.labelSmall,
                    color = pack.textMuted,
                )
            }
        }
    }
}
