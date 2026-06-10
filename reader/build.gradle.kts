import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library") version "8.7.3"
    kotlin("android") version "2.1.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    id("org.jetbrains.kotlinx.kover") version "0.7.6"
}

android {
    namespace = "com.sudokuengine.reader"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // OpenCV Android SDK (official Maven Central distribution) — replaces opencv-python.
    implementation("org.opencv:opencv:4.11.0")
    // LiteRT (Google AI Edge) — runs the committed digit model.
    implementation("com.google.ai.edge.litert:litert:1.0.1")

    // Pure-JVM unit tests (assembly/confidence logic with a fake DigitClassifier).
    testImplementation(kotlin("test"))

    // Instrumented oracle: OpenCV + LiteRT need the Android runtime + a device/emulator.
    androidTestImplementation(kotlin("test"))
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

// Kover wiring for the Android `debug` variant (CLAUDE.md quality bar: >=80%).
//
// Coverage here is measured from `testDebugUnitTest` (pure JVM); the OpenCV/LiteRT adapters
// (`opencv.*`, `litert.*`) and the OpenCV-backed orchestrator can only be exercised by the
// instrumented oracle in `src/androidTest`, which doesn't run in this environment. Excluding
// those adapter packages keeps the bound meaningful for the pure-Kotlin orchestration/assembly
// logic (`GridAssembler`, `ReaderTypes`) that IS unit-tested here, rather than gaming the number.
koverReport {
    filters {
        excludes {
            packages(
                "com.sudokuengine.reader.opencv",
                "com.sudokuengine.reader.litert",
            )
        }
    }

    androidReports("debug") {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
