plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "carmel.shubeli.euchre"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "carmel.shubeli.euchre"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    //noinspection UseTomlInstead,GradleDependency
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))

    implementation(libs.firebase.auth)
    //noinspection UseTomlInstead
    implementation("com.google.firebase:firebase-database")

    //noinspection UseTomlInstead,GradleDependency
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}