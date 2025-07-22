plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.example.androidlib"
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
    kotlinOptions {
        jvmTarget = "21"
    }
//    packaging {
//        resources.excludes.add("META-INF/**")
//    }
    publishing {
        singleVariant("release") {
        }
    }
}

dependencies {
    compileOnly(libs.fonrouge.fslib)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // solves: Could not resolve com.google.guava:listenablefuture:1.0
    implementation(libs.google.guava)
}

project.afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                groupId = "com.fonrouge"
                artifactId = "androidLib"
                version = "1.0.0"

                afterEvaluate {
                    from(components["release"])
                }
            }
        }
    }
    /*
        repositories {
            maven {
                name = "myrepo"
                url = uri(layout.buildDirectory.dir("repo"))
            }
        }
    */
}
