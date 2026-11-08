plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.temproot.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.temproot.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
