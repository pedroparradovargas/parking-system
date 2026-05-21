package com.parking.backend.routes

import com.parking.shared.data.api.dto.OccupancyReportDto
import com.parking.shared.data.api.dto.RevenueBucket
import com.parking.shared.data.api.dto.RevenueReportDto
import com.parking.shared.data.api.dto.ZoneOccupancy
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Reportes ejecutivos.  Implementación intencionalmente sencilla: agregados
 * en memoria a partir de Exposed.  Para volumen alto pasar a SQL nativo o
 * usar TimescaleDB hyperfunctions (time_bucket).
 */
object ReportsRepository {

    fun revenue(parkingId: String, fromIso: String, toIso: String): RevenueReportDto = transaction {
        val from = parseDateOrEpoch(fromIso, default = Instant.now().minusSeconds(86400 * 30))
        val to = parseDateOrEpoch(toIso, default = Instant.now())
        val rows = ParkingSessionsT.selectAll().where {
            ParkingSessionsT.parkingId eq UUID.fromString(parkingId) and
                (ParkingSessionsT.exitAt greaterEqNullable from) and
                (ParkingSessionsT.exitAt lessEqNullable to)
        }.toList()

        var totalRevenue = 0L
        var totalIva = 0L
        val byType = mutableMapOf<String, Long>()
        val byDay = mutableMapOf<String, Pair<Long, Int>>()
        rows.forEach { row ->
            val total = row[ParkingSessionsT.totalCents] ?: 0L
            val iva = row[ParkingSessionsT.ivaCents] ?: 0L
            val type = row[ParkingSessionsT.vehicleType]
            val exit = row[ParkingSessionsT.exitAt] ?: return@forEach
            totalRevenue += total
            totalIva += iva
            byType[type] = (byType[type] ?: 0L) + total
            val dayKey = exit.atZone(ZoneId.of("America/Bogota")).toLocalDate().toString()
            val prev = byDay[dayKey] ?: (0L to 0)
            byDay[dayKey] = (prev.first + total) to (prev.second + 1)
        }
        RevenueReportDto(
            parkingId = parkingId,
            fromIso = fromIso,
            toIso = toIso,
            totalRevenueCents = totalRevenue,
            totalIvaCents = totalIva,
            sessionsCount = rows.size,
            byVehicleType = byType,
            byDay = byDay.entries.sortedBy { it.key }.map { (k, v) -> RevenueBucket(k, v.first, v.second) },
        )
    }

    fun occupancy(parkingId: String): OccupancyReportDto = transaction {
        val zones = ZonesT.selectAll().where { ZonesT.parkingId eq UUID.fromString(parkingId) }.toList()
        val capacityTotal = zones.sumOf { it[ZonesT.capacity] }
        val occupancyTotal = zones.sumOf { it[ZonesT.currentOccupancy] }
        OccupancyReportDto(
            parkingId = parkingId,
            capacityTotal = capacityTotal,
            currentOccupancy = occupancyTotal,
            byZone = zones.map { ZoneOccupancy(it[ZonesT.code], it[ZonesT.capacity], it[ZonesT.currentOccupancy]) },
            percentage = if (capacityTotal == 0) 0.0 else occupancyTotal.toDouble() / capacityTotal * 100.0,
        )
    }

    private fun parseDateOrEpoch(iso: String, default: Instant): Instant = try {
        if (iso.isBlank()) default
        else if (iso.length == 10) LocalDate.parse(iso).atStartOfDay(ZoneId.of("America/Bogota")).toInstant()
        else Instant.parse(iso)
    } catch (_: Throwable) { default }
}

// Helpers Exposed para comparar timestamp nullable contra Instant.
private infix fun org.jetbrains.exposed.sql.Column<Instant?>.greaterEqNullable(value: Instant) =
    org.jetbrains.exposed.sql.GreaterEqOp(this, org.jetbrains.exposed.sql.QueryParameter(value, this.columnType))

private infix fun org.jetbrains.exposed.sql.Column<Instant?>.lessEqNullable(value: Instant) =
    org.jetbrains.exposed.sql.LessEqOp(this, org.jetbrains.exposed.sql.QueryParameter(value, this.columnType))
