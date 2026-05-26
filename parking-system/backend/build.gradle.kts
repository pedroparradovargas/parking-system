/*
 * backend/build.gradle.kts — servidor Ktor.
 *
 * Empaqueta un fat-jar runnable y aplica el plugin de Ktor para generar
 * tarea `:backend:run` (dev) y `:backend:installDist` (producción).
 */

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    application
}

application {
    mainClass.set("com.parking.backend.ApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))

    // Ktor server
    implementation(libs.bundles.ktor.server)
    implementation(libs.logback.classic)

    // Persistencia
    implementation(libs.postgresql.jdbc)
    implementation(libs.hikari.cp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.bundles.exposed)

    // Excel export
    implementation(libs.apache.poi)
    implementation(libs.apache.poi.ooxml)

    // Auth / crypto
    implementation(libs.bcrypt)
    implementation(libs.java.jwt)
    implementation(libs.totp)

    // DI
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Tests
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions)
    testImplementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
    // Las suites de tests usan H2 in-memory compartido entre JVMs.  Ejecutar
    // en serie evita que dos clases pisen el mismo schema.
    maxParallelForks = 1
}
