package com.parking.backend.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Endpoints admin de reportes adicionales:
 *   GET /api/v1/parkings/{id}/reports/cash-closing?from=&to=
 *   GET /api/v1/parkings/{id}/reports/cash-closing.csv?from=&to=
 *   GET /api/v1/parkings/{id}/reports/top-plates?from=&to=&limit=
 *   GET /api/v1/parkings/{id}/reports/monthly-customers
 *
 * Los reportes de revenue/occupancy ya viven en `OtherRoutes`.
 */
fun Application.registerAdminReportsRoutes() {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/parkings/{parkingId}/reports") {
                get("/cash-closing") {
                    val parkingId = call.matchTenant() ?: return@get
                    val from = call.request.queryParameters["from"] ?: ""
                    val to = call.request.queryParameters["to"] ?: ""
                    call.respond(AdminReportsRepository.cashClosing(parkingId, from, to))
                }
                get("/cash-closing.csv") {
                    val parkingId = call.matchTenant() ?: return@get
                    val from = call.request.queryParameters["from"] ?: ""
                    val to = call.request.queryParameters["to"] ?: ""
                    val report = AdminReportsRepository.cashClosing(parkingId, from, to)
                    call.respondText(AdminReportsRepository.cashClosingCsv(report), ContentType.parse("text/csv"))
                }
                get("/top-plates") {
                    val parkingId = call.matchTenant() ?: return@get
                    val from = call.request.queryParameters["from"] ?: ""
                    val to = call.request.queryParameters["to"] ?: ""
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                    call.respond(AdminReportsRepository.topPlates(parkingId, from, to, limit))
                }
                get("/monthly-customers") {
                    val parkingId = call.matchTenant() ?: return@get
                    call.respond(AdminReportsRepository.monthlyCustomersReport(parkingId))
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
