import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("plugin.serialization") version "2.0.20"
}

android {
    namespace = "de.timklge.karooreminder"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.timklge.karooreminder"
        minSdk = 26
        targetSdk = 34
        versionCode = 100 + (System.getenv("BUILD_NUMBER")?.toInt() ?: 1)
        versionName = System.getenv("RELEASE_VERSION") ?: "1.0"
    }

    signingConfigs {
        create("release") {
            val env: MutableMap<String, String> = System.getenv()
            keyAlias = env["KEY_ALIAS"]
            keyPassword = env["KEY_PASSWORD"]

            val base64keystore: String = env["KEYSTORE_BASE64"] ?: ""
            val keystoreFile: File = File.createTempFile("keystore", ".jks")
            keystoreFile.writeBytes(Base64.getDecoder().decode(base64keystore))
            storeFile = keystoreFile
            storePassword = env["KEYSTORE_PASSWORD"]
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.register("addGoogleServicesJson") {
    description = "Adds google-services.json to the project"
    group = "build"

    doLast {
            val googleServicesJson = System.getenv("GOOGLE_SERVICES_JSON_BASE64")
            ?.let { Base64.getDecoder().decode(it) }
            ?.let { String(it) }
        if (googleServicesJson != null) {
            val jsonFile = file("$projectDir/google-services.json")
            jsonFile.writeText(googleServicesJson)
            println("Added google-services.json to the project")
        } else {
            println("No GOOGLE_SERVICES_JSON_BASE64 environment variable found, skipping...")
        }
    }
}

tasks.register("generateManifest") {
    description = "Generates manifest.json with current version information"
    group = "build"

    doLast {
        val manifestFile = file("$projectDir/manifest.json")
        val manifest = mapOf(
            "label" to "karoo-reminder",
            "packageName" to "de.timklge.karooreminder",
            "iconUrl" to "https://github.com/timklge/karoo-reminder/releases/latest/download/karoo-reminder.png",
            "latestApkUrl" to "https://github.com/timklge/karoo-reminder/releases/latest/download/app-release.apk",
            "latestVersion" to android.defaultConfig.versionName,
            "latestVersionCode" to android.defaultConfig.versionCode,
            "developer" to "timklge",
            "description" to "Shows in-ride alerts after a given time interval, distance or HR / power / speed / cadence out of range",
            "releaseNotes" to "* Add rolling average setting for power triggers"
        )

        val gson = groovy.json.JsonBuilder(manifest).toPrettyString()
        manifestFile.writeText(gson)
        println("Generated manifest.json with version ${android.defaultConfig.versionName} (${android.defaultConfig.versionCode})")
    }
}

tasks.named("assemble") {
    dependsOn("generateManifest")
    dependsOn("addGoogleServicesJson")
}

dependencies {
    implementation(libs.hammerhead.karoo.ext)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.androidx.lifeycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose.ui)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.color)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
}