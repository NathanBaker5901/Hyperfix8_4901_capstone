plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.blocklens"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.blocklens"
        minSdk = 24
        targetSdk = 34
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2" // Use the latest version
    }
}

dependencies {
    // Object detection
    implementation("com.google.mlkit:object-detection:17.0.2")

    // Retrofit for making network calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")

    // Material3 (Jetpack Compose)
    implementation("androidx.compose.material3:material3:1.2.0")

    // Compose Activity
    implementation("androidx.activity:activity-compose:1.8.2")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.2.2")

    // CameraX dependencies
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")

    // Compose BOM (Bill of Materials for Compose version alignment)
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))

    // Core Compose dependencies
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui.graphics)
    testImplementation(libs.junit.junit)

    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    // Dependencies for animated screen
    implementation("androidx.core:core-splashscreen:1.0.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("io.coil-kt:coil-compose:2.4.0")


}

