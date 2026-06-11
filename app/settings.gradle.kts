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
// `./gradlew -p app <task>`. Wires up to the solver and `:reader` modules in a later PR.
rootProject.name = "app"
