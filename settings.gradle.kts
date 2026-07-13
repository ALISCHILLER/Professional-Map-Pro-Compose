// Android-only Jetpack Compose project. No multiplatform targets are configured.
pluginManagement {
    repositories {
        maven(url = uri("https://maven.aliyun.com/repository/google"))
        maven(url = uri("https://maven.aliyun.com/repository/gradle-plugin"))
        maven(url = uri("https://maven.aliyun.com/repository/central"))
        maven(url = uri("https://maven.aliyun.com/repository/public"))
        // Prefer official repositories, then use mirrors as network fallbacks.
        google()
        mavenCentral()
        gradlePluginPortal()

        maven(url = uri("https://jitpack.io"))
    }
}


dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = uri("https://maven.aliyun.com/repository/google"))
        maven(url = uri("https://maven.aliyun.com/repository/central"))
        maven(url = uri("https://maven.aliyun.com/repository/public"))
        google()
        mavenCentral()

        maven(url = uri("https://jitpack.io"))
    }
}

rootProject.name = "ProfessionalMapPro"

include(":app")
include(":benchmark")
include(":core:model")
include(":core:geo")
include(":core:location")
include(":core:mapdata")
include(":core:routing")
include(":core:progress")
include(":core:guidance")
include(":core:offline")
include(":core:observability")
include(":feature:map")
include(":core:service")
