plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    jvm("desktop")
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            api(libs.datastore.preferences)
            implementation(libs.commons.compress)

            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)

            implementation(libs.coil.compose)
            implementation(libs.markdown.renderer)
            implementation(libs.markdown.renderer.m3)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.mediapipe.tasks.genai)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.pdfbox.android)
            configurations.all {
                exclude(group = "com.google.ai.edge.litert", module = "litert")
                exclude(group = "com.google.ai.edge.litert", module = "litert-gpu")
                exclude(group = "com.google.ai.edge.litert", module = "litert-api")
                exclude(group = "org.tensorflow", module = "tensorflow-lite-support-api")
            }
            implementation(libs.androidx.activity.compose)
            implementation(libs.accompanist.permissions)
            implementation(libs.mlkit.translate)
           
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
}

android {
    namespace = "com.creativeblisslabs.omnimind.core"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}