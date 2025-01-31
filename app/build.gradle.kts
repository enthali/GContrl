import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Function to determine Git Branch
fun getGitBranch(): String {
    val process = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()

    return process.inputStream.bufferedReader().use { it.readText() }.trim()
        .ifEmpty { "unknown" }
}

android {
    namespace = "de.drachenfels.gcontrl"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.drachenfels.gcontrl"
        minSdk = 26
        targetSdk = 35
        versionCode = 31
        versionName = "connect me"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add Build Date and Time
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        buildConfigField("String", "BUILD_DATE", "\"${formattedDate}\"")

        // Add Git Branch Information
        buildConfigField("String", "GIT_BRANCH", "\"${getGitBranch()}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("C:\\Users\\georg\\OneDrive\\Computer\\Android\\AppKeys\\gcontrol\\keystore.jks")
            storePassword = "gcontrol"
            keyAlias = "upload"
            keyPassword = "gcontrol"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("Boolean", "ENABLE_DEBUG", "false")
        }
        debug {
            buildConfigField("Boolean", "ENABLE_DEBUG", "true")
        }
    }
    
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.hivemq.mqtt.client)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}