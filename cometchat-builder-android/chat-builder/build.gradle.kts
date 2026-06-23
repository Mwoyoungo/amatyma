plugins {
    id("com.android.application")
    kotlin("android")
    // Apply the CometChat Builder settings plugin
    id("com.cometchat.builder.settings") version "5.0.1"
}

android {
    namespace = "com.cometchat.builder"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35

        var versionCode = 1
        var versionName = "5.2.6"
        var applicationId = "com.cometchat.builder"
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", versionCode.toString())
        buildConfigField("String", "APPLICATION_ID", "\"${applicationId}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    viewBinding {
        enable = true
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // CometChat
    implementation("com.cometchat:chat-uikit-android:5.2.6")
    implementation("com.cometchat:calls-sdk-android:4.3.1")

    // Other
    implementation("com.airbnb.android:lottie:6.5.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.code.gson:gson:2.11.0")
}