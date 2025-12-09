plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.example.simpleeditingpictureapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.simpleeditingpictureapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // 修复 Glide 依赖配置
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.androidx.navigationevent)

    // 使用 kapt 进行注解处理
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // 添加 GIF 支持
    implementation("com.github.bumptech.glide:gifdecoder:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")

    // 其他有用的库
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
//    implementation("androidx.lifecycle:lifecycle-transformations-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // 移除可能导致问题的依赖
    // implementation(libs.androidx.compiler)  // 这行可能有问题
    implementation(libs.volley)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}