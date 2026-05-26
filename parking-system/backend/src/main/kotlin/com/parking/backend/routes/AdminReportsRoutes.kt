package com.parking.backend.routes

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
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
                get("/cash-closing.xlsx") {
                    val parkingId = call.matchTenant() ?: return@get
                    val from = call.request.queryParameters["from"] ?: ""
                    val to = call.request.queryParameters["to"] ?: ""
                    val report = AdminReportsRepository.cashClosing(parkingId, from, to)
                    val bytes = AdminReportsRepository.cashClosingXlsx(report)
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName, "cierre-caja-${report.fromIso}-${report.toIso}.xlsx"
                        ).toString()
                    )
                    call.respondBytes(bytes, ContentType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                }
                get("/top-plates") {
                    val parkingId = call.matchTenant() ?: return@get
                    val from = call.request.queryParameters["from"] ?: ""
                    val to = call.request.queryParameters["to"] ?: ""
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                    call.respond(AdminReportsRepository.topPlates(parkingId, from, to, limit))
                }
                get("/top-plates.xlsx") {
                    val parkingId = call.matchTenant() ?: return@get
                    val from = call.request.queryParameters["from"] ?: ""
                    val to = call.request.queryParameters["to"] ?: ""
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                    val report = AdminReportsRepository.topPlates(parkingId, from, to, limit)
                    val bytes = AdminReportsRepository.topPlatesXlsx(report)
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName, "top-placas-${report.fromIso}-${report.toIso}.xlsx"
                        ).toString()
                    )
                    call.respondBytes(bytes, ContentType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
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
