// The watch version: the same game, played by touch or with the crown.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.jjmluz.nationslingo.wear"
    compileSdk = 37

    defaultConfig {
        // Same package as the TV app, as one app for both form factors.
        applicationId = "com.jjmluz.nationslingo"
        minSdk = 30
        targetSdk = 37
        // CI passes these for releases (-PversionCode=… -PversionName=…); local builds use the defaults.
        versionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(6)
        versionName = providers.gradleProperty("versionName").getOrElse("1.4.0")
    }

    // Signed like the TV app; see app/build.gradle.kts.
    val releaseKeystore = providers.environmentVariable("SIGNING_KEYSTORE").orNull
    signingConfigs {
        if (releaseKeystore != null) create("release") {
            storeFile = file(releaseKeystore)
            storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    // Only for the one-handed gesture (double pinch); the screens are drawn with plain Compose.
    implementation("androidx.wear.compose:compose-material3:1.7.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    testImplementation("junit:junit:4.13.2")
}
