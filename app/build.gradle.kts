plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.chaquo.python")
}

android {
    namespace = "com.saidev.stitchoptix"
    compileSdk = 36
    ndkVersion = "28.0.13004108"
    defaultConfig {
        applicationId = "com.saidev.stitchoptix"
        minSdk = 32
        targetSdk = 36
        versionCode = 6
        versionName = "6.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}
chaquopy {
    defaultConfig {
        version = "3.11"
        buildPython = listOf("C:/Users/hi/python311/python.exe")
        pip {
            install("pyembroidery")
        }
    }
}
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Added for RecyclerView and CardView
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    // For Glide (if you plan to use it for image loading)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
}