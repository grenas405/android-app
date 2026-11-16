# Hello World 3D

An animated developer profile for Android, rendered in real time by **Google Filament** through the
**SceneView** Compose DSL. The type is real geometry, not textures or billboards: the app pulls glyph outlines
from the system font, extrudes them into watertight 3D meshes and shades them as physically based
metal, lit by an HDR environment with bloom. The scene types out

> Hello World, my name is **{name}**. I program in **{languages}**. Based in **{location}**.

with a typewriter cursor and matrix-style glyph scrambles, over a themed matrix-rain backdrop.

## What you see

- *HELLO WORLD* in extruded themed metal, then three lines of 3D glyph text that type in letter by
  letter. The personal parts (name, language, location) render in the accent metal.
- Languages cycle with a matrix scramble: each glyph flickers through the theme's alphabet and
  settles left to right.
- The official logo of the language on screen pops in beside its name, and the Settings chips carry
  the same logos.
- A breathing idle sway on the whole typographic block, an emissive halo ring, orbiting sparks and a
  contact shadow.
- Matrix rain behind the viewport, drawn with the theme's alphabet (katakana, binary, hex, markup, JSON).
- Five theme packs from security and web development: **Terminal**, **Red Team**, **Blue Team**,
  **Frontend Neon**, **Backend Emerald**. Each sets colours, backdrop, metal finish, typeface and rain glyphs.
- A Settings page for name, location, languages (curated chips plus custom entries), theme, Filament
  render quality and rain density. Everything persists with Preferences DataStore.
- Gestures: drag to orbit, pinch to zoom, tap to pulse the type (with haptics).

## Stack

