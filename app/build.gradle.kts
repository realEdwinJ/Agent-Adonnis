plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.adonnis.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adonnis.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Compose BOM ──────────────────────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // ── Compose UI ───────────────────────────────────────────
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // M2 material — richtext-ui-material's RichText reads androidx.compose.material
    // LocalTextStyle/LocalContentColor, so it must be on the compile classpath.
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    // ── Activity & Navigation ────────────────────────────────
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ── Lifecycle ────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // ── Core ─────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.12.0")

    // ── Room (local database) ────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── DataStore (preferences) ──────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ── Security (encrypted storage) ─────────────────────────
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ── Markdown rendering (AI chat & diary rich text) ────────
    // compose-richtext 0.20.0 — built with Kotlin 1.9.20, compatible with
    // this project's Kotlin 1.9.22. Published on Maven Central.
    implementation("com.halilibo.compose-richtext:richtext-commonmark:0.20.0")
    implementation("com.halilibo.compose-richtext:richtext-ui-material:0.20.0")

    // ── OpenRouter AI ────────────────────────────────────────
    // Uses the OpenRouter REST API directly via HttpURLConnection.
    // No SDK dependency — one key, hundreds of models.

    // ── Coroutines ───────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // ── WorkManager (background tasks) ───────────────────────
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ── Debug tooling ────────────────────────────────────────
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
