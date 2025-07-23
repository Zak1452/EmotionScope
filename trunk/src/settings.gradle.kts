pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://chaquo.com/maven")
    }
    plugins {
        id("com.android.application")       version "8.9.0"
        id("org.jetbrains.kotlin.android") version "1.9.0"
        id("com.chaquo.python")            version "16.0.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://chaquo.com/maven")
        maven("https://jitpack.io")
        maven {
            name = "TarsosDSP-Arthenica"
            url  = uri("https://repo.arthenica.com/maven2")
        }
        maven {
            name = "TarsosDSP-0110"
            url  = uri("https://mvn.0110.be/releases")
        }
    }
}

rootProject.name = "EmotionScope"
include(":app")