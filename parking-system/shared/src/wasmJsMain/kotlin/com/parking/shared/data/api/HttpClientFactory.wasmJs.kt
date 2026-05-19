package com.parking.shared.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

/**
 * Engine fetch() para Wasm/JS — totalmente delegado al navegador.
 * No requiere setup adicional.
 */
actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Js) { block() }
