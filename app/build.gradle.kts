plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.yourname.employee"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yourname.employee"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Add this for FCM server key
        buildConfigField("String", "FCM_SERVER_KEY", "\"YOUR_SERVER_KEY_HERE\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField("String", "FCM_SERVER_KEY", "\"YOUR_SERVER_KEY_HERE\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
        // 🔴 ADD THIS LINE - Enable BuildConfig
        buildConfig = true
    }
}

dependencies {
    // AndroidX dependencies
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firebase dependencies (versions managed by BOM)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")

    // AndroidX for RecyclerView, CardView, Navigation
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.navigation:navigation-fragment:2.7.5")
    implementation("androidx.navigation:navigation-ui:2.7.5")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}