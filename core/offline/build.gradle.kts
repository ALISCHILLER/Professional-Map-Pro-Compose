plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.msa.professionalmap.core.offline"
    compileSdk = 36

    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.maplibre.android)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
}
