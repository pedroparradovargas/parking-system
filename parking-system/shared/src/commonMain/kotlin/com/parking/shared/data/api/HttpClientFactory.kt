package com.parking.shared.data.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/*
 * Factory de HttpClient con configuración compartida.
 *
 * `expect`/`actual` permite elegir engine por plataforma:
 *   - Android  → OkHttp
 *   - iOS      → Darwin
 *   - JVM      → Java
 *   - Wasm/JS  → JS
 *
 * Los `actual` viven en androidMain/iosMain/jvmMain/wasmJsMain y se limitan
 * a llamar `createPlatformHttpClient { applyCommonConfig() }` para que TODA
 * la configuración no específica del engine viva aquí.
 */
expect fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient

/** Serializador JSON único — ignora propiedades desconocidas para resiliencia ante cambios del API. */
val SharedJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    prettyPrint = false
}

/** Configuración compartida que aplican TODAS las implementaciones por plataforma. */
fun HttpClientConfig<*>.applyCommonConfig(
    baseUrl: String,
    tokenProvider: () -> String? = { null },
) {
    install(ContentNegotiation) {
        json(SharedJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 60_000
    }
    install(Logging) {
        level = LogLevel.INFO
        logger = object : Logger {
            override fun log(message: String) {
                io.github.aakira.napier.Napier.d(message, tag = "Http")
            }
        }
    }
    install(Auth) {
        bearer {
            loadTokens {
                val tok = tokenProvider()
                if (tok != null) BearerTokens(tok, tok) else null
            }
        }
    }
    defaultRequest {
        url(baseUrl)
        contentType(ContentType.Application.Json)
    }
}
