import com.google.protobuf.gradle.*

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin") version "2.9.7"
    id("com.google.protobuf") version "0.9.4"
}

android {
    namespace = "com.example.easypaisasdk"
    compileSdk = 36

    defaultConfig {
        //applicationId = "com.example.easypaisasdk"
        minSdk = 26
        targetSdk = 36
        /*versionCode = 1
        versionName = "1.0"*/

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
    buildFeatures {
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        viewBinding = true
    }

    sourceSets {
        named("main") {
            java.srcDirs(
                "build/generated/source/proto/main/java",
                "build/generated/source/proto/main/grpckt"
            )
        }
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

   // implementation("com.google.android.material:material:1.9.0")
   // implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:3.0.0")

    implementation("org.kodein.di:kodein-di:7.22.0")
    implementation("org.kodein.di:kodein-di-framework-android-x:7.22.0")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.2.0"))
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation("io.grpc:grpc-stub:1.52.1")
    implementation("io.grpc:grpc-protobuf:1.52.1")
    implementation("io.grpc:grpc-okhttp:1.52.1")
    implementation("io.grpc:protoc-gen-grpc-kotlin:1.3.0")
    implementation("io.grpc:grpc-kotlin-stub:1.3.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.21.12")

    implementation("com.facebook.shimmer:shimmer:0.5.0")

}

// Source - https://stackoverflow.com/a/75279798
// Posted by Konrad Sikorski
// Retrieved 2026-03-05, License - CC BY-SA 4.0

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.23.4"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.57.1"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.3.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("java") //needed either it throws Unresolved Reference
                create("kotlin")
            }
        }
    }

}