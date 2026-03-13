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
        mavenLocal()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "AndroidLib"
include(":app")
include(":androidLib")
include(":samples:showcase-android")

// Composite build: include FSLib for running the showcase-fullstack server locally.
// The showcase backend serves both its KVision web frontend AND Android clients
// via the /apiContract endpoint for JSON-RPC route discovery.
includeBuild("../../IdeaProjects/fsLib") {
    name = "fsLib"
}
