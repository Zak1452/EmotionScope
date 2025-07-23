plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
    id("com.google.gms.google-services")}
android {
    namespace = "com.example.emotionscope"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.emotionscope"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // On Apple silicon, you can omit x86_64.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    chaquopy {
        defaultConfig {
            //buildPython("C:/Users/lamin/AppData/Local/Programs/Python/Python311/python.exe")
            buildPython("C:/Users/lamin/AppData/Local/Programs/Python/Python38/python.exe")
            pip {
                install("librosa==0.9.2")
                install("tensorflow==2.1.0")
                install("resampy==0.2.2")
                install("numba==0.48.0")
                install("scipy==1.4.1") // nÃ©cessaire pour resampy
                install("numpy")
            }
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packagingOptions {
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/DEPENDENCIES")
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.mlkit:face-detection:16.0.0")
    implementation("com.google.mlkit:common:16.0.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.3.1")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.12.0")
    implementation("be.tarsos.dsp:core:2.5")
    implementation("be.tarsos.dsp:jvm:2.5")
    implementation("androidx.camera:camera-core:1.0.2")
    implementation("androidx.camera:camera-camera2:1.0.2")
    implementation("androidx.camera:camera-lifecycle:1.0.2")
    implementation("androidx.camera:camera-view:1.0.0-alpha28")
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.cloud:google-cloud-language:1.101.5")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation ("com.github.PhilJay:MPAndroidChart:3.1.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
    implementation("org.mindrot:jbcrypt:0.4")
}