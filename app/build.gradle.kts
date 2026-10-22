plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.prooflock"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.prooflock"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.2.0"
        val apiUrl = providers.environmentVariable("PROOFLOCK_API_URL").orElse("").get()
        require(apiUrl.isEmpty() || apiUrl.matches(Regex("https://[a-zA-Z0-9._/-]+"))) {
            "PROOFLOCK_API_URL must be an HTTPS backend URL"
        }
        buildConfigField("String", "PROOFLOCK_API_URL", "\"$apiUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = JavaVersion.VERSION_17.toString() }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
