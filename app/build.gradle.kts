plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.smokeshield"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.smokeshield"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
// Google Health Connect (Ufficiale)
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation("androidx.work:work-runtime-ktx:2.9.0")
// Libreria Vico per la Data Visualization (Grafici)
    implementation("com.patrykandpatrick.vico:core:1.14.0")
    implementation("com.patrykandpatrick.vico:views:1.14.0")
    implementation("com.google.android.gms:play-services-auth:21.6.0")

    val nav_version = "2.9.8"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}