package com.parking.backend.routes

import com.parking.backend.config.OccupancyEvent
import com.parking.backend.config.publishOccupancy
import com.parking.shared.data.api.dto.SessionDto
import com.parking.shared.domain.model.SessionStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Rutas /api/v1/sessions/... — abre y cierra sesiones de parqueo.
 *
 * Multi-tenant: todas las operaciones filtran por `parkingId` del JWT.
 * Los repositorios validan que el `parking_id` del payload coincida con
 * el del token, para evitar accesos cruzados entre clientes.
 */
fun Application.registerSessionRoutes() {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/sessions") {

                post {
                    val principal = call.principal<JWTPrincipal>()!!
                    val callerParkingId = principal.payload.getClaim("parkingId").asString()
                    val body = call.receive<SessionDto>()
                    if (body.parkingId != callerParkingId) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "tenant_mismatch"))
                        return@post
                    }
                    val actorUserId = principal.payload.getClaim("userId").asString()
                    val saved = SessionRepository.openSession(body, actorUserId)
                    // Notifica censo de ocupación.
                    saved.zoneId?.let { zoneId ->
                        val occ = SessionRepository.zoneOccupancy(zoneId)
                        publishOccupancy(OccupancyEvent(saved.parkingId, occ.code, occ.occupancy, occ.capacity))
                    }
                    runCatching {
                        AuditRepository.append(
                            parkingId = saved.parkingId,
                            action = "session.opened",
                            entity = "session",
                            entityId = saved.id,
                            actorUserId = actorUserId,
                            payloadJson = "{\"plate\":\"${saved.plate}\",\"vehicleType\":\"${saved.vehicleType}\",\"zoneId\":\"${saved.zoneId.orEmpty()}\"}",
                        )
                    }
                    call.respond(HttpStatusCode.Created, saved)
                }

                get("/active") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val callerParkingId = principal.payload.getClaim("parkingId").asString()
                    call.respond(SessionRepository.activeSessions(callerParkingId))
                }

                post("/{sessionId}/close") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val sessionId = call.parameters["sessionId"]!!
                    val actorUserId = principal.payload.getClaim("userId").asString()
                    val body = call.receive<SessionDto>()
                    val closed = SessionRepository.closeSession(
                        sessionId = sessionId,
                        parkingId = principal.payload.getClaim("parkingId").asString(),
                        exitAtMillis = body.exitAtMillis ?: System.currentTimeMillis(),
                        totalCents = body.totalCents ?: 0,
                        ivaCents = body.ivaCents ?: 0,
                    )
                    closed.zoneId?.let { zoneId ->
                        val occ = SessionRepository.zoneOccupancy(zoneId)
                        publishOccupancy(OccupancyEvent(closed.parkingId, occ.code, occ.occupancy, occ.capacity))
                    }
                    runCatching {
                        AuditRepository.append(
                            parkingId = closed.parkingId,
                            action = "session.closed",
                            entity = "session",
                            entityId = sessionId,
                            actorUserId = actorUserId,
                            payloadJson = "{\"totalCents\":${closed.totalCents ?: 0},\"ivaCents\":${closed.ivaCents ?: 0}}",
                        )
                    }
                    call.respond(closed.copy(status = SessionStatus.CLOSED))
                }
            }
        }
    }
}
