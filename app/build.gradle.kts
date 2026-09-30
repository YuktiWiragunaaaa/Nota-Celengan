plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "id.cukup"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.cukup"
        minSdk = 26
        targetSdk = 36
        // CI mengisi CUKUP_BUILD dengan nomor build supaya tiap APK terbaca sebagai pembaruan.
        versionCode = 940 + (System.getenv("CUKUP_BUILD")?.toIntOrNull() ?: 0)
        versionName = "0.9.4"
    }

    signingConfigs {
        // Kunci tanda tangan dari lingkungan (GitHub Secrets di CI). Tidak pernah disimpan di repo.
        val store = System.getenv("CUKUP_KEYSTORE")?.let(::file)?.takeIf { it.exists() }
        if (store != null) {
            create("sideload") {
                storeFile = store
                storePassword = System.getenv("CUKUP_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CUKUP_KEY_ALIAS") ?: "cukup"
                keyPassword = System.getenv("CUKUP_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("sideload") ?: signingConfigs.getByName("debug")
        }
        // Build cepat untuk dipakai sehari-hari di HP: dioptimalkan R8 dan tidak debuggable (Compose jauh
        // lebih lancar), tapi ID & kunci sama dengan debug, jadi bisa menimpa versi debug tanpa kehilangan data.
        create("fast") {
            initWith(getByName("release"))
            applicationIdSuffix = ".debug"
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.vico.compose.m3)
    implementation(libs.biometric)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
