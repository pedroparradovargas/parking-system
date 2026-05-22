package com.parking.backend.routes

import com.parking.shared.data.api.dto.UpsertZoneRequest
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
 * Rutas administrativas para zonas:
 *   GET    /api/v1/parkings/{parkingId}/zones
 *   POST   /api/v1/parkings/{parkingId}/zones
 *   PUT    /api/v1/parkings/{parkingId}/zones/{zoneId}
 *   DELETE /api/v1/parkings/{parkingId}/zones/{zoneId}
 *
 * El GET existía antes en `OtherRoutes` con `SyncRepository.zonesSnapshot`,
 * que no proyectaba los campos V2 (under_maintenance, enabled, ...).  Ahora
 * el GET vive aquí y devuelve el [ZoneDto] completo.
 */
fun Application.registerAdminZoneRoutes() {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/parkings/{parkingId}/zones") {

                get {
                    val parkingId = call.matchTenant() ?: return@get
                    call.respond(AdminZoneRepository.list(parkingId))
                }

                post {
                    val parkingId = call.matchTenant() ?: return@post
                    val actor = call.actor()
                    val body = call.receive<UpsertZoneRequest>()
                    val created = AdminZoneRepository.create(parkingId, body)
                    call.audit(parkingId, actor, "zone.created", "zone", created.id,
                        """{"code":"${body.code}","capacity":${body.capacity}}""")
                    call.respond(HttpStatusCode.Created, created)
                }

                put("/{zoneId}") {
                    val parkingId = call.matchTenant() ?: return@put
                    val actor = call.actor()
                    val zoneId = call.parameters["zoneId"]!!
                    val body = call.receive<UpsertZoneRequest>()
                    val updated = AdminZoneRepository.update(parkingId, zoneId, body)
                    call.audit(parkingId, actor, "zone.updated", "zone", zoneId,
                        """{"code":"${body.code}","capacity":${body.capacity},"enabled":${body.enabled}}""")
                    call.respond(updated)
                }

                delete("/{zoneId}") {
                    val parkingId = call.matchTenant() ?: return@delete
                    val actor = call.actor()
                    val zoneId = call.parameters["zoneId"]!!
                    val attempt = runCatching { AdminZoneRepository.delete(parkingId, zoneId) }
                    val n = attempt.getOrElse {
                        val err = it.message ?: "zone_delete_failed"
                        call.respond(HttpStatusCode.Conflict, mapOf("error" to err))
                        return@delete
                    }
                    if (n == 0) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "zone_not_found"))
                    } else {
                        call.audit(parkingId, actor, "zone.deleted", "zone", zoneId, "{}")
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}

// Helpers de tenant + audit — copia de los de AdminTariffRoutes para no
// acoplar archivos.  Si crecen, se extraen a un common file.
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

private fun ApplicationCall.audit(
    parkingId: String,
    actorUserId: String?,
    action: String,
    entity: String,
    entityId: String,
    payloadJson: String,
) {
    runCatching {
        AuditRepository.append(parkingId, action, entity, entityId, actorUserId, payloadJson)
    }
}
