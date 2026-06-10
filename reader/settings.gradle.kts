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

// Self-contained build for the Android reader module. Run from the repo root with
// `./gradlew -p reader <task>`. Folds into the app project as `:reader` later with no code change.
rootProject.name = "reader"
