/*
 * app/build.gradle.kts
 * Build settings for the Bingo app module.
 *
 * The app only uses classes that ship with Android itself
 * (Activity, SQLiteOpenHelper, GridLayout ...), so no extra libraries are
 * needed to run it. JUnit is used only for the plain-Java unit tests.
 */
plugins {
    id("com.android.application")
}

android {
    // Package name used for the generated R class.
    namespace = "com.example.bingogame"
    // Android API level used to compile the code.
    compileSdk = 35

    defaultConfig {
        // Unique id of the app on a device / in the Play Store.
        applicationId = "com.example.bingogame"
        // Oldest Android version supported: 7.0 (API 24).
        minSdk = 24
        // targetSdk 34 keeps the classic window layout (action bar below the
        // status bar). Android 15+ forces "edge-to-edge" drawing for apps that
        // target 35+, which would need extra window-inset handling code.
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // No code shrinking: keeps this classroom project simple.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        // Java 11 language level (lambdas, try-with-resources, etc.).
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Unit tests for the game rules (run with: Run 'BingoGameTest').
    testImplementation("junit:junit:4.13.2")
}
