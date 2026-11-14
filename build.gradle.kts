// Top-level build file: plugin versions live in gradle/libs.versions.toml.
//
// AGP 9 ships built-in Kotlin support, so the org.jetbrains.kotlin.android plugin is no longer
// applied. The Kotlin Gradle plugin is pinned on the build classpath so the compiler matches the
// Compose compiler plugin and can read metadata from libraries built with Kotlin 2.4 (SceneView).
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
