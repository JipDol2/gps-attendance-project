import java.io.ByteArrayOutputStream
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.gpsattendance.mobile"
    compileSdk = 35

    defaultConfig {
        val configuredBaseUrl = (project.findProperty("BASE_URL") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: "http://10.0.2.2:8080/"

        applicationId = "com.gpsattendance.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"$configuredBaseUrl\"")
        buildConfigField(
            "String",
            "KAKAO_NATIVE_APP_KEY",
            "\"${(project.findProperty("KAKAO_NATIVE_APP_KEY") as? String) ?: ""}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.kakao.maps.open:android:2.13.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

val setupAdbReverseForDebug by tasks.registering {
    group = "android"
    description = "Sets up adb reverse tcp:8080 for debug installs."

    doLast {
        val sdkDirPath = android.sdkDirectory?.absolutePath ?: System.getenv("ANDROID_SDK_ROOT")
        if (sdkDirPath.isNullOrBlank()) {
            logger.lifecycle("Skipping adb reverse: Android SDK path not found.")
            return@doLast
        }

        val adbName = if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            "adb.exe"
        } else {
            "adb"
        }
        val adbFile = File(sdkDirPath, "platform-tools/$adbName")
        if (!adbFile.exists()) {
            logger.lifecycle("Skipping adb reverse: adb not found at ${adbFile.absolutePath}.")
            return@doLast
        }

        val devicesOutput = ByteArrayOutputStream()
        exec {
            commandLine(adbFile.absolutePath, "devices")
            standardOutput = devicesOutput
            isIgnoreExitValue = true
        }

        val hasDevice = devicesOutput.toString()
            .lineSequence()
            .drop(1)
            .any { it.contains("\tdevice") }

        if (!hasDevice) {
            logger.lifecycle("Skipping adb reverse: no connected device.")
            return@doLast
        }

        exec {
            commandLine(adbFile.absolutePath, "reverse", "tcp:8080", "tcp:8080")
            isIgnoreExitValue = true
        }
        logger.lifecycle("adb reverse configured: tcp:8080 -> tcp:8080")
    }
}

tasks.matching { it.name.startsWith("install") && it.name.endsWith("Debug") }
    .configureEach {
        dependsOn(setupAdbReverseForDebug)
    }
