package com.parking.backend.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import java.util.Date
import kotlin.time.Duration.Companion.minutes

/**
 * Instala CORS, RateLimit, JWT y StatusPages — toda la seguridad transversal
 * vive en este archivo para minimizar superficie y facilitar auditoría.
 */
fun Application.configureSecurity(cfg: AppConfig) {

    // --- CORS ---
    install(CORS) {
        cfg.cors.allowedHosts.forEach { host ->
            val noScheme = host.removePrefix("http://").removePrefix("https://")
            allowHost(noScheme, listOf("http", "https"))
        }
        HttpMethod.DefaultMethods.forEach { allowMethod(it) }
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        maxAgeInSeconds = 3600
    }

    // --- Rate limiting ---
    install(RateLimit) {
        register {
            rateLimiter(limit = cfg.rateLimit.globalRequestsPerMinute, refillPeriod = 1.minutes)
        }
        register(RateLimitName("auth")) {
            rateLimiter(limit = cfg.rateLimit.authRequestsPerMinute, refillPeriod = 1.minutes)
        }
    }

    // --- JWT ---
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "parking-system"
            verifier(
                JWT.require(Algorithm.HMAC256(cfg.security.jwtSecret))
                    .withAudience(cfg.security.jwtAudience)
                    .withIssuer(cfg.security.jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString().isNotBlank()) JWTPrincipal(credential.payload)
                else null
            }
        }
    }

    // --- Manejo uniforme de errores ---
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "bad request")))
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("error" to (cause.message ?: "not found")))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal_error"))
        }
    }
}

/** Helper para emitir un access token con expiración y claims estándar. */
fun issueAccessToken(secret: String, issuer: String, audience: String, ttlSeconds: Long, userId: String, parkingId: String, roles: List<String>): String =
    JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", userId)
        .withClaim("parkingId", parkingId)
        .withArrayClaim("roles", roles.toTypedArray())
        .withIssuedAt(Date())
        .withExpiresAt(Date(System.currentTimeMillis() + ttlSeconds * 1000))
        .sign(Algorithm.HMAC256(secret))
