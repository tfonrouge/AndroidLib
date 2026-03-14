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

// ---------------------------------------------------------------------------
// Maven Central publishing via Sonatype Central Portal
//
// Usage:
//   1. ./gradlew publishAllPublicationsToStagingRepository
//   2. ./gradlew publishToCentralPortal
// ---------------------------------------------------------------------------
tasks.register("publishToCentralPortal", Exec::class) {
    description = "Uploads the staging-deploy bundle to Maven Central Portal"
    group = "publishing"

    val stagingDir = layout.buildDirectory.dir("staging-deploy")
    val bundleFile = layout.buildDirectory.file("central-bundle.zip")
    val username = providers.gradleProperty("ossrhUsername")
    val password = providers.gradleProperty("ossrhPassword")

    inputs.dir(stagingDir)
    outputs.file(bundleFile)

    doFirst {
        val staging = stagingDir.get().asFile
        if (!staging.exists() || staging.listFiles()?.isEmpty() != false) {
            error("No staged artifacts found. Run publishAllPublicationsToStagingRepository first.")
        }

        // Create ZIP bundle from staging directory
        ant.withGroovyBuilder {
            "zip"("destfile" to bundleFile.get().asFile, "basedir" to staging)
        }

        val user = username.getOrElse("")
        val pass = password.getOrElse("")
        if (user.isBlank() || pass.isBlank()) {
            error("ossrhUsername/ossrhPassword not set in ~/.gradle/gradle.properties")
        }

        val authToken = java.util.Base64.getEncoder()
            .encodeToString("$user:$pass".toByteArray())

        commandLine(
            "curl", "-s", "-w", "\n%{http_code}",
            "--fail-with-body",
            "-X", "POST",
            "https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC",
            "-H", "Authorization: UserToken $authToken",
            "-F", "bundle=@${bundleFile.get().asFile.absolutePath}"
        )
    }
}