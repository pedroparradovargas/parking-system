/*
 * build.gradle.kts (raíz) — declara todos los plugins en modo `apply false`.
 *
 * Cada submódulo aplica los plugins que necesita.  Mantener la versión
 * centralizada aquí garantiza que Compose, Kotlin y Android estén
 * alineados en TODO el monorepo (es una de las causas más comunes
 * de errores en proyectos KMP grandes).
 */

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktor) apply false
}

// Tarea de conveniencia: limpia todos los módulos a la vez.
tasks.register("cleanAll") {
    group = "build"
    description = "Limpia todos los módulos del monorepo."
    dependsOn(subprojects.map { it.tasks.named("clean") })
}
