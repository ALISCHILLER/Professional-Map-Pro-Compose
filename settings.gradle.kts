pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        maven(url = uri("https://maven.aliyun.com/repository/google"))
        maven(url = uri("https://maven.aliyun.com/repository/gradle-plugin"))
        maven(url = uri("https://maven.aliyun.com/repository/public"))
        maven(url = uri("https://maven.aliyun.com/repository/central"))
        maven(url = uri("https://redirector.kotlinlang.org/maven/compose-dev"))
        maven(url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev"))
        maven(url = uri("https://packages.jetbrains.team/maven/p/cmp/dev"))
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven(url = uri("https://maven.aliyun.com/repository/google"))
        maven(url = uri("https://maven.aliyun.com/repository/gradle-plugin"))
        maven(url = uri("https://maven.aliyun.com/repository/public"))
        maven(url = uri("https://maven.aliyun.com/repository/central"))
        maven(url = uri("https://redirector.kotlinlang.org/maven/compose-dev"))
        maven(url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev"))
        maven(url = uri("https://packages.jetbrains.team/maven/p/cmp/dev"))
    }
}

rootProject.name = "ProfessionalMapPro"

include(":app")
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
