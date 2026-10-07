plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.waveyy.baselineprofile"
    compileSdk = libs.versions.compileSdk.get().toInt()

    // This module instruments :app to generate baseline profiles.
    targetProjectPath = ":app"

    defaultConfig {
        // Macrobenchmark requires API 28+.
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.test.uiautomator)
    implementation(libs.test.espresso.core)
    implementation(libs.test.runner)
    implementation(libs.test.ext.junit)
}
