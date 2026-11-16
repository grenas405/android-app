package dev.grenas.hello3d.data

import androidx.annotation.DrawableRes
import dev.grenas.hello3d.R

/**
 * Maps a language name to its official logo (full-colour artwork from the devicon set, bundled as
 * `drawable-nodpi/logo_*.webp`). Names are matched case-insensitively and through common aliases so
 * custom entries such as "golang", "cpp" or "JS" resolve too.
 *
 * SQL and Assembly have no official logo, so they (and unknown custom languages) return null and
 * are simply shown without one.
 */
object LanguageLogos {

    @DrawableRes
    fun forLanguage(name: String): Int? = byKey[key(name)]

    private fun key(name: String): String = name.trim().lowercase().replace(Regex("\\s+"), " ")

    private val byKey: Map<String, Int> = buildMap {
        fun logo(@DrawableRes resId: Int, vararg names: String) = names.forEach { put(it, resId) }

        // Curated languages (see [Languages.curated]).
        logo(R.drawable.logo_kotlin, "kotlin", "kt")
        logo(R.drawable.logo_java, "java")
        logo(R.drawable.logo_rust, "rust", "rs")
        logo(R.drawable.logo_go, "go", "golang")
        logo(R.drawable.logo_python, "python", "python3", "py")
        logo(R.drawable.logo_typescript, "typescript", "ts")
        logo(R.drawable.logo_javascript, "javascript", "js", "ecmascript")
        logo(R.drawable.logo_c, "c")
        logo(R.drawable.logo_cplusplus, "c++", "cpp", "cplusplus")
        logo(R.drawable.logo_csharp, "c#", "csharp", "c sharp")
        logo(R.drawable.logo_swift, "swift")
        logo(R.drawable.logo_dart, "dart")
        logo(R.drawable.logo_ruby, "ruby", "rb")
        logo(R.drawable.logo_php, "php")
        logo(R.drawable.logo_bash, "bash", "shell", "sh")
        logo(R.drawable.logo_powershell, "powershell", "pwsh")
        logo(R.drawable.logo_haskell, "haskell", "hs")
        logo(R.drawable.logo_scala, "scala")
        logo(R.drawable.logo_elixir, "elixir", "ex")
        logo(R.drawable.logo_lua, "lua")
        logo(R.drawable.logo_zig, "zig")
        logo(R.drawable.logo_solidity, "solidity", "sol")

        // Popular custom entries.
        logo(R.drawable.logo_wasm, "webassembly", "wasm")
        logo(R.drawable.logo_perl, "perl")
        logo(R.drawable.logo_r, "r")
        logo(R.drawable.logo_julia, "julia")
        logo(R.drawable.logo_clojure, "clojure", "clj")
        logo(R.drawable.logo_erlang, "erlang", "erl")
        logo(R.drawable.logo_ocaml, "ocaml")
        logo(R.drawable.logo_fsharp, "f#", "fsharp", "f sharp")
        logo(R.drawable.logo_groovy, "groovy")
        logo(R.drawable.logo_objectivec, "objective-c", "objective c", "objectivec", "objc", "obj-c")
        logo(R.drawable.logo_nim, "nim")
        logo(R.drawable.logo_crystal, "crystal")
        logo(R.drawable.logo_html, "html", "html5")
        logo(R.drawable.logo_css, "css", "css3")
        logo(R.drawable.logo_fortran, "fortran")
        logo(R.drawable.logo_matlab, "matlab")
        logo(R.drawable.logo_elm, "elm")
    }
}
