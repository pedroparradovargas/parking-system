package com.parking.shared.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.java.Java

/**
 * Engine java.net.http (HttpClient JDK 11+) — sin dependencias nativas.
 * Suficiente para la caja de escritorio.
 */
actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Java) {
        engine {
            // HTTP/2 habilitado por defecto en java.net.http.
        }
        block()
    }
