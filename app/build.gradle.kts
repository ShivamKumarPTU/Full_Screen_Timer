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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.monktemple"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Add signing configurations for release build
    signingConfigs {
        create("release") {
            storeFile = file("../monktemple-release.keystore")
            storePassword = "MitShivam3@"        // ← Change this
            keyAlias = "monktemple"                    // ← Change this
            keyPassword = "MitShivam3@"          // ← Change this (or the actual key password)
        }

    }

    packagingOptions {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
        }

        create("staging") {
            initWith(getByName("release"))
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".staging"
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

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.sessions)

    // Google Sign-In
    implementation(libs.google.play.services.auth)

    // TimePicker Library
    implementation("nl.joery.timerangepicker:timerangepicker:1.0.0")

    // Animated Vector Drawable & Biometrics
    implementation("androidx.vectordrawable:vectordrawable-animated:1.2.0")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Circle ImageView & MPAndroidChart
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)

    // Swipe Refresh Layout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // For Hilt and WorkManager Integration
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // AI/ML for insights
    implementation("org.tensorflow:tensorflow-lite:2.17.0")

    // Encryption
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.cardview:cardview:1.0.0")
}
