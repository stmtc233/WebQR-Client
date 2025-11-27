// build.gradle.kts (Module :app)

import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.example.webqrclient"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.webqrclient"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val defaultUrl = localProperties.getProperty("API_DEFAULT_URL") ?: "\"https://placeholder.com/\""
        val backupUrl = localProperties.getProperty("API_BACKUP_URL") ?: "\"https://placeholder.com/\""

        buildConfigField("String", "API_DEFAULT_URL", defaultUrl)
        buildConfigField("String", "API_BACKUP_URL", backupUrl)
    }

    buildTypes {
        // 在 KTS 中，需要用 getByName 来获取和配置 release 版本
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        // 移除了 compose = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    // 基础依赖
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 生命周期和协程
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // CameraX 依赖
    val cameraxVersion = "1.3.1" // 在 KTS 中这样定义变量
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ML Kit 二维码扫描依赖
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // Retrofit 网络请求库
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // 测试依赖
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
