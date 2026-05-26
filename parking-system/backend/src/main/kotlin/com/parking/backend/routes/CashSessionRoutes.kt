package com.parking.backend.routes

import com.parking.shared.data.api.dto.CloseCashSessionRequest
import com.parking.shared.data.api.dto.OpenCashSessionRequest
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
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.registerCashSessionRoutes() {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/parkings/{parkingId}/cash-sessions") {

                post("/open") {
                    val parkingId = call.matchCashTenant() ?: return@post
                    val operatorUserId = call.operatorId() ?: return@post
                    val body = call.receive<OpenCashSessionRequest>()
                    try {
                        val session = CashSessionRepository.open(parkingId, operatorUserId, body.notes)
                        call.respond(HttpStatusCode.Created, session)
                    } catch (e: IllegalStateException) {
                        call.respond(HttpStatusCode.Conflict, mapOf("error" to (e.message ?: "conflict")))
                    }
                }

                post("/{cashSessionId}/close") {
                    val parkingId = call.matchCashTenant() ?: return@post
                    val operatorUserId = call.operatorId() ?: return@post
                    val cashSessionId = call.parameters["cashSessionId"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing_cash_session_id"))
                        return@post
                    }
                    val body = call.receive<CloseCashSessionRequest>()
                    try {
                        val session = CashSessionRepository.close(cashSessionId, parkingId, operatorUserId, body.notes)
                        call.respond(session)
                    } catch (e: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "bad_request")))
                    }
                }

                get("/current") {
                    val parkingId = call.matchCashTenant() ?: return@get
                    val operatorUserId = call.operatorId() ?: return@get
                    val session = CashSessionRepository.currentForOperator(parkingId, operatorUserId)
                    if (session != null) {
                        call.respond(session)
                    } else {
                        call.respond(HttpStatusCode.NoContent)
                    }
                }

                get {
                    val parkingId = call.matchCashTenant() ?: return@get
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                    val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
                    call.respond(CashSessionRepository.list(parkingId, limit, offset))
                }
            }
        }
    }
}

private suspend fun ApplicationCall.matchCashTenant(): String? {
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

private suspend fun ApplicationCall.operatorId(): String? {
    val userId = principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
    if (userId == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "missing_user_id"))
        return null
    }
    return userId
}
