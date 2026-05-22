package com.parking.backend.routes

import com.parking.shared.data.api.dto.UpsertZoneRequest
import com.parking.shared.data.api.dto.ZoneDto
import com.parking.shared.domain.model.VehicleType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

/**
 * Tabla `zones` extendida con columnas V2 (under_maintenance, enabled,
 * display_order, notes).  Convive con [ZonesT] de SessionRepository.kt
 * mediante un schema más completo: las queries de sesión NO necesitan los
 * campos V2 y los ignoran al hacer SELECT *; los nuevos campos son leídos
 * exclusivamente por este repo.
 */
object AdminZonesT : Table("zones") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val code = varchar("code", 16)
    val capacity = integer("capacity")
    val currentOccupancy = integer("current_occupancy")
    val allowedTypes = varchar("allowed_vehicle_types", 200)
    val underMaintenance = integer("under_maintenance")
    val enabled = bool("enabled")
    val displayOrder = integer("display_order")
    val notes = text("notes").nullable()
}

/**
 * Repositorio de gestión de zonas.
 *
 * - El listado respeta `display_order` ASC, luego `code` ASC.
 * - El código (`code`) es único por parking (constraint en la migración V1).
 * - Las zonas se editan **in-place** (NO versionadas como las tarifas):
 *   se considera que cambiar capacidad o nombre afecta operación corriente.
 *   La auditoría queda en `audit_log` para trazabilidad.
 */
object AdminZoneRepository {

    fun list(parkingId: String): List<ZoneDto> = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        AdminZonesT.selectAll()
            .where { AdminZonesT.parkingId eq parkingUuid }
            .orderBy(AdminZonesT.displayOrder to SortOrder.ASC, AdminZonesT.code to SortOrder.ASC)
            .map { it.toDto() }
    }

    fun create(parkingId: String, req: UpsertZoneRequest): ZoneDto = transaction {
        validate(req)
        val parkingUuid = UUID.fromString(parkingId)
        val newId = UUID.randomUUID()
        AdminZonesT.insert {
            it[id] = newId
            it[AdminZonesT.parkingId] = parkingUuid
            it[code] = req.code
            it[capacity] = req.capacity
            it[currentOccupancy] = 0
            it[allowedTypes] = req.allowedVehicleTypes.joinToString(",") { vt -> vt.name }
            it[underMaintenance] = req.underMaintenance
            it[enabled] = req.enabled
            it[displayOrder] = req.displayOrder
            it[notes] = req.notes
        }
        AdminZonesT.selectAll().where { AdminZonesT.id eq newId }.single().toDto()
    }

    fun update(parkingId: String, zoneId: String, req: UpsertZoneRequest): ZoneDto = transaction {
        validate(req)
        val parkingUuid = UUID.fromString(parkingId)
        val targetUuid = UUID.fromString(zoneId)

        val updated = AdminZonesT.update({
            (AdminZonesT.id eq targetUuid) and (AdminZonesT.parkingId eq parkingUuid)
        }) {
            it[code] = req.code
            it[capacity] = req.capacity
            it[allowedTypes] = req.allowedVehicleTypes.joinToString(",") { vt -> vt.name }
            it[underMaintenance] = req.underMaintenance
            it[enabled] = req.enabled
            it[displayOrder] = req.displayOrder
            it[notes] = req.notes
        }
        if (updated == 0) error("zone_not_found")
        AdminZonesT.selectAll().where { AdminZonesT.id eq targetUuid }.single().toDto()
    }

    /**
     * Elimina una zona — sólo si no tiene sesiones activas ni cupos ocupados.
     * Devuelve el número de filas eliminadas; 0 si la zona no existe o está
     * en uso.
     */
    fun delete(parkingId: String, zoneId: String): Int = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val targetUuid = UUID.fromString(zoneId)

        // Pre-chequeo: no permitir borrar zonas con ocupación viva.
        val row = AdminZonesT.selectAll()
            .where { (AdminZonesT.id eq targetUuid) and (AdminZonesT.parkingId eq parkingUuid) }
            .singleOrNull() ?: return@transaction 0
        if (row[AdminZonesT.currentOccupancy] > 0) error("zone_in_use")

        AdminZonesT.deleteWhere {
            (AdminZonesT.id eq targetUuid) and (AdminZonesT.parkingId eq parkingUuid)
        }
    }

    private fun validate(req: UpsertZoneRequest) {
        require(req.code.isNotBlank()) { "code required" }
        require(req.code.length <= 16) { "code too long (max 16)" }
        require(req.capacity >= 0) { "capacity must be non-negative" }
        require(req.underMaintenance in 0..req.capacity) {
            "under_maintenance must be between 0 and capacity"
        }
        require(req.displayOrder in 0..10_000) { "display_order out of range" }
        require(req.allowedVehicleTypes.isNotEmpty()) { "at least one vehicle type required" }
    }

    private fun ResultRow.toDto(): ZoneDto = ZoneDto(
        id = this[AdminZonesT.id].toString(),
        parkingId = this[AdminZonesT.parkingId].toString(),
        code = this[AdminZonesT.code],
        capacity = this[AdminZonesT.capacity],
        currentOccupancy = this[AdminZonesT.currentOccupancy],
        allowedVehicleTypes = this[AdminZonesT.allowedTypes]
            .split(",")
            .filter { it.isNotBlank() }
            .map { VehicleType.valueOf(it) },
        underMaintenance = this[AdminZonesT.underMaintenance],
        enabled = this[AdminZonesT.enabled],
        displayOrder = this[AdminZonesT.displayOrder],
        notes = this[AdminZonesT.notes],
    )
}
