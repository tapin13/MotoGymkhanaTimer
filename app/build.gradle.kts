plugins {
    id("com.android.application")
}

val LOGS_UPLOAD_URL: String = project.findProperty("LOGS_UPLOAD_URL") as? String ?: ""

android {
    namespace = "com.t13.motogymkhanatimer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.t13.motogymkhanatimer"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"

        buildConfigField(
            "String",
            "LOGS_UPLOAD_URL",
            "\"$LOGS_UPLOAD_URL\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}