import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.serialization)
    id("maven-publish")
    signing
}

android {
    namespace = "com.fonrouge.fslib.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 30

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    buildFeatures {
        compose = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// Append "-SNAPSHOT" when -PSNAPSHOT is passed.
// Usage: ./gradlew publishToMavenLocal -PSNAPSHOT
val publishVersion = libs.versions.fslibAndroid.get().let { base ->
    if (hasProperty("SNAPSHOT") && !base.endsWith("-SNAPSHOT")) "$base-SNAPSHOT" else base
}

val fslibOverride = findProperty("fslibVersion") as? String

dependencies {
    api(fslibOverride?.let { "com.fonrouge.fslib:core:$it" } ?: libs.fslib.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.paging.compose)
    implementation(platform(libs.androidx.compose.bom))

    api(libs.compose.material3.pullrefresh)

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.okhttp)
    api(libs.ktor.client.android)
    api(libs.ktor.client.auth)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.ktor.client.serialization)
    api(libs.ktor.client.logging)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

project.afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "com.fonrouge.fslib"
                artifactId = "android"
                version = publishVersion

                afterEvaluate {
                    from(components["release"])
                }

                artifact(javadocJar)

                pom {
                    name.set("FSLib Android")
                    description.set(
                        "Android client SDK for fsLib — MVVM ViewModels, Compose UI components, " +
                            "type-safe JSON-RPC 2.0 client with automatic route discovery"
                    )
                    url.set("https://github.com/tfonrouge/fslib-android")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("tfonrouge")
                            name.set("Teo Fonrouge")
                            url.set("https://github.com/tfonrouge")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/tfonrouge/fslib-android.git")
                        developerConnection.set("scm:git:ssh://github.com/tfonrouge/fslib-android.git")
                        url.set("https://github.com/tfonrouge/fslib-android")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "Staging"
                url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
            }
        }
    }
}

val isSnapshotPublish = hasProperty("SNAPSHOT")
val isForceLocal = hasProperty("FORCE_LOCAL")

// Prevent publishing release versions to mavenLocal — this would silently shadow
// Maven Central artifacts for every project on the machine that uses mavenLocal().
// Fail at configuration time (not execution time) to stay compatible with the
// configuration cache, which cannot serialize script object references in doFirst lambdas.
if (gradle.startParameter.taskNames.any { it.contains("publishToMavenLocal", ignoreCase = true) }) {
    if (!isSnapshotPublish && !isForceLocal) {
        error(
            "Publishing release version $publishVersion to mavenLocal is blocked to prevent " +
                "shadowing Maven Central artifacts.\n" +
                "  Use: ./gradlew publishToMavenLocal -PSNAPSHOT  (recommended)\n" +
                "  Or:  ./gradlew publishToMavenLocal -PFORCE_LOCAL  (override safety check)"
        )
    }
}

signing {
    useGpgCmd()
    isRequired = findProperty("signing.gnupg.keyName") != null
    sign(publishing.publications)
}

// Disable signing for local publishes. The signing plugin's internal actions access
// Task.project at execution time, which is incompatible with the configuration cache.
// Signing is unnecessary for mavenLocal artifacts.
if (isSnapshotPublish || isForceLocal) {
    tasks.withType<Sign> {
        enabled = false
    }
}
