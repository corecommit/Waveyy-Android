import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Secrets come from local.properties (never committed); dummies keep debug builds compiling.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun localProperty(key: String, default: String): String =
    localProperties.getProperty(key, default).trim().ifEmpty { default }

android {
    namespace = "com.waveyy.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.waveyy.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        buildConfigField("String", "API_BASE_URL", "\"${localProperty("API_BASE_URL", "https://example.com/api/")}\"")
        buildConfigField("String", "SOCKET_URL", "\"${localProperty("SOCKET_URL", "https://example.com")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${localProperty("SUPABASE_URL", "https://example.supabase.co")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperty("SUPABASE_ANON_KEY", "dummy-key-for-local-builds-only")}\"")
        buildConfigField("String", "SAAVN_API_URL", "\"${localProperty("SAAVN_API_URL", "")}\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

detekt {
    config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
    // Unit tests live under src/test and are excluded from static analysis;
    // assertion literals would otherwise trip naming/magic-number rules.
    source.setFrom(files("src/main/java"))
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.adaptive.navigation.suite)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Playback (Media3) — wired in Phase 1, used in Phase 4.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Networking & serialization — used in Phase 3.
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.guava)

    // Local persistence — used in Phase 3.
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // Media loading / backend clients — used in Phases 3/6/10.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    // Ktor engine required by the Supabase Kotlin client (no Ktor version pinned in TOML).
    implementation("io.ktor:ktor-client-okhttp:3.0.0")
    implementation(libs.socketio.client)

    implementation(libs.timber)
    debugImplementation(libs.leakcanary)

    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.room.testing)
}
