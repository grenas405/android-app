package dev.grenas.hello3d.data

/** Curated programming languages offered as chips in Settings; custom entries are added on top. */
object Languages {
    val curated: List<String> = listOf(
        "Kotlin", "Java", "Rust", "Go", "Python", "TypeScript", "JavaScript", "C", "C++", "C#",
        "Swift", "Dart", "Ruby", "PHP", "Bash", "PowerShell", "SQL", "Haskell", "Scala", "Elixir",
        "Lua", "Zig", "Solidity", "Assembly",
    )

    /** Trims, collapses whitespace and caps the length so a custom entry fits on one 3D line. */
    fun normalise(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ").take(Profile.MAX_LANGUAGE_LENGTH)
}
