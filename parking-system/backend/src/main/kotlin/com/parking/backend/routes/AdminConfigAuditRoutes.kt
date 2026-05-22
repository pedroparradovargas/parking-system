package com.parking.backend.routes

import com.parking.shared.data.api.dto.AuditEntryRowDto
import com.parking.shared.data.api.dto.AuditVerifyResponse
import com.parking.shared.data.api.dto.UpsertParkingConfigRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Endpoints admin:
 *   GET /api/v1/parkings/{id}/config
 *   PUT /api/v1/parkings/{id}/config
 *   GET /api/v1/parkings/{id}/audit?entity=&action=&from=&to=&limit=&offset=
 *   POST /api/v1/parkings/{id}/audit/verify
 */
fun Application.registerAdminConfigAuditRoutes() {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/parkings/{parkingId}") {

                // --- Config ----
                get("/config") {
                    val parkingId = call.matchTenant() ?: return@get
                    call.respond(AdminConfigRepository.get(parkingId))
                }
                put("/config") {
                    val parkingId = call.matchTenant() ?: return@put
                    val actor = call.actor()
                    val body = call.receive<UpsertParkingConfigRequest>()
                    val updated = AdminConfigRepository.upsert(parkingId, body, actor)
                    call.audit(parkingId, actor, "config.updated", "parking_config", parkingId,
                        """{"timezone":"${body.timezone ?: ""}","mode":"${body.operatingMode ?: ""}"}""")
                    call.respond(updated)
                }

                // --- Audit ----
                get("/audit") {
                    val parkingId = call.matchTenant() ?: return@get
                    val entity = call.request.queryParameters["entity"]
                    val action = call.request.queryParameters["action"]
                    val fromMs = call.request.queryParameters["from"]?.toLongOrNull()
                    val toMs = call.request.queryParameters["to"]?.toLongOrNull()
                    val actorId = call.request.queryParameters["actor"]
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 100
                    val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0L
                    val rows = AuditRepository.query(
                        parkingId = parkingId,
                        entity = entity,
                        action = action,
                        fromMillis = fromMs,
                        toMillis = toMs,
                        actorUserId = actorId,
                        limit = limit,
                        offset = offset,
                    ).map {
                        AuditEntryRowDto(
                            id = it.id, parkingId = it.parkingId, tsEpochMillis = it.tsEpochMillis,
                            action = it.action, entity = it.entity, entityId = it.entityId,
                            actorUserId = it.actorUserId, payloadJson = it.payloadJson,
                            prevHash = it.prevHash, currentHash = it.currentHash,
                        )
                    }
                    call.respond(rows)
                }
                post("/audit/verify") {
                    val parkingId = call.matchTenant() ?: return@post
                    val corrupted = AuditRepository.verifyChain(parkingId)
                    val total = AuditRepository.query(parkingId, limit = 500, offset = 0).size
                    call.respond(AuditVerifyResponse(parkingId = parkingId, checked = total, corrupted = corrupted))
                }
            }
        }
    }
}

private suspend fun ApplicationCall.matchTenant(): String? {
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
