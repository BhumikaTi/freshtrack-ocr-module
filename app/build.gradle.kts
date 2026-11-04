import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.freshtrack.ocr"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val localProps = Properties()
        val localFile = rootProject.file("local.properties")

        if (localFile.exists()) {
            localFile.inputStream().use {
                localProps.load(it)
            }
        }

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProps.getProperty("GEMINI_API_KEY", "")}\""
        )
        // Optional overrides in local.properties (handy while testing on the free tier):
        //   GEMINI_CHAT_MODEL=gemini-3.1-flash-lite
        //   GEMINI_USE_SEARCH=false   (barcode AI fallback uses Google Search grounding, which the free tier may not allow)
        buildConfigField(
            "String",
            "GEMINI_CHAT_MODEL",
            "\"${localProps.getProperty("GEMINI_CHAT_MODEL", "gemini-3.1-flash-lite")}\""
        )
        buildConfigField(
            "boolean",
            "GEMINI_USE_SEARCH",
            localProps.getProperty("GEMINI_USE_SEARCH", "false")
        )

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProps.getProperty("SUPABASE_URL", "")}\""
        )

        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${localProps.getProperty("SUPABASE_PUBLISHABLE_KEY", "")}\""
        )

        applicationId = "com.freshtrack.ocr"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation(libs.androidx.activity)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // ExifInterface
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // CameraX
    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Google ML Kit OCR
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Room Database
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    kapt("androidx.room:room-compiler:2.8.5")


    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Barcode scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Networking: Open Food Facts API
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4") // ChatViewModel

    // Logging
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil: product photos
    implementation("io.coil-kt:coil:2.7.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Supabase Auth
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.3"))
    implementation("io.github.jan-tennert.supabase:auth-kt")

    // Ktor Android client
    implementation("io.ktor:ktor-client-android:3.0.3")
}
