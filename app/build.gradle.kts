plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.lifengqiang.movement"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lifengqiang.movement"
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
    buildFeatures{
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    implementation ("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation ("com.google.android.exoplayer:exoplayer-dash:2.19.1")
    implementation ("com.google.android.exoplayer:exoplayer-ui:2.19.1")

    implementation ("com.arthenica:ffmpeg-kit-video:5.1.LTS")
    implementation ("org.florescu.android.rangeseekbar:rangeseekbar-library:0.3.0")

    implementation("androidx.compose.ui:ui-android:1.5.4")

    debugImplementation ("androidx.compose.ui:ui-tooling:1.5.2")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.5.2")

    implementation("androidx.compose.foundation:foundation:1.5.2")

    implementation("androidx.compose.material:material:1.5.2")
}