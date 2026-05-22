package com.parking.backend.routes

import com.parking.shared.data.api.dto.UpsertHolidayRequest
import com.parking.shared.data.api.dto.UpsertSpecialTariffRequest
import com.parking.shared.data.api.dto.UpsertTariffPlanRequest
import com.parking.shared.data.api.dto.UpsertTariffRequest
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
 * Rutas administrativas para gestión de tarifas:
 *   /api/v1/parkings/{parkingId}/tariffs           (CRUD versionado)
 *   /api/v1/parkings/{parkingId}/tariff-plans      (CRUD planes mensualidad)
 *   /api/v1/parkings/{parkingId}/special-tariffs   (CRUD multiplicadores)
 *   /api/v1/parkings/{parkingId}/holidays          (CR + D festivos)
 *
 * Todas protegidas por JWT; el `parkingId` del path se valida contra el claim
 * del token vía [requireMatchingParking].  Cada mutación crítica produce una
 * entrada en `audit_log` con hash chain.
 */
fun Application.registerAdminTariffRoutes() {
    routing {
        authenticate("auth-jwt") {
            route("/api/v1/parkings/{parkingId}") {

                tariffs()
                tariffPlans()
                specialTariffs()
                holidays()
            }
        }
    }
}

// ───── Tarifas (con versionado) ────────────────────────────────────────────
private fun io.ktor.server.routing.Route.tariffs() {
    route("/tariffs") {
        get {
            val parkingId = call.requireMatchingParking() ?: return@get
            val historic = call.request.queryParameters["historic"]?.toBoolean() ?: false
            call.respond(AdminTariffRepository.list(parkingId, historic))
        }
        post {
            val parkingId = call.requireMatchingParking() ?: return@post
            val actor = call.actorUserId()
            val body = call.receive<UpsertTariffRequest>()
            val created = AdminTariffRepository.create(parkingId, body)
            call.auditOk(parkingId, actor, "tariff.created", "tariff", created.id,
                """{"vehicleType":"${body.vehicleType}","firstHourCents":${body.firstHourCents}}""")
            call.respond(HttpStatusCode.Created, created)
        }
        put("/{tariffId}") {
            val parkingId = call.requireMatchingParking() ?: return@put
            val actor = call.actorUserId()
            val tariffId = call.parameters["tariffId"]!!
            val body = call.receive<UpsertTariffRequest>()
            val updated = AdminTariffRepository.update(parkingId, tariffId, body)
            call.auditOk(parkingId, actor, "tariff.updated", "tariff", updated.id,
                """{"replacesTariffId":"$tariffId","vehicleType":"${body.vehicleType}"}""")
            call.respond(updated)
        }
        delete("/{tariffId}") {
            val parkingId = call.requireMatchingParking() ?: return@delete
            val actor = call.actorUserId()
            val tariffId = call.parameters["tariffId"]!!
            val n = AdminTariffRepository.close(parkingId, tariffId)
            if (n == 0) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "tariff_not_found"))
            } else {
                call.auditOk(parkingId, actor, "tariff.closed", "tariff", tariffId, "{}")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// ───── Planes de mensualidad ───────────────────────────────────────────────
private fun io.ktor.server.routing.Route.tariffPlans() {
    route("/tariff-plans") {
        get {
            val parkingId = call.requireMatchingParking() ?: return@get
            call.respond(AdminTariffPlanRepository.list(parkingId))
        }
        post {
            val parkingId = call.requireMatchingParking() ?: return@post
            val actor = call.actorUserId()
            val body = call.receive<UpsertTariffPlanRequest>()
            val created = AdminTariffPlanRepository.create(parkingId, body)
            call.auditOk(parkingId, actor, "tariff_plan.created", "tariff_plan", created.id,
                """{"name":"${body.name}","durationDays":${body.durationDays}}""")
            call.respond(HttpStatusCode.Created, created)
        }
        put("/{planId}") {
            val parkingId = call.requireMatchingParking() ?: return@put
            val actor = call.actorUserId()
            val planId = call.parameters["planId"]!!
            val body = call.receive<UpsertTariffPlanRequest>()
            val updated = AdminTariffPlanRepository.update(parkingId, planId, body)
            call.auditOk(parkingId, actor, "tariff_plan.updated", "tariff_plan", planId,
                """{"name":"${body.name}"}""")
            call.respond(updated)
        }
        delete("/{planId}") {
            val parkingId = call.requireMatchingParking() ?: return@delete
            val actor = call.actorUserId()
            val planId = call.parameters["planId"]!!
            val n = AdminTariffPlanRepository.delete(parkingId, planId)
            if (n == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "tariff_plan_not_found"))
            else {
                call.auditOk(parkingId, actor, "tariff_plan.deleted", "tariff_plan", planId, "{}")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// ───── Tarifas especiales ──────────────────────────────────────────────────
private fun io.ktor.server.routing.Route.specialTariffs() {
    route("/special-tariffs") {
        get {
            val parkingId = call.requireMatchingParking() ?: return@get
            call.respond(AdminSpecialTariffRepository.list(parkingId))
        }
        post {
            val parkingId = call.requireMatchingParking() ?: return@post
            val actor = call.actorUserId()
            val body = call.receive<UpsertSpecialTariffRequest>()
            val created = AdminSpecialTariffRepository.create(parkingId, body)
            call.auditOk(parkingId, actor, "special_tariff.created", "special_tariff", created.id,
                """{"name":"${body.name}","ruleType":"${body.ruleType}","multiplier":${body.multiplier}}""")
            call.respond(HttpStatusCode.Created, created)
        }
        put("/{specialId}") {
            val parkingId = call.requireMatchingParking() ?: return@put
            val actor = call.actorUserId()
            val specialId = call.parameters["specialId"]!!
            val body = call.receive<UpsertSpecialTariffRequest>()
            val updated = AdminSpecialTariffRepository.update(parkingId, specialId, body)
            call.auditOk(parkingId, actor, "special_tariff.updated", "special_tariff", specialId,
                """{"name":"${body.name}"}""")
            call.respond(updated)
        }
        delete("/{specialId}") {
            val parkingId = call.requireMatchingParking() ?: return@delete
            val actor = call.actorUserId()
            val specialId = call.parameters["specialId"]!!
            val n = AdminSpecialTariffRepository.delete(parkingId, specialId)
            if (n == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "special_tariff_not_found"))
            else {
                call.auditOk(parkingId, actor, "special_tariff.deleted", "special_tariff", specialId, "{}")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// ───── Festivos ────────────────────────────────────────────────────────────
private fun io.ktor.server.routing.Route.holidays() {
    route("/holidays") {
        get {
            val parkingId = call.requireMatchingParking() ?: return@get
            call.respond(AdminHolidayRepository.list(parkingId))
        }
        post {
            val parkingId = call.requireMatchingParking() ?: return@post
            val actor = call.actorUserId()
            val body = call.receive<UpsertHolidayRequest>()
            val created = AdminHolidayRepository.create(parkingId, body)
            call.auditOk(parkingId, actor, "holiday.created", "holiday", created.id,
                """{"date":"${body.dateIso}","name":"${body.name}"}""")
            call.respond(HttpStatusCode.Created, created)
        }
        delete("/{holidayId}") {
            val parkingId = call.requireMatchingParking() ?: return@delete
            val actor = call.actorUserId()
            val holidayId = call.parameters["holidayId"]!!
            val n = AdminHolidayRepository.delete(parkingId, holidayId)
            if (n == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "holiday_not_found"))
            else {
                call.auditOk(parkingId, actor, "holiday.deleted", "holiday", holidayId, "{}")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

// ───── Helpers de auth/audit ───────────────────────────────────────────────

/**
 * Verifica que el `parkingId` del path coincida con el claim del JWT.
 * Responde 403 y devuelve null si no coincide; en caso OK devuelve el id.
 */
private suspend fun ApplicationCall.requireMatchingParking(): String? {
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

private fun ApplicationCall.actorUserId(): String? =
    principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

private fun ApplicationCall.auditOk(
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
