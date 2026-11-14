# Filament and SceneView talk to native code through JNI; keep their public surface intact.
-keep class com.google.android.filament.** { *; }
-keep class io.github.sceneview.** { *; }
-dontwarn com.google.android.filament.**
