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
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Self-contained build for the Android app module. Run from the repo root with
// `./gradlew -p app <task>`. Wired up to the solver and `:reader` modules via the composite
// builds below.
rootProject.name = "app"

// Composite builds: pull in the solver (root) and :reader as included builds so this app
// module can depend on `com.sudokuengine:sudokuengine` and `com.sudokuengine:reader` via
// Gradle's automatic dependency substitution.
includeBuild("..")
includeBuild("../reader")
