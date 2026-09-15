plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// کلیدهای ادیوری از gradle.properties خوانده می‌شوند تا داخل کد hardcode نشوند
val adiveryAppId: String = (project.findProperty("ADIVERY_APP_ID") ?: "") as String
val adiveryAppOpenPlacement: String = (project.findProperty("ADIVERY_PLACEMENT_APPOPEN") ?: "") as String
val adiveryBannerPlacement: String = (project.findProperty("ADIVERY_PLACEMENT_BANNER") ?: "") as String

android {
    namespace = "ir.amir.applimiter"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.amir.applimiter"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"

        buildConfigField("String", "ADIVERY_APP_ID", "\"$adiveryAppId\"")
        buildConfigField("String", "ADIVERY_PLACEMENT_APPOPEN", "\"$adiveryAppOpenPlacement\"")
        buildConfigField("String", "ADIVERY_PLACEMENT_BANNER", "\"$adiveryBannerPlacement\"")
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!System.getenv("RELEASE_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        compose = true
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ---- ادیوری ----
    implementation("com.adivery:sdk:4.9.0")
    // شناسه‌ی تبلیغاتی گوگل (GAID) برای هدف‌گذاری بهتر؛ نسخه عمداً پین شده، جدیدتر نگیر
    // (18.1.0+ باعث کرش NoClassDefFoundError روی اندروید ۷ می‌شود و 18.3.0 حداقل minSdk 23 می‌خواهد)
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
