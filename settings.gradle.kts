pluginManagement {
    // The convention plugins (PLANNING.md §1): every module's Android, Kotlin, Compose, Hilt
    // and Room setup is written once, in build-logic/, and applied by id.
    includeBuild("build-logic")
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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tetto"

// The module map is PLANNING.md §2. core:model and core:domain are pure Kotlin/JVM; feature
// modules depend on core:*, never on each other; :app wires everything together.
// A module joins here in the phase that brings its first code.
include(":app")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:reminder")
include(":core:designsystem")
include(":core:testing")
include(":feature:caps")
include(":feature:onboarding")
include(":feature:settings")
include(":feature:guide")
include(":widget")
