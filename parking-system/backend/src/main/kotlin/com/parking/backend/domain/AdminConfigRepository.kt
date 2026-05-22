package com.parking.backend.routes

import com.parking.shared.data.api.dto.ParkingConfigDto
import com.parking.shared.data.api.dto.UpsertParkingConfigRequest
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

object ParkingConfigT : Table("parking_config") {
    val parkingId = uuid("parking_id")
    val legalName = varchar("legal_name", 200).nullable()
    val taxId = varchar("tax_id", 40).nullable()
    val legalAddress = varchar("legal_address", 300).nullable()
    val city = varchar("city", 120).nullable()
    val dianResolution = varchar("dian_resolution", 120).nullable()
    val dianResolutionFrom = date("dian_resolution_from").nullable()
    val dianResolutionTo = date("dian_resolution_to").nullable()
    val invoiceSeries = varchar("invoice_series", 20).nullable()
    val invoiceNextSequence = long("invoice_next_sequence")
    val timezone = varchar("timezone", 60)
    val operatingMode = varchar("operating_mode", 20)
    val operatingScheduleJson = text("operating_schedule_json").nullable()
    val aiServiceUrl = varchar("ai_service_url", 300).nullable()
    val notificationEmail = varchar("notification_email", 200).nullable()
    val updatedAt = timestamp("updated_at")
    val updatedBy = uuid("updated_by").nullable()

    override val primaryKey = PrimaryKey(parkingId)
}

object AdminConfigRepository {

    fun get(parkingId: String): ParkingConfigDto = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val row = ParkingConfigT.selectAll()
            .where { ParkingConfigT.parkingId eq parkingUuid }
            .singleOrNull()
        if (row == null) {
            // No existe registro — devolvemos defaults.  El primer PUT crea la fila.
            ParkingConfigDto(parkingId = parkingId, updatedAtMillis = Instant.now().toEpochMilli())
        } else ParkingConfigDto(
            parkingId = parkingId,
            legalName = row[ParkingConfigT.legalName],
            taxId = row[ParkingConfigT.taxId],
            legalAddress = row[ParkingConfigT.legalAddress],
            city = row[ParkingConfigT.city],
            dianResolution = row[ParkingConfigT.dianResolution],
            dianResolutionFromIso = row[ParkingConfigT.dianResolutionFrom]?.toString(),
            dianResolutionToIso = row[ParkingConfigT.dianResolutionTo]?.toString(),
            invoiceSeries = row[ParkingConfigT.invoiceSeries],
            invoiceNextSequence = row[ParkingConfigT.invoiceNextSequence],
            timezone = row[ParkingConfigT.timezone],
            operatingMode = row[ParkingConfigT.operatingMode],
            operatingScheduleJson = row[ParkingConfigT.operatingScheduleJson],
            aiServiceUrl = row[ParkingConfigT.aiServiceUrl],
            notificationEmail = row[ParkingConfigT.notificationEmail],
            updatedAtMillis = row[ParkingConfigT.updatedAt].toEpochMilli(),
        )
    }

    fun upsert(parkingId: String, req: UpsertParkingConfigRequest, actorUserId: String?): ParkingConfigDto = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val now = Instant.now()
        val actor = actorUserId?.let(UUID::fromString)

        val exists = ParkingConfigT.selectAll()
            .where { ParkingConfigT.parkingId eq parkingUuid }
            .any()

        if (!exists) {
            ParkingConfigT.insert {
                it[ParkingConfigT.parkingId] = parkingUuid
                it[legalName] = req.legalName
                it[taxId] = req.taxId
                it[legalAddress] = req.legalAddress
                it[city] = req.city
                it[dianResolution] = req.dianResolution
                it[dianResolutionFrom] = req.dianResolutionFromIso?.let(LocalDate::parse)
                it[dianResolutionTo] = req.dianResolutionToIso?.let(LocalDate::parse)
                it[invoiceSeries] = req.invoiceSeries
                it[invoiceNextSequence] = req.invoiceNextSequence ?: 1L
                it[timezone] = req.timezone ?: "America/Bogota"
                it[operatingMode] = req.operatingMode ?: "24x7"
                it[operatingScheduleJson] = req.operatingScheduleJson
                it[aiServiceUrl] = req.aiServiceUrl
                it[notificationEmail] = req.notificationEmail
                it[updatedAt] = now
                it[updatedBy] = actor
            }
        } else {
            ParkingConfigT.update({ ParkingConfigT.parkingId eq parkingUuid }) {
                req.legalName?.let { v -> it[legalName] = v }
                req.taxId?.let { v -> it[taxId] = v }
                req.legalAddress?.let { v -> it[legalAddress] = v }
                req.city?.let { v -> it[city] = v }
                req.dianResolution?.let { v -> it[dianResolution] = v }
                req.dianResolutionFromIso?.let { v -> it[dianResolutionFrom] = LocalDate.parse(v) }
                req.dianResolutionToIso?.let { v -> it[dianResolutionTo] = LocalDate.parse(v) }
                req.invoiceSeries?.let { v -> it[invoiceSeries] = v }
                req.invoiceNextSequence?.let { v -> it[invoiceNextSequence] = v }
                req.timezone?.let { v -> it[timezone] = v }
                req.operatingMode?.let { v -> it[operatingMode] = v }
                req.operatingScheduleJson?.let { v -> it[operatingScheduleJson] = v }
                req.aiServiceUrl?.let { v -> it[aiServiceUrl] = v }
                req.notificationEmail?.let { v -> it[notificationEmail] = v }
                it[updatedAt] = now
                it[updatedBy] = actor
            }
        }
        get(parkingId)
    }
}
