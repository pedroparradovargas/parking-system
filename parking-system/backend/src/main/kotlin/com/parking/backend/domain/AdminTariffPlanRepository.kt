package com.parking.backend.routes

import com.parking.shared.data.api.dto.TariffPlanDto
import com.parking.shared.data.api.dto.UpsertTariffPlanRequest
import com.parking.shared.domain.model.VehicleType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

/** Tabla `tariff_plans` (V2). */
object TariffPlansT : Table("tariff_plans") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val name = varchar("name", 80)
    val durationDays = integer("duration_days")
    val priceCents = long("price_cents")
    val vehicleType = varchar("vehicle_type", 20)
    val enabled = bool("enabled")
    val createdAt = timestamp("created_at")
}

object AdminTariffPlanRepository {

    fun list(parkingId: String): List<TariffPlanDto> = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        TariffPlansT.selectAll()
            .where { TariffPlansT.parkingId eq parkingUuid }
            .orderBy(TariffPlansT.durationDays to SortOrder.ASC)
            .map { it.toDto() }
    }

    fun create(parkingId: String, req: UpsertTariffPlanRequest): TariffPlanDto = transaction {
        validate(req)
        val newId = UUID.randomUUID()
        TariffPlansT.insert {
            it[id] = newId
            it[TariffPlansT.parkingId] = UUID.fromString(parkingId)
            it[name] = req.name
            it[durationDays] = req.durationDays
            it[priceCents] = req.priceCents
            it[vehicleType] = req.vehicleType.name
            it[enabled] = req.enabled
            it[createdAt] = Instant.now()
        }
        TariffPlansT.selectAll().where { TariffPlansT.id eq newId }.single().toDto()
    }

    fun update(parkingId: String, planId: String, req: UpsertTariffPlanRequest): TariffPlanDto = transaction {
        validate(req)
        val parkingUuid = UUID.fromString(parkingId)
        val targetUuid = UUID.fromString(planId)
        val updated = TariffPlansT.update({
            (TariffPlansT.id eq targetUuid) and (TariffPlansT.parkingId eq parkingUuid)
        }) {
            it[name] = req.name
            it[durationDays] = req.durationDays
            it[priceCents] = req.priceCents
            it[vehicleType] = req.vehicleType.name
            it[enabled] = req.enabled
        }
        if (updated == 0) error("tariff_plan_not_found")
        TariffPlansT.selectAll().where { TariffPlansT.id eq targetUuid }.single().toDto()
    }

    fun delete(parkingId: String, planId: String): Int = transaction {
        TariffPlansT.deleteWhere {
            (TariffPlansT.id eq UUID.fromString(planId)) and
                (TariffPlansT.parkingId eq UUID.fromString(parkingId))
        }
    }

    fun listSince(parkingId: String, sinceMillis: Long): List<TariffPlanDto> = transaction {
        // Para el SyncManager: planes creados desde un timestamp.
        val parkingUuid = UUID.fromString(parkingId)
        TariffPlansT.selectAll()
            .where { TariffPlansT.parkingId eq parkingUuid }
            .map { it.toDto() }
            .filter { it.createdAtMillis >= sinceMillis }
    }

    private fun validate(req: UpsertTariffPlanRequest) {
        require(req.name.isNotBlank()) { "name required" }
        require(req.durationDays > 0) { "duration_days must be positive" }
        require(req.priceCents >= 0) { "price_cents must be non-negative" }
    }

    private fun ResultRow.toDto(): TariffPlanDto = TariffPlanDto(
        id = this[TariffPlansT.id].toString(),
        parkingId = this[TariffPlansT.parkingId].toString(),
        name = this[TariffPlansT.name],
        durationDays = this[TariffPlansT.durationDays],
        priceCents = this[TariffPlansT.priceCents],
        vehicleType = VehicleType.valueOf(this[TariffPlansT.vehicleType]),
        enabled = this[TariffPlansT.enabled],
        createdAtMillis = this[TariffPlansT.createdAt].toEpochMilli(),
    )
}
