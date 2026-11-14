package dev.grenas.hello3d.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Palette: deep ink backgrounds, soft white text, and a swappable accent. */
object Palette {
    val Ink = Color(0xFF05070D)
    val InkRaised = Color(0xFF0B1020)
    val Mist = Color(0xFFE8ECF2)
    val MistMuted = Color(0xFF9AA4B2)

    val Cyan = Color(0xFF38E1FF)
    val Violet = Color(0xFFA78BFA)
    val Amber = Color(0xFFFFB454)
    val Emerald = Color(0xFF4ADE80)

    val accents = listOf(Cyan, Violet, Amber, Emerald)
}

private val Display = FontFamily.SansSerif

val Hello3DTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = 0.1.sp,
    ),
)

@Composable
fun Hello3DTheme(accent: Color = Palette.Cyan, content: @Composable () -> Unit) {
    // The experience is designed dark-first; the system setting only nudges surface tint.
    val raised = if (isSystemInDarkTheme()) Palette.InkRaised else Palette.InkRaised.copy(alpha = 0.96f)
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Palette.Ink,
        background = Palette.Ink,
        onBackground = Palette.Mist,
        surface = raised,
        onSurface = Palette.Mist,
        surfaceVariant = raised,
        onSurfaceVariant = Palette.MistMuted,
        outline = Palette.Mist.copy(alpha = 0.16f),
    )
    MaterialTheme(colorScheme = scheme, typography = Hello3DTypography, content = content)
}
