package dev.grenas.hello3d.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.grenas.hello3d.scene.HelloWorldScene
import dev.grenas.hello3d.ui.theme.Hello3DTheme
import dev.grenas.hello3d.ui.theme.Palette
import io.github.sceneview.RenderQuality
import io.github.sceneview.rememberEngine
import kotlinx.coroutines.launch

/**
 * Full-screen experience: a layered backdrop, the Filament viewport, a glass control panel and a
 * warm-up veil that fades out on the first presented frame.
 */
@Composable
fun HelloWorldScreen() {
    var accentIndex by rememberSaveable { mutableStateOf(0) }
    var quality by rememberSaveable { mutableStateOf(RenderQuality.Cinematic) }
    var ready by remember { mutableStateOf(false) }

    val accent by animateColorAsState(
        targetValue = Palette.accents[accentIndex],
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "accent",
    )

    val pulse = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val onTap: () -> Unit = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            pulse.snapTo(1f)
            pulse.animateTo(1.12f, tween(durationMillis = 130, easing = FastOutSlowInEasing))
            pulse.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessLow))
        }
    }

    Hello3DTheme(accent = accent) {
        val engine = rememberEngine()

        Box(modifier = Modifier.fillMaxSize().background(Palette.Ink)) {
            Backdrop(accent = accent)

            HelloWorldScene(
                engine = engine,
                accent = accent,
                quality = quality,
                pulse = pulse.value,
                onFirstFrame = { ready = true },
                onTap = onTap,
                modifier = Modifier.fillMaxSize(),
            )

            Vignette()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Header(backend = engine.backend.name, accent = accent)
                ControlPanel(
                    accent = accent,
                    accentIndex = accentIndex,
                    onAccentSelected = { accentIndex = it },
                    quality = quality,
                    onQualitySelected = { quality = it },
                )
            }

            WarmUpVeil(visible = !ready, accent = accent)
        }
    }
}

// ── Backdrop & atmosphere ─────────────────────────────────────────────────────────────────────────

@Composable
private fun Backdrop(accent: Color) {
    val glow = rememberInfiniteTransition(label = "glow")
    val breathe by glow.animateFloat(
        initialValue = 0.82f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.InkRaised, Palette.Ink, Color(0xFF02030A))))
            .drawBehind {
                // Accent bloom behind the hero type.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.44f),
                        radius = size.minDimension * 0.78f * breathe,
                    ),
                    radius = size.minDimension * 0.78f * breathe,
                    center = Offset(size.width * 0.5f, size.height * 0.44f),
                )
                // Horizon haze that grounds the contact shadow.
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, accent.copy(alpha = 0.05f), Color.Transparent),
                        startY = size.height * 0.62f,
                        endY = size.height * 0.86f,
                    ),
                    topLeft = Offset(0f, size.height * 0.62f),
                    size = Size(size.width, size.height * 0.24f),
                )
            }
    )
}

@Composable
private fun Vignette() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Palette.Ink.copy(alpha = 0.55f)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.72f,
                    )
                )
            }
    )
}

// ── Chrome ────────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun Header(backend: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = "SCENEVIEW  ·  FILAMENT",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.MistMuted,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Hello World 3D",
                style = MaterialTheme.typography.displaySmall,
                color = Palette.Mist,
            )
        }
        Pill {
            LiveDot(accent)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "PBR · ${backend.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = Palette.Mist,
            )
        }
    }
}

@Composable
private fun ControlPanel(
    accent: Color,
    accentIndex: Int,
    onAccentSelected: (Int) -> Unit,
    quality: RenderQuality,
    onQualitySelected: (RenderQuality) -> Unit,
) {
    Glass(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = "Real-time physically based rendering",
                style = MaterialTheme.typography.titleMedium,
                color = Palette.Mist,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Glyph outlines are extruded into metal at launch and lit by an HDR environment with bloom.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.MistMuted,
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("ACCENT")
                Spacer(Modifier.weight(1f))
                Palette.accents.forEachIndexed { index, color ->
                    Swatch(color = color, selected = index == accentIndex) { onAccentSelected(index) }
                    if (index != Palette.accents.lastIndex) Spacer(Modifier.width(10.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            SectionLabel("QUALITY")
            Spacer(Modifier.height(8.dp))
            val options = RenderQuality.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == quality,
                        onClick = { onQualitySelected(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = accent.copy(alpha = 0.18f),
                            activeContentColor = Palette.Mist,
                            activeBorderColor = accent.copy(alpha = 0.6f),
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = Palette.MistMuted,
                            inactiveBorderColor = Palette.Mist.copy(alpha = 0.14f),
                        ),
                        icon = {},
                    ) {
                        Text(option.name, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "Drag to orbit  ·  Pinch to zoom  ·  Tap to pulse",
                style = MaterialTheme.typography.labelMedium,
                color = Palette.MistMuted.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = Palette.MistMuted)
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val ring = if (selected) Palette.Mist else Color.Transparent
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .border(width = 2.dp, color = ring, shape = CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(color, color.copy(alpha = 0.75f))))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = "Accent colour" }
    )
}

@Composable
private fun Glass(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.085f), Color.White.copy(alpha = 0.045f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
    ) { content() }
}

@Composable
private fun Pill(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun LiveDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "liveAlpha",
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}

// ── Warm-up veil ──────────────────────────────────────────────────────────────────────────────────

@Composable
private fun WarmUpVeil(visible: Boolean, accent: Color) {
    AnimatedVisibility(visible = visible, exit = fadeOut(tween(650))) {
        val transition = rememberInfiniteTransition(label = "warmup")
        val angle by transition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
            label = "spin",
        )
        Box(
            modifier = Modifier.fillMaxSize().background(Palette.Ink),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .rotate(angle)
                        .drawBehind {
                            drawArc(
                                brush = Brush.sweepGradient(listOf(Color.Transparent, accent)),
                                startAngle = 0f, sweepAngle = 300f, useCenter = false,
                                style = Stroke(width = 3.dp.toPx()),
                            )
                        }
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "WARMING UP THE RENDERER",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.MistMuted,
                )
            }
        }
    }
}
