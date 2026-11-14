# Hello World 3D

An immersive "Hello, World" for Android, rendered in real time by **Google Filament** through the
**SceneView** Compose DSL. The words *HELLO* and *WORLD* are not textures or billboards: at launch
the app pulls the system font's glyph outlines, extrudes them into watertight 3D meshes and shades
them as physically based metal, lit by an HDR environment with bloom.

## What you see

- Two lines of extruded type. *HELLO* in chrome, *WORLD* in a live accent colour.
- A slow, breathing idle animation on the hero rig and an emissive halo ring with a bright bead.
- A swarm of glowing sparks on tilted orbits that catch the bloom pass.
- A soft contact shadow grounding the type over a layered Compose backdrop.
- A glass control panel: pick the accent colour and switch Filament's render quality preset.
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
├── MainActivity.kt                 Edge-to-edge activity hosting the Compose screen
├── scene/
│   ├── ExtrudedText.kt             Glyph outline → extruded mesh (caps, holes, walls)
│   └── HelloWorldScene.kt          Filament scene: materials, lights, camera, nodes, animation
└── ui/
    ├── HelloWorldScreen.kt         Backdrop, header, glass controls, warm-up veil
    └── theme/Theme.kt              Palette and typography
```

### How the 3D text is built

`ExtrudedText` asks `Paint.getTextPath` for the outlines, walks them with androidx `PathIterator`
(lines stay exact, curves are flattened adaptively), classifies contours into outer rings and holes
by containment depth, triangulates the caps with SceneView's ear-clipping triangulator and stitches
hard-edged side walls. The result is uploaded once as a Filament `VertexBuffer`/`IndexBuffer`
through SceneView's `Geometry.Builder` and rendered with a `GeometryNode`.

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