| Layer | Library | Version |
| --- | --- | --- |
| Language | Kotlin | 2.4.21 |
| UI | Jetpack Compose (BOM) / Material 3 | 2026.09.00 |
| 3D DSL | [SceneView](https://github.com/SceneView/sceneview-android) | 4.69.5 |
| Renderer | [Google Filament](https://github.com/google/filament) | 1.77.3 |
| Build | Android Gradle Plugin (built-in Kotlin) / Gradle | 9.5.1 / 9.9.1 |
| SDK | compileSdk 37, targetSdk 37, minSdk 26 | |

## Project layout

```
app/src/main/kotlin/dev/grenas/hello3d/
├── MainActivity.kt                 Edge-to-edge activity hosting the navigation root
├── data/
│   ├── Profile.kt                  Profile model, RenderPreset and RainIntensity enums
│   ├── Languages.kt                Curated language list and normalisation
│   ├── LanguageLogos.kt            Language name (and aliases) → official logo drawable
│   ├── ProfileRepository.kt        Preferences DataStore persistence
│   └── ProfileViewModel.kt         StateFlow<Profile?> plus intent-named edits
├── scene/
│   ├── ExtrudedText.kt             Glyph outline → extruded mesh (caps, holes, walls, advances)
│   ├── GlyphAtlas.kt               Per-character mesh cache and shared GPU glyph geometry
│   ├── LogoBadge.kt                Language logo badges and the textured quad that shows them
│   ├── TextLine3D.kt               One 3D text line: glyph nodes, pop-in, in-place scramble, cursor
│   ├── SentenceScript.kt           Pure-Kotlin choreography: typewriter, scramble, language loop
│   └── ProfileScene.kt             Filament scene: themed materials, lights, camera, nodes, animation
└── ui/
    ├── Navigation.kt               NavHost (profile ⇄ settings), engine and view model owners
    ├── ProfileScreen.kt            Backdrop, matrix rain, header, theme panel, warm-up veil
    ├── SettingsScreen.kt           Identity, languages, theme cards, render options
    ├── MatrixRain.kt               Canvas glyph rain driven by the theme alphabet
    ├── components/Glass.kt         Shared frosted panels and pills
    └── theme/                      ThemePacks.kt (five packs) and Theme.kt (Material 3 mapping)
app/src/main/res/drawable-nodpi/logo_*.webp           Language logos (256 px, lossless)
app/src/test/kotlin/.../scene/SentenceScriptTest.kt   Unit tests for the choreography
app/src/test/kotlin/.../data/LanguageLogosTest.kt     Unit tests for the logo mapping
```

### How the 3D text is built

`ExtrudedText` asks `Paint.getTextPath` for the outlines, walks them with androidx `PathIterator`
(lines stay exact, curves are flattened adaptively), classifies contours into outer rings and holes
by containment depth, triangulates the caps with SceneView's ear-clipping triangulator and stitches
hard-edged side walls. The result is uploaded once as a Filament `VertexBuffer`/`IndexBuffer`
through SceneView's `Geometry.Builder` and rendered with a `GeometryNode`. The hero words are one
mesh each. The sentence lines use `GlyphAtlas`, which caches one baseline-anchored mesh per
character and lays glyphs out by pen advance. Every visible glyph is its own node, but all nodes
showing the same character share one vertex/index buffer pair (`GlyphGeometries`); a scramble
re-points a node at another character's buffers, so the typewriter and matrix effects neither
rebuild the scene graph nor upload meshes.

### Language logos

`LanguageLogos` maps a language name to a bundled logo. The artwork is the full-colour set from
[devicon](https://github.com/devicons/devicon) (MIT), rasterised to 256 px lossless WebP. In the
scene, `LogoBadges` draws each logo unmodified on a light rounded plate (several marks are black and
would vanish on the dark themes) and `LanguageBadge` shows it on one textured quad that trails the
language name. SQL and Assembly have no official logo. The logos are trademarks of their respective
owners and are used only to identify the languages.

### How the sentence is choreographed

`SentenceScript` is plain Kotlin: it turns a `Profile` into an intro (type the name line, type the
languages label, scramble in the first language, type the location) and a loop (scramble to each
next language, hold, repeat). `play` writes `LineState`s into a sink with injectable delays, which
is what the unit tests exercise. In the app the sink is a Compose-backed `LineBoard`, and
`TextLine3D` recomposes the glyph nodes from it.

## Build and run

Requirements: JDK 17 and an Android SDK with platform 37 (Android Studio ships both; the SDK
Manager offers API 37 if it is missing).

```bash
./gradlew assembleDebug            # Linux / macOS / Git Bash
gradlew.bat assembleDebug          # Windows
```

The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk`. Install it with
`adb install -r` on a device or emulator with OpenGL ES 3.0 (any modern phone, or an x86_64
emulator image with hardware GPU acceleration).

To open in Android Studio: **File → Open** the repository root. A `local.properties` file pointing
`sdk.dir` at your SDK is created automatically; otherwise set `ANDROID_HOME`.

### Headless build on this dev VM

The VM has no system JDK or SDK on the PATH. The ones used for the verified build live in the
user's home:

```bash
export JAVA_HOME=~/jdk ANDROID_HOME=~/android-sdk
./gradlew assembleDebug
```

## Notes

- `SurfaceType.TextureSurface` with `isOpaque = false` lets the Compose gradient and accent glow
  show through behind the geometry.
- `FrameRatePolicy.Continuous` keeps the loop running because every frame animates; a static scene
  should use `OnDemand`.
- Render quality presets map to Filament view settings (MSAA, SSAO, bloom, shadows) and can be
  switched live from the panel.
- AGP 9 compiles Kotlin itself (built-in Kotlin). The Kotlin Gradle plugin is pinned to 2.4.21 on
  the build classpath so the compiler matches the Compose compiler plugin and can read SceneView's
  Kotlin 2.4 metadata.
- SceneView's classes target Java 21, so local unit tests (which run on the build JDK 17) must not
  load them. The data layer therefore uses its own `RenderPreset` enum, mapped to Filament's
  `RenderQuality` in the UI. Run the tests with `./gradlew testDebugUnitTest`.
