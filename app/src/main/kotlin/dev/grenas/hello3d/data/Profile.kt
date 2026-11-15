package dev.grenas.hello3d.data

/** Filament render quality preset, kept free of SceneView types so the data layer is plain Kotlin. */
enum class RenderPreset { Cinematic, Default, Performance }

/** How dense the matrix-rain backdrop is. */
enum class RainIntensity { Off, Subtle, Full }

/** Everything the profile screen needs to tell the developer's story. Persisted via [ProfileRepository]. */
data class Profile(
    val name: String = "Developer",
    val location: String = "Planet Earth",
    val languages: List<String> = listOf("Kotlin", "Rust", "TypeScript"),
    val themeId: String = DEFAULT_THEME_ID,
    val quality: RenderPreset = RenderPreset.Cinematic,
    val rainIntensity: RainIntensity = RainIntensity.Subtle,
) {
    companion object {
        const val MAX_NAME = 24
        const val MAX_LOCATION = 32
        const val MAX_LANGUAGES = 8
        const val MAX_LANGUAGE_LENGTH = 16
        const val DEFAULT_THEME_ID = "terminal"
    }
}
