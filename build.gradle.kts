plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
}

// Convenience task: start the FSLib showcase server (backend + web frontend + /apiContract)
tasks.register("runShowcaseServer") {
    group = "application"
    description = "Starts the FSLib showcase server on http://localhost:8080"
    dependsOn(gradle.includedBuild("fsLib").task(":samples:fullstack:showcase:showcase-app:run"))
}