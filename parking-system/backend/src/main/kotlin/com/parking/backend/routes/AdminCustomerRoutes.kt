package com.parking.backend.routes

import com.parking.shared.data.api.dto.AssignMonthlyRequest
import com.parking.shared.data.api.dto.UpsertCustomerRequest
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Rutas admin para clientes (mensualistas):
 *   GET    /api/v1/parkings/{id}/customers
 *   GET    /api/v1/parkings/{id}/customers/expiring-soon?days=N
 *   POST   /api/v1/parkings/{id}/customers
 *   PUT    /api/v1/parkings/{id}/customers/{customerId}
 *   POST   /api/v1/parkings/{id}/customers/{customerId}/monthly  (asignar/renovar)
 */
fun Application.registerAdminCustomerRoutes() {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/parkings/{parkingId}/customers") {
                get {
                    val parkingId = call.matchTenant() ?: return@get
                    call.respond(AdminCustomerRepository.list(parkingId))
                }
                get("/expiring-soon") {
                    val parkingId = call.matchTenant() ?: return@get
                    val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 7
                    call.respond(AdminCustomerRepository.expiringSoon(parkingId, days))
                }
                post {
                    val parkingId = call.matchTenant() ?: return@post
                    val actor = call.actor()
                    val body = call.receive<UpsertCustomerRequest>()
                    val created = AdminCustomerRepository.create(parkingId, body)
                    call.audit(parkingId, actor, "customer.created", "customer", created.id,
                        """{"document":"${body.documentNumber}","plates":${body.vehicles.size}}""")
                    call.respond(HttpStatusCode.Created, created)
                }
                put("/{customerId}") {
                    val parkingId = call.matchTenant() ?: return@put
                    val actor = call.actor()
                    val customerId = call.parameters["customerId"]!!
                    val body = call.receive<UpsertCustomerRequest>()
                    val updated = AdminCustomerRepository.update(parkingId, customerId, body)
                    call.audit(parkingId, actor, "customer.updated", "customer", customerId,
                        """{"document":"${body.documentNumber}"}""")
                    call.respond(updated)
                }
                post("/{customerId}/monthly") {
                    val parkingId = call.matchTenant() ?: return@post
                    val actor = call.actor()
                    val customerId = call.parameters["customerId"]!!
                    val body = call.receive<AssignMonthlyRequest>()
                    // Cargamos el plan para conocer duración y precio.
                    val plan = transaction {
                        TariffPlansT.selectAll()
                            .where { TariffPlansT.id eq UUID.fromString(body.planId) }
                            .singleOrNull()
                    } ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "plan_not_found"))
                        return@post
                    }
                    val duration = plan[TariffPlansT.durationDays]
                    val price = plan[TariffPlansT.priceCents]
                    val created = AdminCustomerRepository.assignMonthly(
                        parkingId, customerId, body, duration, price
                    )
                    call.audit(parkingId, actor, "monthly.assigned", "monthly", created.id,
                        """{"customerId":"$customerId","planId":"${body.planId}","amount":${created.amountCents}}""")
                    call.respond(HttpStatusCode.Created, created)
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
