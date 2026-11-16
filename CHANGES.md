# Changes

## 1.2.0 — 2026-11-16

Language logos and a performance pass.

- Official language logos: the logo of the language on screen pops in beside its name in the 3D
  scene, and the Settings chips show each language's logo. Full-colour artwork from devicon for
  the 22 curated languages that have one plus 17 common custom entries (Perl, R, Julia, HTML, CSS,
  WebAssembly, …), matched case-insensitively and through aliases such as "golang", "cpp", "js".
  SQL and Assembly have no official logo and are shown without one.
- Glyph geometry is shared: one GPU buffer pair per character, re-pointed during scrambles
  instead of re-uploading a mesh per glyph every 60 ms.
- Sentence glyphs use a coarser curve step (several times fewer vertices and faster tessellation);
  glyph and hero meshes are cached per typeface for the life of the process and the hero words are
  tessellated off the main thread.
- The tap pulse and theme colour fades no longer recompose the scene every frame; they are
  applied from the render loop and a snapshot flow.
- Backdrop: gradient shaders are cached instead of rebuilt per frame, the glow and the rain draw
  into their own layers, the rain redraws only when a row changes and allocates nothing per glyph.
- The live dot and warm-up spinner animate as layer properties rather than by recomposition.
- Unit tests for the logo mapping (`LanguageLogosTest`).

## 1.1.0 — 2026-11-14

Developer profile.

- Settings screen (name, location, languages with curated chips plus custom entries, theme, Filament
  render quality, matrix-rain density) persisted with Preferences DataStore; Navigation Compose
  between profile and settings with a single shared Filament engine.
- The scene now types "Hello World, my name is …", "I program in …" and "based in …" as 3D extruded
  glyphs: per-character glyph atlas, pop-in typewriter with a blinking cursor, and matrix-style
  scrambles that cycle through the chosen languages.
- Five theme packs replace the generic accent colours: Terminal, Red Team, Blue Team, Frontend Neon
  and Backend Emerald. Each drives colours, backdrop, metal finish, typeface and the glyph alphabet.
- Matrix-rain backdrop drawn on a Compose Canvas with the theme alphabet; Off / Subtle / Full.
- Unit tests for the sentence choreography (`SentenceScriptTest`).
- Build: added DataStore 1.3.0, Navigation Compose 2.10.2, lifecycle-viewmodel-compose, Material
  icons core, JUnit 4.

## 1.0.0 — 2026-11-14

Initial release.

- New Android project (Kotlin 2.4.21, Jetpack Compose BOM 2026.09.00, Material 3, AGP 9.5.1 with
  built-in Kotlin, Gradle 9.9.1, compileSdk 37, minSdk 26).
- 3D "Hello World" scene rendered by Google Filament 1.77.3 via SceneView 4.69.5.
- Runtime glyph extrusion: system-font outlines are turned into watertight metal meshes with
  correct holes (`scene/ExtrudedText.kt`).
- Hero rig idle animation, emissive halo ring and orbiting sparks driven by per-node frame callbacks.
- Transparent Filament surface over a layered Compose backdrop with animated accent glow and vignette.
- Glass control panel: live accent colour picker and Filament render-quality switch.
- Gestures: orbit, pinch-zoom and tap-to-pulse with haptic feedback.
- Edge-to-edge dark UI, adaptive launcher icon, ProGuard rules for Filament/SceneView JNI.
