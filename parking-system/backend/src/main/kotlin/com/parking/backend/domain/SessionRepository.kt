package com.parking.backend.routes

import com.parking.shared.data.api.dto.SessionDto
import com.parking.shared.domain.model.SessionStatus
import com.parking.shared.domain.model.VehicleType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

/**
 * Tablas Exposed para sesiones y zonas, junto con un repositorio fino.
 */

object ParkingSessionsT : Table("parking_sessions") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val localId = uuid("local_id")
    val plate = varchar("plate", 16)
    val vehicleType = varchar("vehicle_type", 20)
    val zoneId = uuid("zone_id").nullable()
    val operatorUserId = uuid("operator_user_id").nullable()
    val entryGate = varchar("entry_gate", 32).nullable()
    val entryAt = timestamp("entry_at")
    val exitAt = timestamp("exit_at").nullable()
    val status = varchar("status", 20)
    val totalCents = long("total_cents").nullable()
    val ivaCents = long("iva_cents").nullable()
}

object ZonesT : Table("zones") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val code = varchar("code", 16)
    val capacity = integer("capacity")
    val currentOccupancy = integer("current_occupancy")
    val allowed = varchar("allowed_vehicle_types", 200)
}

data class ZoneOccupancySnapshot(val code: String, val capacity: Int, val occupancy: Int)

object SessionRepository {

    fun openSession(body: SessionDto, operatorUserId: String): SessionDto = transaction {
        val sessionId = UUID.randomUUID()
        ParkingSessionsT.insert {
            it[id] = sessionId
            it[parkingId] = UUID.fromString(body.parkingId)
            it[localId] = UUID.fromString(body.localId)
            it[plate] = body.plate
            it[vehicleType] = body.vehicleType.name
            it[zoneId] = body.zoneId?.let(UUID::fromString)
            it[ParkingSessionsT.operatorUserId] = UUID.fromString(operatorUserId)
            it[entryGate] = body.entryGate
            it[entryAt] = Instant.ofEpochMilli(body.entryAtMillis)
            it[status] = SessionStatus.ACTIVE.name
        }
        body.zoneId?.let { zone ->
            ZonesT.update({ ZonesT.id eq UUID.fromString(zone) }) {
                it[currentOccupancy] = currentOccupancy + 1
            }
        }
        body.copy(id = sessionId.toString(), status = SessionStatus.ACTIVE)
    }

    fun closeSession(sessionId: String, parkingId: String, exitAtMillis: Long, totalCents: Long, ivaCents: Long): SessionDto = transaction {
        val sUuid = UUID.fromString(sessionId)
        val pUuid = UUID.fromString(parkingId)
        ParkingSessionsT.update({ ParkingSessionsT.id eq sUuid and (ParkingSessionsT.parkingId eq pUuid) }) {
            it[exitAt] = Instant.ofEpochMilli(exitAtMillis)
            it[status] = SessionStatus.CLOSED.name
            it[ParkingSessionsT.totalCents] = totalCents
            it[ParkingSessionsT.ivaCents] = ivaCents
        }
        val row = ParkingSessionsT.select { ParkingSessionsT.id eq sUuid }.single()
        val zoneId = row[ParkingSessionsT.zoneId]
        if (zoneId != null) {
            ZonesT.update({ ZonesT.id eq zoneId }) { it[currentOccupancy] = currentOccupancy - 1 }
        }
        SessionDto(
            id = sessionId,
            localId = row[ParkingSessionsT.localId].toString(),
            parkingId = parkingId,
            plate = row[ParkingSessionsT.plate],
            vehicleType = VehicleType.valueOf(row[ParkingSessionsT.vehicleType]),
            zoneId = zoneId?.toString(),
            entryAtMillis = row[ParkingSessionsT.entryAt].toEpochMilli(),
            exitAtMillis = exitAtMillis,
            status = SessionStatus.CLOSED,
            operatorUserId = row[ParkingSessionsT.operatorUserId]?.toString(),
            entryGate = row[ParkingSessionsT.entryGate],
            totalCents = totalCents,
            ivaCents = ivaCents,
        )
    }

    fun activeSessions(parkingId: String): List<SessionDto> = transaction {
        ParkingSessionsT.select {
            ParkingSessionsT.parkingId eq UUID.fromString(parkingId) and
                (ParkingSessionsT.status eq SessionStatus.ACTIVE.name)
        }.map { row ->
            SessionDto(
                id = row[ParkingSessionsT.id].toString(),
                localId = row[ParkingSessionsT.localId].toString(),
                parkingId = parkingId,
                plate = row[ParkingSessionsT.plate],
                vehicleType = VehicleType.valueOf(row[ParkingSessionsT.vehicleType]),
                zoneId = row[ParkingSessionsT.zoneId]?.toString(),
                entryAtMillis = row[ParkingSessionsT.entryAt].toEpochMilli(),
                exitAtMillis = row[ParkingSessionsT.exitAt]?.toEpochMilli(),
                status = SessionStatus.valueOf(row[ParkingSessionsT.status]),
                operatorUserId = row[ParkingSessionsT.operatorUserId]?.toString(),
                entryGate = row[ParkingSessionsT.entryGate],
                totalCents = row[ParkingSessionsT.totalCents],
                ivaCents = row[ParkingSessionsT.ivaCents],
            )
        }
    }

    fun zoneOccupancy(zoneId: String): ZoneOccupancySnapshot = transaction {
        val row = ZonesT.select { ZonesT.id eq UUID.fromString(zoneId) }.single()
        ZoneOccupancySnapshot(row[ZonesT.code], row[ZonesT.capacity], row[ZonesT.currentOccupancy])
    }
}
