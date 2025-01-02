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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2" // Use the latest version
    }
}

dependencies {
    // Material3 (Jetpack Compose)
    implementation("androidx.compose.material3:material3:1.2.0")

    // Compose Activity
    implementation("androidx.activity:activity-compose:1.7.2")

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

    // Debugging tools
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Unit testing dependencies
    testImplementation("junit:junit:4.13.2")

    // Instrumented testing dependencies
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Compose testing dependencies
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


