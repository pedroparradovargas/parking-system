package com.parking.shared.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

/**
 * Engine OkHttp en Android — incluye HTTP/2 y reutilización de conexiones.
 */
actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) {
        engine {
            config {
                retryOnConnectionFailure(true)
                connectTimeout(10, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
            }
        }
        block()
    }
