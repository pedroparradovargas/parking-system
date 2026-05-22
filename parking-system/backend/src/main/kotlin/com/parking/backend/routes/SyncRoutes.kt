package com.parking.backend.routes

import com.parking.shared.data.api.dto.SyncAccepted
import com.parking.shared.data.api.dto.SyncPullResponse
import com.parking.shared.data.api.dto.SyncPushRequest
import com.parking.shared.data.api.dto.SyncPushResponse
import com.parking.shared.data.api.dto.SyncRejected
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
 * Rutas /api/v1/sync/... — recepción del outbox del cliente y entrega de cambios.
 *
 * Estrategia:
 *  - push: acepta lotes, valida idempotency_key (localId UUID).
 *  - pull: devuelve cambios desde `since` (millis del último pull exitoso).
 */
fun Application.registerSyncRoutes() {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/sync") {

                post("/push") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val callerParkingId = principal.payload.getClaim("parkingId").asString()
                    val body = call.receive<SyncPushRequest>()
                    if (body.parkingId != callerParkingId) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "tenant_mismatch"))
                        return@post
                    }
                    val accepted = mutableListOf<SyncAccepted>()
                    val rejected = mutableListOf<SyncRejected>()
                    body.receipts.forEach { dto ->
                        try {
                            val serverId = SyncRepository.upsertReceipt(dto, body.deviceId)
                            accepted += SyncAccepted(localId = dto.localId, serverId = serverId)
                        } catch (e: Throwable) {
                            rejected += SyncRejected(localId = dto.localId, errorCode = "save_failed", message = e.message ?: "unknown")
                        }
                    }
                    SyncRepository.logSync(
                        parkingId = body.parkingId,
                        deviceId = body.deviceId,
                        payloadSize = body.receipts.size,
                        accepted = accepted.size,
                        conflicts = 0,
                        rejected = rejected.size,
                    )
                    call.respond(
                        SyncPushResponse(
                            accepted = accepted,
                            conflicts = emptyList(),
                            rejected = rejected,
                        )
                    )
                }

                get("/pull") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val callerParkingId = principal.payload.getClaim("parkingId").asString()
                    val parkingId = call.request.queryParameters["parkingId"] ?: callerParkingId
                    if (parkingId != callerParkingId) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "tenant_mismatch"))
                        return@get
                    }
                    val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                    val tariffs = SyncRepository.tariffsSince(parkingId, since)
                    val zones = SyncRepository.zonesSnapshot(parkingId)
                    val plans = AdminTariffPlanRepository.listSince(parkingId, since)
                    val specials = AdminSpecialTariffRepository.list(parkingId)  // snapshot completo
                    val holidays = AdminHolidayRepository.list(parkingId)        // snapshot completo
                    call.respond(
                        SyncPullResponse(
                            sinceMillis = since,
                            nowMillis = System.currentTimeMillis(),
                            tariffs = tariffs,
                            zones = zones,
                            customersChanged = 0,
                            tariffPlans = plans,
                            specialTariffs = specials,
                            holidays = holidays,
                        )
                    )
                }
            }
        }
    }
}
