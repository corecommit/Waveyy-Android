// Root build file: plugin versions are managed via gradle/libs.versions.toml.
// Per-module plugins are applied in :app. No repositories here on purpose
// (dependencyResolutionManagement in settings.gradle.kts enforces FAIL_ON_PROJECT_REPOS).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}
