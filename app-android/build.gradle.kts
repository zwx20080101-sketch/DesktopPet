plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mascot.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mascot.app"
        minSdk = 28
        targetSdk = 35
        // 自动递增版本号：基于时间戳，保证每次构建都大于上次
        // 1700000000 是基准时间戳，当前约 2023-11，减去后得到相对值，避免溢出
        val buildTimestamp = (System.currentTimeMillis() / 1000 - 1700000000).toInt()
        versionCode = buildTimestamp
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    implementation(project(":overlay-android"))
}
