plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val firebaseConfigFile = file("google-services.json")
val firebaseConfigured = firebaseConfigFile.isFile
if (firebaseConfigured) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
} else {
    logger.lifecycle("Firebase plugins are disabled because app/google-services.json is missing.")
}

fun environmentOrProperty(name: String): String? = providers.gradleProperty(name)
    .orElse(providers.environmentVariable(name))
    .orNull
    ?.trim()
    ?.takeIf(String::isNotEmpty)

val releaseStoreFilePath = environmentOrProperty("PMP_RELEASE_STORE_FILE")
val releaseStorePassword = environmentOrProperty("PMP_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = environmentOrProperty("PMP_RELEASE_KEY_ALIAS")
val releaseKeyPassword = environmentOrProperty("PMP_RELEASE_KEY_PASSWORD")
val releaseVersionCode = environmentOrProperty("PMP_VERSION_CODE")?.toIntOrNull()
val releaseVersionName = environmentOrProperty("PMP_VERSION_NAME")
val versionCodeValue = releaseVersionCode ?: 1
val versionNameValue = releaseVersionName ?: "1.0.0"
val telemetryDefaultEnabled = environmentOrProperty("PMP_TELEMETRY_DEFAULT_ENABLED")
    ?.toBooleanStrictOrNull()
    ?: false

fun validateReleaseConfiguration() {
    val missingSigningValues = buildList {
        if (releaseStoreFilePath == null) add("PMP_RELEASE_STORE_FILE")
        if (releaseStorePassword == null) add("PMP_RELEASE_STORE_PASSWORD")
        if (releaseKeyAlias == null) add("PMP_RELEASE_KEY_ALIAS")
        if (releaseKeyPassword == null) add("PMP_RELEASE_KEY_PASSWORD")
        if (releaseVersionCode == null) add("PMP_VERSION_CODE")
        if (releaseVersionName == null) add("PMP_VERSION_NAME")
    }
    require(missingSigningValues.isEmpty()) {
        "Release configuration is mandatory. Missing: ${missingSigningValues.joinToString()}"
    }
    require(file(requireNotNull(releaseStoreFilePath)).isFile) {
        "Release keystore does not exist: $releaseStoreFilePath"
    }
    require(requireNotNull(releaseVersionCode) > 0) { "PMP_VERSION_CODE must be a positive integer." }
    require(!releaseVersionName.isNullOrBlank()) { "PMP_VERSION_NAME must not be blank." }
}

tasks.configureEach {
    if (name.contains("release", ignoreCase = true)) {
        doFirst {
            validateReleaseConfiguration()
        }
    }
}

android {
    namespace = "com.msa.professionalmap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.msa.professionalmap"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeValue
        versionName = versionNameValue
        resourceConfigurations += listOf("en", "fa")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())
        buildConfigField("boolean", "TELEMETRY_DEFAULT_ENABLED", telemetryDefaultEnabled.toString())
    }

    signingConfigs {
        if (
            releaseStoreFilePath != null &&
            releaseStorePassword != null &&
            releaseKeyAlias != null &&
            releaseKeyPassword != null
        ) {
            create("release") {
                storeFile = file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "TELEMETRY_DEFAULT_ENABLED", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("release")?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    packaging {
        resources {
            // Do not discard third-party LICENSE/NOTICE resources. Distribution
            // obligations and project licensing are documented in LICENSE.md.
            excludes += setOf("META-INF/DEPENDENCIES")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":feature:map"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(project(":core:observability"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.profileinstaller)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.leakcanary.android)
}
