package com.parking.backend.routes

import com.parking.shared.data.api.dto.CreateUserRequest
import com.parking.shared.data.api.dto.EnableTotpResponse
import com.parking.shared.data.api.dto.ResetPasswordResponse
import com.parking.shared.data.api.dto.UpdateUserRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Rutas admin para usuarios y 2FA:
 *   GET    /api/v1/parkings/{id}/users
 *   POST   /api/v1/parkings/{id}/users
 *   PUT    /api/v1/parkings/{id}/users/{userId}
 *   DELETE /api/v1/parkings/{id}/users/{userId}             (soft-disable)
 *   POST   /api/v1/parkings/{id}/users/{userId}/reset-password
 *   POST   /api/v1/parkings/{id}/users/{userId}/2fa/enable
 *   POST   /api/v1/parkings/{id}/users/{userId}/2fa/disable
 */
fun Application.registerAdminUserRoutes() {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/parkings/{parkingId}/users") {
                get {
                    val parkingId = call.requireSameTenant() ?: return@get
                    call.respond(AdminUserRepository.list(parkingId))
                }
                post {
                    val parkingId = call.requireSameTenant() ?: return@post
                    val actor = call.actor()
                    val body = call.receive<CreateUserRequest>()
                    val created = AdminUserRepository.create(parkingId, body)
                    call.audit(parkingId, actor, "user.created", "user", created.id,
                        """{"username":"${body.username}","roles":${body.roles.toJsonArray()}}""")
                    call.respond(HttpStatusCode.Created, created)
                }
                put("/{userId}") {
                    val parkingId = call.requireSameTenant() ?: return@put
                    val actor = call.actor()
                    val userId = call.parameters["userId"]!!
                    val body = call.receive<UpdateUserRequest>()
                    val updated = AdminUserRepository.update(parkingId, userId, body)
                    call.audit(parkingId, actor, "user.updated", "user", userId,
                        """{"email":"${body.email}","enabled":${body.enabled}}""")
                    call.respond(updated)
                }
                delete("/{userId}") {
                    val parkingId = call.requireSameTenant() ?: return@delete
                    val actor = call.actor()
                    val userId = call.parameters["userId"]!!
                    val n = AdminUserRepository.disable(parkingId, userId)
                    if (n == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "user_not_found"))
                    else {
                        call.audit(parkingId, actor, "user.disabled", "user", userId, "{}")
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
                post("/{userId}/reset-password") {
                    val parkingId = call.requireSameTenant() ?: return@post
                    val actor = call.actor()
                    val userId = call.parameters["userId"]!!
                    val temp = AdminUserRepository.resetPassword(parkingId, userId)
                    call.audit(parkingId, actor, "user.password_reset", "user", userId, "{}")
                    call.respond(ResetPasswordResponse(temporaryPassword = temp))
                }
                post("/{userId}/2fa/enable") {
                    val parkingId = call.requireSameTenant() ?: return@post
                    val actor = call.actor()
                    val userId = call.parameters["userId"]!!
                    val (secret, uri) = AdminUserRepository.enable2fa(parkingId, userId)
                    call.audit(parkingId, actor, "user.2fa_enabled", "user", userId, "{}")
                    call.respond(EnableTotpResponse(secret = secret, otpAuthUri = uri))
                }
                post("/{userId}/2fa/disable") {
                    val parkingId = call.requireSameTenant() ?: return@post
                    val actor = call.actor()
                    val userId = call.parameters["userId"]!!
                    AdminUserRepository.disable2fa(parkingId, userId)
                    call.audit(parkingId, actor, "user.2fa_disabled", "user", userId, "{}")
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

// Helpers comunes — duplicados pero compactos.
private suspend fun ApplicationCall.requireSameTenant(): String? {
    val pathParking = parameters["parkingId"] ?: run {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "missing_parking_id"))
        return null
    }
    val claim = principal<JWTPrincipal>()?.payload?.getClaim("parkingId")?.asString()
    if (claim != pathParking) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "tenant_mismatch"))
        return null
    }
    return pathParking
}

private fun ApplicationCall.actor(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

private fun ApplicationCall.audit(parkingId: String, actor: String?, action: String, entity: String, entityId: String, payload: String) {
    runCatching { AuditRepository.append(parkingId, action, entity, entityId, actor, payload) }
}

internal fun List<String>.toJsonArray(): String =
    joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
