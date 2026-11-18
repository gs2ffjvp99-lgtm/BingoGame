/*
 * settings.gradle.kts
 * Tells Gradle where to download plugins/libraries from and which modules
 * belong to this project (only ":app").
 */
pluginManagement {
    repositories {
        google()            // Android Gradle Plugin and AndroidX
        mavenCentral()      // JUnit and other open-source libraries
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Force every module to use the repositories declared here.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BingoGame"
include(":app")
