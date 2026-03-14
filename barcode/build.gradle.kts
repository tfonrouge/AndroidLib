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
    namespace = "com.fonrouge.fslib.android.barcode"
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

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.ui.tooling.preview)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.play.services.code.scanner)

    api(libs.barcode.scanning)

    implementation(libs.accompanist.permissions)

    implementation(libs.google.guava)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

project.afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "com.fonrouge.fslib"
                artifactId = "barcode"
                version = libs.versions.fslibAndroid.get()

                afterEvaluate {
                    from(components["release"])
                }

                artifact(javadocJar)

                pom {
                    name.set("FSLib Android Barcode")
                    description.set(
                        "Optional barcode scanning module for fsLib Android — " +
                            "CameraX, ML Kit, and Google Play Services code scanner"
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

signing {
    useGpgCmd()
    isRequired = findProperty("signing.gnupg.keyName") != null
    sign(publishing.publications)
}
