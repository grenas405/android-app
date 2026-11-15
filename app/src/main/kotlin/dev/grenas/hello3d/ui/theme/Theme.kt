package dev.grenas.hello3d.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Neutral surfaces shared by every theme pack; the packs supply the colour. */
object Palette {
    val Ink = Color(0xFF05070D)
    val InkRaised = Color(0xFF0B1020)
    val Mist = Color(0xFFE8ECF2)
    val MistMuted = Color(0xFF9AA4B2)
}

private fun typographyFor(family: FontFamily) = Typography(
    displaySmall = TextStyle(
        fontFamily = family, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = family, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = family, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = family, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp,
    ),
)

@Composable
fun Hello3DTheme(pack: ThemePack, content: @Composable () -> Unit) {
    val family = when (pack.typeface) {
        GlyphTypeface.Mono -> FontFamily.Monospace
        GlyphTypeface.Sans -> FontFamily.SansSerif
    }
    val scheme = darkColorScheme(
        primary = pack.accent,
        onPrimary = pack.backdrop.last(),
        secondary = pack.accentAlt,
        onSecondary = pack.backdrop.last(),
        background = pack.backdrop.last(),
        onBackground = pack.textPrimary,
        surface = pack.backdrop.first(),
        onSurface = pack.textPrimary,
        surfaceVariant = pack.backdrop.first(),
        onSurfaceVariant = pack.textMuted,
        outline = pack.textPrimary.copy(alpha = 0.16f),
        error = Color(0xFFFF6B6B),
    )
    MaterialTheme(colorScheme = scheme, typography = typographyFor(family), content = content)
}
