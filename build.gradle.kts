// Android-only Jetpack Compose build. Kotlin/JVM modules are internal libraries for the Android app.
buildscript {
    repositories {
        // Resolve official plugin artifacts first. Mirrors are fallback-only because
        // some mirrors do not publish Gradle plugin-marker metadata consistently.
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(url = uri("https://maven.aliyun.com/repository/google"))
        maven(url = uri("https://maven.aliyun.com/repository/gradle-plugin"))
        maven(url = uri("https://maven.aliyun.com/repository/central"))
        maven(url = uri("https://maven.aliyun.com/repository/public"))
    }
    dependencies {
        // AGP 9 provides built-in Kotlin. This classpath deliberately upgrades its
        // runtime KGP from 2.2.10 to the project compiler version used by JVM and
        // compiler plugins. This is the AGP 9-supported upgrade mechanism.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")

        // Do not resolve Firebase Gradle plugins for source builds that do not ship
        // a real app/google-services.json. This removes an unnecessary network failure
        // and fixes the plugin-marker error reported on restricted networks.
        if (rootProject.file("app/google-services.json").isFile) {
            classpath("com.google.gms:google-services:4.5.0")
            classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.7")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
