import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Single source of truth: version.properties (bumped by scripts/release-pipeline.sh)
val versionProps = Properties().apply {
    load(file("../version.properties").inputStream())
}

android {
    namespace = "ai.chemistry_learning_org"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.chemistry_learning_org"
        minSdk = 26
        targetSdk = 36
        versionCode = (versionProps.getProperty("versionCode") ?: "1").toInt()
        versionName = versionProps.getProperty("versionName") ?: "1.0.0"

        buildConfigField("String", "BASE_URL", "\"https://chemie-lernen.org\"")
    }

    signingConfigs {
        // Release keystore read from gradle properties or environment.
        // When absent, no signing config is created and the release build stays
        // UNSIGNED (app-release-unsigned.apk) — required for F-Droid, which signs
        // with its own key. Google Play builds set these via scripts/build-release.sh / CI.
        val storeFile = providers.gradleProperty("CHEMIELERNEN_RELEASE_STORE_FILE")
            .orElse(providers.environmentVariable("CHEMIELERNEN_RELEASE_STORE_FILE"))
            .orNull
        val storePassword = providers.gradleProperty("CHEMIELERNEN_RELEASE_STORE_PASSWORD")
            .orElse(providers.environmentVariable("CHEMIELERNEN_RELEASE_STORE_PASSWORD"))
            .orNull
        val keyAlias = providers.gradleProperty("CHEMIELERNEN_RELEASE_KEY_ALIAS")
            .orElse(providers.environmentVariable("CHEMIELERNEN_RELEASE_KEY_ALIAS"))
            .orNull
        val keyPassword = providers.gradleProperty("CHEMIELERNEN_RELEASE_KEY_PASSWORD")
            .orElse(providers.environmentVariable("CHEMIELERNEN_RELEASE_KEY_PASSWORD"))
            .orNull

        if (
            !storeFile.isNullOrBlank() &&
            !storePassword.isNullOrBlank() &&
            !keyAlias.isNullOrBlank() &&
            !keyPassword.isNullOrBlank()
        ) {
            create("release") {
                this.storeFile = if (file(storeFile).exists()) file(storeFile) else file("../" + storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") // null → unsigned (F-Droid)
            ndk {
                // Native debug symbols (libandroidx.graphics.path.so) for Google Play
                // crash/ANR analysis — upload symbols.zip with the AAB in Play Console.
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.webview)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
