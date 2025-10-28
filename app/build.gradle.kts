@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.monktemple"
    // Using compileSdk 34 as it's a stable choice and matches targetSdk.
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.monktemple"
        // Choosing minSdk 24 for broader compatibility as intended in your latest changes.
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        // Using Java 1.8 as it is the standard for modern Android development.
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Firebase - Using the complete and correct block from your latest changes
    implementation(platform(libs.firebase.bom))
    // The following names assume you have corrected your libs.versions.toml to remove "-ktx"
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.sessions)

    // Google Sign-In
    implementation(libs.google.play.services.auth)

    // TimePicker Library
    implementation("nl.joery.timerangepicker:timerangepicker:1.0.0")

    // Animated Vector Drawable & Biometrics
    implementation(libs.animated.vector.drawable)
    implementation("androidx.biometric:biometric:1.1.0") // Using stable version

    // Circle ImageView & MPAndroidChart
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Room Database
    val room_version = "2.6.1" // Using a known stable version
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // Swipe Refresh Layout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Hilt for Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51.1") // Using a stable version
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    // For Hilt and WorkManager Integration
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}