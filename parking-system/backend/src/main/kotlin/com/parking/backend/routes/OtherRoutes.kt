package com.parking.backend.routes

import com.parking.shared.data.api.dto.OccupancyReportDto
import com.parking.shared.data.api.dto.RevenueReportDto
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Rutas adicionales — catálogos (tariffs/zones), reportes y health.
 */
fun Application.registerOtherRoutes() {
    routing {
        // Health + readiness — sin auth para que K8s pueda interrogar.
        get("/healthz") { call.respond(mapOf("status" to "ok")) }
        get("/readyz") { call.respond(mapOf("status" to "ready")) }

        authenticate("auth-jwt") {

            route("/api/v1/parkings/{parkingId}") {

                get("/tariffs") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val parkingId = call.parameters["parkingId"]!!
                    requireSameTenant(principal, parkingId)
                    call.respond(SyncRepository.tariffsSince(parkingId, 0L))
                }

                get("/zones") {
                    val principal = call.principal<JWTPrincipal>()!!
                    val parkingId = call.parameters["parkingId"]!!
                    requireSameTenant(principal, parkingId)
                    call.respond(SyncRepository.zonesSnapshot(parkingId))
                }

                route("/reports") {

                    get("/revenue") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val parkingId = call.parameters["parkingId"]!!
                        requireSameTenant(principal, parkingId)
                        val from = call.request.queryParameters["from"] ?: ""
                        val to = call.request.queryParameters["to"] ?: ""
                        val report = ReportsRepository.revenue(parkingId, from, to)
                        call.respond(report)
                    }

                    get("/occupancy") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val parkingId = call.parameters["parkingId"]!!
                        requireSameTenant(principal, parkingId)
                        val report: OccupancyReportDto = ReportsRepository.occupancy(parkingId)
                        call.respond(report)
                    }
                }
            }
        }
    }
}

private fun requireSameTenant(principal: JWTPrincipal, parkingId: String) {
    val claim = principal.payload.getClaim("parkingId").asString()
    if (claim != parkingId) throw IllegalArgumentException("tenant_mismatch")
}
