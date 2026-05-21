package com.parking.backend.routes

import com.parking.shared.data.api.dto.ReceiptDto
import com.parking.shared.data.api.dto.TariffDto
import com.parking.shared.data.api.dto.ZoneDto
import com.parking.shared.domain.model.VehicleType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

private object ReceiptsT : Table("receipts") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val sessionId = uuid("session_id")
    val localId = uuid("local_id")
    val sequenceLocal = varchar("sequence_local", 32)
    val issuedAt = timestamp("issued_at")
    val subtotalCents = long("subtotal_cents")
    val ivaCents = long("iva_cents")
    val totalCents = long("total_cents")
    val cufe = varchar("cufe", 255).nullable()
}

private object TariffsT : Table("tariffs") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val vehicleType = varchar("vehicle_type", 20)
    val firstHourCents = long("first_hour_cents")
    val subsequentHourCents = long("subsequent_hour_cents")
    val nightSurchargePercent = integer("night_surcharge_percent")
    val nightFrom = time("night_from")
    val nightTo = time("night_to")
    val graceMinutes = integer("grace_minutes")
    val ivaPercent = integer("iva_percent")
    val validFrom = timestamp("valid_from")
    val validTo = timestamp("valid_to").nullable()
}

private object SyncLogT : Table("sync_log") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val deviceId = varchar("device_id", 80)
    val payloadSize = integer("payload_size")
    val acceptedCount = integer("accepted_count")
    val conflictCount = integer("conflict_count")
    val rejectedCount = integer("rejected_count")
}

object SyncRepository {

    /** Idempotente: si `local_id` ya existe en `receipts`, devuelve su `id`. */
    fun upsertReceipt(dto: ReceiptDto, deviceId: String): String = transaction {
        val existing = ReceiptsT
            .selectAll()
            .where { ReceiptsT.parkingId eq UUID.fromString(dto.parkingId) and (ReceiptsT.localId eq UUID.fromString(dto.localId)) }
            .singleOrNull()
        if (existing != null) return@transaction existing[ReceiptsT.id].toString()

        val id = UUID.randomUUID()
        ReceiptsT.insert {
            it[ReceiptsT.id] = id
            it[parkingId] = UUID.fromString(dto.parkingId)
            it[sessionId] = UUID.fromString(dto.sessionId)
            it[localId] = UUID.fromString(dto.localId)
            it[sequenceLocal] = dto.sequenceLocal
            it[issuedAt] = Instant.ofEpochMilli(dto.issuedAtMillis)
            it[subtotalCents] = dto.subtotalCents
            it[ivaCents] = dto.ivaCents
            it[totalCents] = dto.totalCents
            it[cufe] = dto.cufe
        }
        id.toString()
    }

    fun tariffsSince(parkingId: String, sinceMillis: Long): List<TariffDto> = transaction {
        TariffsT.selectAll().where {
            TariffsT.parkingId eq UUID.fromString(parkingId) and
                (TariffsT.validFrom greaterEq Instant.ofEpochMilli(sinceMillis))
        }.map { row ->
            TariffDto(
                id = row[TariffsT.id].toString(),
                parkingId = parkingId,
                vehicleType = VehicleType.valueOf(row[TariffsT.vehicleType]),
                firstHourCents = row[TariffsT.firstHourCents],
                subsequentHourCents = row[TariffsT.subsequentHourCents],
                nightSurchargePercent = row[TariffsT.nightSurchargePercent],
                nightFromIso = row[TariffsT.nightFrom].toString(),
                nightToIso = row[TariffsT.nightTo].toString(),
                graceMinutes = row[TariffsT.graceMinutes],
                ivaPercent = row[TariffsT.ivaPercent],
                validFromMillis = row[TariffsT.validFrom].toEpochMilli(),
                validToMillis = row[TariffsT.validTo]?.toEpochMilli(),
            )
        }
    }

    fun zonesSnapshot(parkingId: String): List<ZoneDto> = transaction {
        ZonesT.selectAll().where { ZonesT.parkingId eq UUID.fromString(parkingId) }.map { row ->
            ZoneDto(
                id = row[ZonesT.id].toString(),
                parkingId = parkingId,
                code = row[ZonesT.code],
                capacity = row[ZonesT.capacity],
                currentOccupancy = row[ZonesT.currentOccupancy],
                allowedVehicleTypes = row[ZonesT.allowed].split(",").filter { it.isNotBlank() }.map { VehicleType.valueOf(it) },
            )
        }
    }

    fun logSync(parkingId: String, deviceId: String, payloadSize: Int, accepted: Int, conflicts: Int, rejected: Int) {
        transaction {
            SyncLogT.insert {
                it[SyncLogT.parkingId] = UUID.fromString(parkingId)
                it[SyncLogT.deviceId] = deviceId
                it[SyncLogT.payloadSize] = payloadSize
                it[acceptedCount] = accepted
                it[conflictCount] = conflicts
                it[rejectedCount] = rejected
            }
        }
    }
}
