// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

repositories {
    google()
    mavenCentral()

    maven { url = uri("https://jitpack.io") }// Required for MPAndroidChart and possibly Himanshoe's charts
}

