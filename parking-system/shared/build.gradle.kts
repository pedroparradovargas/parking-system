/*
 * shared/build.gradle.kts — módulo Kotlin Multiplatform raíz del dominio.
 *
 * Este módulo NO depende de Compose. Únicamente contiene:
 *   - Dominio (modelos puros, motor de tarifas, interfaces de repositorio).
 *   - Cliente HTTP (Ktor multiplataforma).
 *   - DB local (SQLDelight multiplataforma).
 *   - Sincronización (outbox pattern).
 *
 * Targets:
 *   - androidTarget (apps móviles Android)
 *   - jvm           (backend Y desktop)
 *   - iosX64 / iosArm64 / iosSimulatorArm64
 *   - wasmJs        (PWA browser) — reactivado con SQLDelight 2.0.2 web-worker-driver.
 */

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    // jvmToolchain(17) cubre el target JVM en todos los compiles JVM.
    // Las opciones comunes (como -Xexpect-actual-classes) van a nivel del extension.
    // jvmTarget NO se setea aquí porque KotlinCommonCompilerOptions no lo expone.
    jvmToolchain(17)

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilations.all {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            compileTaskProvider.configure {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
        }
    }

    jvm()

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.collections.immutable)

            implementation(libs.bundles.ktor.client.common)
            implementation(libs.ktor.serialization.json)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.primitive.adapters)

            implementation(libs.koin.core)
            implementation(libs.napier)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.assertions)
            implementation(libs.kotest.property)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.swing)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
                implementation(libs.sqldelight.web.worker.driver)
            }
        }
    }
}

android {
    namespace = "com.parking.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("ParkingDatabase") {
            packageName.set("com.parking.shared.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/schemas"))
            verifyMigrations.set(true)
            // Dialecto 3.38 habilita UPSERT (ON CONFLICT DO UPDATE).
            // Hardcodeado para evitar ambigüedades de naming en el version catalog.
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:${libs.versions.sqldelight.get()}")
        }
    }
}
