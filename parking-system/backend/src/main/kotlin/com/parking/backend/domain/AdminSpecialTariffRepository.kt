package com.parking.backend.routes

import com.parking.shared.data.api.dto.HolidayDto
import com.parking.shared.data.api.dto.SpecialTariffDto
import com.parking.shared.data.api.dto.SpecialTariffRule
import com.parking.shared.data.api.dto.UpsertHolidayRequest
import com.parking.shared.data.api.dto.UpsertSpecialTariffRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Tabla `special_tariffs` (V2). */
object SpecialTariffsT : Table("special_tariffs") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val name = varchar("name", 80)
    val ruleType = varchar("rule_type", 20)
    val multiplier = decimal("multiplier", 4, 2)
    val dateFrom = date("date_from").nullable()
    val dateTo = date("date_to").nullable()
    val dayOfWeekCsv = varchar("day_of_week_csv", 32).nullable()
    val enabled = bool("enabled")
    val createdAt = timestamp("created_at")
}

/** Tabla `holidays` (V2). */
object HolidaysT : Table("holidays") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val date = date("date")
    val name = varchar("name", 120)
    val createdAt = timestamp("created_at")
}

object AdminSpecialTariffRepository {

    fun list(parkingId: String): List<SpecialTariffDto> = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        SpecialTariffsT.selectAll()
            .where { SpecialTariffsT.parkingId eq parkingUuid }
            .orderBy(SpecialTariffsT.name to SortOrder.ASC)
            .map { it.toDto() }
    }

    fun create(parkingId: String, req: UpsertSpecialTariffRequest): SpecialTariffDto = transaction {
        validate(req)
        val newId = UUID.randomUUID()
        SpecialTariffsT.insert {
            it[id] = newId
            it[SpecialTariffsT.parkingId] = UUID.fromString(parkingId)
            it[name] = req.name
            it[ruleType] = req.ruleType.name
            it[multiplier] = BigDecimal(req.multiplier.toString())
            it[dateFrom] = req.dateFromIso?.let(LocalDate::parse)
            it[dateTo] = req.dateToIso?.let(LocalDate::parse)
            it[dayOfWeekCsv] = req.dayOfWeekCsv
            it[enabled] = req.enabled
            it[createdAt] = Instant.now()
        }
        SpecialTariffsT.selectAll().where { SpecialTariffsT.id eq newId }.single().toDto()
    }

    fun update(parkingId: String, specialId: String, req: UpsertSpecialTariffRequest): SpecialTariffDto = transaction {
        validate(req)
        val parkingUuid = UUID.fromString(parkingId)
        val targetUuid = UUID.fromString(specialId)
        val updated = SpecialTariffsT.update({
            (SpecialTariffsT.id eq targetUuid) and (SpecialTariffsT.parkingId eq parkingUuid)
        }) {
            it[name] = req.name
            it[ruleType] = req.ruleType.name
            it[multiplier] = BigDecimal(req.multiplier.toString())
            it[dateFrom] = req.dateFromIso?.let(LocalDate::parse)
            it[dateTo] = req.dateToIso?.let(LocalDate::parse)
            it[dayOfWeekCsv] = req.dayOfWeekCsv
            it[enabled] = req.enabled
        }
        if (updated == 0) error("special_tariff_not_found")
        SpecialTariffsT.selectAll().where { SpecialTariffsT.id eq targetUuid }.single().toDto()
    }

    fun delete(parkingId: String, specialId: String): Int = transaction {
        SpecialTariffsT.deleteWhere {
            (SpecialTariffsT.id eq UUID.fromString(specialId)) and
                (SpecialTariffsT.parkingId eq UUID.fromString(parkingId))
        }
    }

    private fun validate(req: UpsertSpecialTariffRequest) {
        require(req.name.isNotBlank()) { "name required" }
        require(req.multiplier in 0.0..10.0) { "multiplier out of range [0, 10]" }
        when (req.ruleType) {
            SpecialTariffRule.DATE_RANGE -> {
                require(req.dateFromIso != null && req.dateToIso != null) {
                    "DATE_RANGE requires date_from and date_to"
                }
                val from = LocalDate.parse(req.dateFromIso)
                val to = LocalDate.parse(req.dateToIso)
                require(!to.isBefore(from)) { "date_to must be >= date_from" }
            }
            SpecialTariffRule.DAY_OF_WEEK -> {
                val csv = req.dayOfWeekCsv
                require(!csv.isNullOrBlank()) { "DAY_OF_WEEK requires day_of_week_csv" }
                csv.split(",").forEach { d ->
                    val n = d.trim().toIntOrNull()
                    require(n != null && n in 1..7) { "day_of_week_csv must be 1..7 ints" }
                }
            }
            SpecialTariffRule.WEEKEND, SpecialTariffRule.HOLIDAY -> { /* sin params */ }
        }
    }

    private fun ResultRow.toDto(): SpecialTariffDto = SpecialTariffDto(
        id = this[SpecialTariffsT.id].toString(),
        parkingId = this[SpecialTariffsT.parkingId].toString(),
        name = this[SpecialTariffsT.name],
        ruleType = SpecialTariffRule.valueOf(this[SpecialTariffsT.ruleType]),
        multiplier = this[SpecialTariffsT.multiplier].toDouble(),
        dateFromIso = this[SpecialTariffsT.dateFrom]?.toString(),
        dateToIso = this[SpecialTariffsT.dateTo]?.toString(),
        dayOfWeekCsv = this[SpecialTariffsT.dayOfWeekCsv],
        enabled = this[SpecialTariffsT.enabled],
        createdAtMillis = this[SpecialTariffsT.createdAt].toEpochMilli(),
    )
}

object AdminHolidayRepository {

    fun list(parkingId: String): List<HolidayDto> = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        HolidaysT.selectAll()
            .where { HolidaysT.parkingId eq parkingUuid }
            .orderBy(HolidaysT.date to SortOrder.ASC)
            .map { it.toDto() }
    }

    fun create(parkingId: String, req: UpsertHolidayRequest): HolidayDto = transaction {
        require(req.name.isNotBlank()) { "name required" }
        val newId = UUID.randomUUID()
        HolidaysT.insert {
            it[id] = newId
            it[HolidaysT.parkingId] = UUID.fromString(parkingId)
            it[date] = LocalDate.parse(req.dateIso)
            it[name] = req.name
            it[createdAt] = Instant.now()
        }
        HolidaysT.selectAll().where { HolidaysT.id eq newId }.single().toDto()
    }

    fun delete(parkingId: String, holidayId: String): Int = transaction {
        HolidaysT.deleteWhere {
            (HolidaysT.id eq UUID.fromString(holidayId)) and
                (HolidaysT.parkingId eq UUID.fromString(parkingId))
        }
    }

    private fun ResultRow.toDto(): HolidayDto = HolidayDto(
        id = this[HolidaysT.id].toString(),
        parkingId = this[HolidaysT.parkingId].toString(),
        dateIso = this[HolidaysT.date].toString(),
        name = this[HolidaysT.name],
        createdAtMillis = this[HolidaysT.createdAt].toEpochMilli(),
    )
}
