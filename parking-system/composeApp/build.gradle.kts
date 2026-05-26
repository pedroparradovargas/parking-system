/*
 * composeApp/build.gradle.kts
 *
 * Módulo de aplicación multiplataforma con UI Compose.
 * Targets activos: androidTarget, jvm("desktop"), iOS frameworks, wasmJs (PWA).
 */

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(17)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilations.all {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compileTaskProvider.configure { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
        }
    }

    jvm("desktop")

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // QR multiplataforma (KMP incluye wasmJs desde 1.0.0).
            implementation(libs.qrose)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.collections.immutable)

            // Ktor client core para que AppModule.kt pueda referenciar
            // HttpClient/HttpClientConfig (shared los expone como `implementation`).
            implementation(libs.bundles.ktor.client.common)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.napier)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.kotlinx.coroutines.android)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.logback.classic)
            }
        }

        val wasmJsMain by getting {
            dependencies {
                // Compose UI ya viene heredado de commonMain; aquí solo extras web.
            }
        }
    }
}

android {
    namespace = "com.parking.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.parking.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.parking.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "parking-system"
            packageVersion = "1.0.0"
        }
    }
}
