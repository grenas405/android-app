package dev.grenas.hello3d.ui.theme

import androidx.compose.ui.graphics.Color

/** Which typeface family the 3D glyphs and the UI use. */
enum class GlyphTypeface { Mono, Sans }

/** Physically based surface parameters for an extruded-metal material. */
data class MaterialFinish(val metallic: Float, val roughness: Float, val reflectance: Float)

/**
 * A complete look: colours for the UI and backdrop, the finish of the 3D metal, the alphabet the
 * matrix rain and glyph scrambles draw from, and the typeface family.
 */
data class ThemePack(
    val id: String,
    val name: String,
    val tagline: String,
    val accent: Color,
    val accentAlt: Color,
    val backdrop: List<Color>,
    val textPrimary: Color,
    val textMuted: Color,
    val glyphAlphabet: String,
    val typeface: GlyphTypeface,
    val heroColor: Color,
    val heroFinish: MaterialFinish,
    val nameFinish: MaterialFinish,
)

object ThemePacks {

    private const val HEX = "0123456789ABCDEF"
    private const val KATAKANA = "ﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾂｦﾅﾆﾇﾈﾉ"

    val Terminal = ThemePack(
        id = "terminal",
        name = "Terminal",
        tagline = "Phosphor green, falling katakana, the classic matrix look.",
        accent = Color(0xFF39FF14),
        accentAlt = Color(0xFFB6FFB0),
        backdrop = listOf(Color(0xFF041008), Color(0xFF020805), Color(0xFF000000)),
        textPrimary = Color(0xFFD9FFD2),
        textMuted = Color(0xFF6FBF6A),
        glyphAlphabet = KATAKANA + HEX,
        typeface = GlyphTypeface.Mono,
        heroColor = Color(0xFFCFE9CF),
        heroFinish = MaterialFinish(metallic = 0.85f, roughness = 0.42f, reflectance = 0.55f),
        nameFinish = MaterialFinish(metallic = 0.75f, roughness = 0.35f, reflectance = 0.7f),
    )

    val RedTeam = ThemePack(
        id = "red-team",
        name = "Red Team",
        tagline = "Crimson and ember on charcoal. Offensive, binary rain.",
        accent = Color(0xFFFF2A4D),
        accentAlt = Color(0xFFFF7A1A),
        backdrop = listOf(Color(0xFF1A0A0E), Color(0xFF0D0506), Color(0xFF040203)),
        textPrimary = Color(0xFFFFE4E8),
        textMuted = Color(0xFFB08087),
        glyphAlphabet = "01",
        typeface = GlyphTypeface.Mono,
        heroColor = Color(0xFFE6E0E2),
        heroFinish = MaterialFinish(metallic = 1f, roughness = 0.3f, reflectance = 0.7f),
        nameFinish = MaterialFinish(metallic = 0.9f, roughness = 0.26f, reflectance = 0.85f),
    )

    val BlueTeam = ThemePack(
        id = "blue-team",
        name = "Blue Team",
        tagline = "Navy and signal cyan. Defensive, hex-dump rain.",
        accent = Color(0xFF38E1FF),
        accentAlt = Color(0xFF3B82F6),
        backdrop = listOf(Color(0xFF0B1630), Color(0xFF060D1F), Color(0xFF02050D)),
        textPrimary = Color(0xFFE2F4FF),
        textMuted = Color(0xFF8BA6C2),
        glyphAlphabet = HEX,
        typeface = GlyphTypeface.Mono,
        heroColor = Color(0xFFEEF2F7),
        heroFinish = MaterialFinish(metallic = 1f, roughness = 0.34f, reflectance = 0.65f),
        nameFinish = MaterialFinish(metallic = 0.92f, roughness = 0.28f, reflectance = 0.85f),
    )

    val Frontend = ThemePack(
        id = "frontend",
        name = "Frontend Neon",
        tagline = "Coral, violet and electric blue. Markup rain.",
        accent = Color(0xFFFF6B8A),
        accentAlt = Color(0xFFA78BFA),
        backdrop = listOf(Color(0xFF1A1033), Color(0xFF0E0A1F), Color(0xFF05040C)),
        textPrimary = Color(0xFFF6EEFF),
        textMuted = Color(0xFFA899C8),
        glyphAlphabet = "<>/{}();=#.",
        typeface = GlyphTypeface.Sans,
        heroColor = Color(0xFFF4EFFA),
        heroFinish = MaterialFinish(metallic = 0.7f, roughness = 0.22f, reflectance = 0.9f),
        nameFinish = MaterialFinish(metallic = 0.6f, roughness = 0.2f, reflectance = 1f),
    )

    val Backend = ThemePack(
        id = "backend",
        name = "Backend Emerald",
        tagline = "Emerald and slate. JSON rain, server-room calm.",
        accent = Color(0xFF34D399),
        accentAlt = Color(0xFF94A3B8),
        backdrop = listOf(Color(0xFF0C1614), Color(0xFF070E0D), Color(0xFF030606)),
        textPrimary = Color(0xFFE6F7F0),
        textMuted = Color(0xFF8FAFA3),
        glyphAlphabet = "{}[]:,\"0123456789",
        typeface = GlyphTypeface.Mono,
        heroColor = Color(0xFFDCE5E1),
        heroFinish = MaterialFinish(metallic = 0.95f, roughness = 0.4f, reflectance = 0.6f),
        nameFinish = MaterialFinish(metallic = 0.85f, roughness = 0.32f, reflectance = 0.75f),
    )

    val all: List<ThemePack> = listOf(Terminal, RedTeam, BlueTeam, Frontend, Backend)

    fun byId(id: String?): ThemePack = all.firstOrNull { it.id == id } ?: Terminal
}
