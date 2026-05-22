package com.parking.backend.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.parking.backend.config.AppConfig
import com.parking.backend.config.issueAccessToken
import com.parking.shared.data.api.dto.LoginRequest
import com.parking.shared.data.api.dto.LoginResponse
import com.parking.shared.data.api.dto.UserDto
import dev.samstevens.totp.code.CodeVerifier
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.time.SystemTimeProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

/**
 * Rutas /api/v1/auth/... — login, refresh y validación TOTP.
 *
 * Política:
 *   - Rate limit estricto (5/min) en este grupo.
 *   - bcrypt con cost ≥12.
 *   - Si el usuario tiene `requires_2fa=true`, exige `totpCode` válido.
 */
fun Application.registerAuthRoutes() {
    val cfg by inject<AppConfig>()
    val verifier: CodeVerifier = DefaultCodeVerifier(DefaultCodeGenerator(), SystemTimeProvider())

    routing {
        rateLimit(RateLimitName("auth")) {
            route("/api/v1/auth") {

                post("/login") {
                    val req = call.receive<LoginRequest>()
                    val user = AuthRepository.findByUsername(req.username)
                    if (user == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_credentials"))
                        return@post
                    }
                    if (!BCrypt.verifyer().verify(req.password.toCharArray(), user.passwordHash).verified) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_credentials"))
                        return@post
                    }
                    if (user.requires2fa) {
                        val code = req.totpCode
                        if (code.isNullOrBlank() || user.totpSecret == null || !verifier.isValidCode(user.totpSecret, code)) {
                            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "totp_required"))
                            return@post
                        }
                    }
                    val access = issueAccessToken(
                        secret = cfg.security.jwtSecret,
                        issuer = cfg.security.jwtIssuer,
                        audience = cfg.security.jwtAudience,
                        ttlSeconds = cfg.security.accessTokenTtlSeconds,
                        userId = user.id,
                        parkingId = user.parkingId,
                        roles = user.roles,
                    )
                    val refresh = AuthRepository.issueRefreshToken(user.id, cfg.security.refreshTokenTtlSeconds)
                    AuthRepository.touchLastLogin(user.id)
                    runCatching {
                        AuditRepository.append(
                            parkingId = user.parkingId,
                            action = "login.success",
                            entity = "user",
                            entityId = user.id,
                            actorUserId = user.id,
                            payloadJson = "{\"username\":\"${user.username}\"}",
                        )
                    }

                    call.respond(
                        LoginResponse(
                            accessToken = access,
                            refreshToken = refresh,
                            expiresInSeconds = cfg.security.accessTokenTtlSeconds,
                            user = UserDto(
                                id = user.id,
                                username = user.username,
                                fullName = user.fullName,
                                email = user.email,
                                roles = user.roles,
                                parkingId = user.parkingId,
                                requires2fa = user.requires2fa,
                            ),
                        )
                    )
                }

                post("/refresh") {
                    val body = call.receive<Map<String, String>>()
                    val refresh = body["refreshToken"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing_refresh"))
                    val user = AuthRepository.validateRefreshToken(refresh)
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid_refresh"))
                    val access = issueAccessToken(
                        cfg.security.jwtSecret, cfg.security.jwtIssuer, cfg.security.jwtAudience,
                        cfg.security.accessTokenTtlSeconds, user.id, user.parkingId, user.roles,
                    )
                    val newRefresh = AuthRepository.rotateRefreshToken(refresh, user.id, cfg.security.refreshTokenTtlSeconds)
                    runCatching {
                        AuditRepository.append(
                            parkingId = user.parkingId,
                            action = "refresh.success",
                            entity = "user",
                            entityId = user.id,
                            actorUserId = user.id,
                            payloadJson = "{}",
                        )
                    }
                    call.respond(
                        LoginResponse(
                            accessToken = access,
                            refreshToken = newRefresh,
                            expiresInSeconds = cfg.security.accessTokenTtlSeconds,
                            user = UserDto(
                                id = user.id, username = user.username, fullName = user.fullName,
                                email = user.email, roles = user.roles, parkingId = user.parkingId,
                                requires2fa = user.requires2fa,
                            ),
                        )
                    )
                }
            }
        }
    }
}
