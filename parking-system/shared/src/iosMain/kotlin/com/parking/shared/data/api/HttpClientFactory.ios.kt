package com.parking.shared.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

/**
 * Engine Darwin (NSURLSession nativo) — recomendado para iOS.
 * Soporta HTTP/2, cookies y certificados del trust store del sistema.
 */
actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin) {
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
        block()
    }
