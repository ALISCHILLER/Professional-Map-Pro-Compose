plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

fun String.gradleStringLiteral(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val osrmBaseUrl = providers.gradleProperty("PMP_OSRM_BASE_URL")
    .orElse(providers.environmentVariable("PMP_OSRM_BASE_URL"))
    .getOrElse("https://router.project-osrm.org")
val routingUserAgent = providers.gradleProperty("PMP_ROUTING_USER_AGENT")
    .orElse(providers.environmentVariable("PMP_ROUTING_USER_AGENT"))
    .getOrElse("ProfessionalMapPro/1.0")

android {
    namespace = "com.msa.professionalmap.feature.map"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "OSRM_BASE_URL", osrmBaseUrl.gradleStringLiteral())
        buildConfigField("String", "ROUTING_USER_AGENT", routingUserAgent.gradleStringLiteral())
        buildConfigField("boolean", "ALLOW_CLEARTEXT_ROUTING", "false")
    }

    buildFeatures { buildConfig = true }

    buildTypes {
        debug {
            buildConfigField("boolean", "ALLOW_CLEARTEXT_ROUTING", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:geo"))
    implementation(project(":core:location"))
    implementation(project(":core:mapdata"))
    implementation(project(":core:routing"))
    implementation(project(":core:progress"))
    implementation(project(":core:guidance"))
    implementation(project(":core:offline"))
    implementation(project(":core:service"))
    implementation(project(":core:observability"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.accompanist.permissions)
    implementation(libs.ktor.client.android)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.maplibre.android)
    // For devices/emulators where Vulkan causes rendering issues, replace the line above with:
    // implementation(libs.maplibre.android.opengl)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
