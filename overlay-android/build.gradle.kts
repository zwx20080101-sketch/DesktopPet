plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mascot.overlay"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ai-client"))
    implementation(project(":memory"))
    implementation(project(":animation-api"))
    implementation(project(":animation-webp"))
    implementation("androidx.core:core-ktx:1.13.1")
}
