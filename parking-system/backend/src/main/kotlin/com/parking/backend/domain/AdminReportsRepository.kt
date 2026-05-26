package com.parking.backend.routes

import com.parking.shared.data.api.dto.CashClosingReportDto
import com.parking.shared.data.api.dto.CashClosingRowDto
import com.parking.shared.data.api.dto.MonthlyCustomersReportDto
import com.parking.shared.data.api.dto.TopPlateRowDto
import com.parking.shared.data.api.dto.TopPlatesReportDto
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Reportes ejecutivos del módulo Admin.  Construye agregados en memoria
 * a partir de `parking_sessions` y tablas de mensualidades.  Para volumen
 * alto considerar mover a SQL nativo o TimescaleDB hyperfunctions.
 */
object AdminReportsRepository {

    fun cashClosing(parkingId: String, fromIso: String, toIso: String): CashClosingReportDto = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val zone = ZoneId.of("America/Bogota")
        val from = parseDate(fromIso, default = Instant.now().minusSeconds(86400 * 30))
        val to = parseDate(toIso, default = Instant.now())

        val rows = ParkingSessionsT.selectAll()
            .where {
                (ParkingSessionsT.parkingId eq parkingUuid) and
                    (ParkingSessionsT.status eq "CLOSED")
            }
            .toList()
            .filter { row ->
                val exit = row[ParkingSessionsT.exitAt] ?: return@filter false
                !exit.isBefore(from) && !exit.isAfter(to)
            }

        // Carga nombres de operador.
        val operatorIds = rows.mapNotNull { it[ParkingSessionsT.operatorUserId] }.distinct()
        val operatorNames: Map<UUID, String> = if (operatorIds.isEmpty()) emptyMap()
        else AdminUsersT.selectAll().where { AdminUsersT.id.inList(operatorIds) }
            .associate { it[AdminUsersT.id] to it[AdminUsersT.fullName] }

        // Agrupa por (operador, día).
        data class Key(val operatorId: UUID?, val day: String)
        val grouped = mutableMapOf<Key, Triple<Int, Long, Long>>()  // (sessions, total, iva)
        rows.forEach { row ->
            val exit = row[ParkingSessionsT.exitAt]!!
            val day = ZonedDateTime.ofInstant(exit, zone).toLocalDate().toString()
            val key = Key(row[ParkingSessionsT.operatorUserId], day)
            val prev = grouped[key] ?: Triple(0, 0L, 0L)
            grouped[key] = Triple(
                prev.first + 1,
                prev.second + (row[ParkingSessionsT.totalCents] ?: 0L),
                prev.third + (row[ParkingSessionsT.ivaCents] ?: 0L),
            )
        }

        val outRows = grouped.entries.sortedWith(compareBy({ it.key.day }, { it.key.operatorId.toString() })).map { (k, v) ->
            CashClosingRowDto(
                operatorUserId = k.operatorId?.toString() ?: "",
                operatorName = k.operatorId?.let(operatorNames::get) ?: "(sin operador)",
                dayIso = k.day,
                sessionsCount = v.first,
                totalCents = v.second,
                ivaCents = v.third,
            )
        }
        CashClosingReportDto(
            parkingId = parkingId,
            fromIso = from.toString(),
            toIso = to.toString(),
            rows = outRows,
            grandTotalCents = outRows.sumOf { it.totalCents },
            grandIvaCents = outRows.sumOf { it.ivaCents },
        )
    }

    fun topPlates(parkingId: String, fromIso: String, toIso: String, limit: Int): TopPlatesReportDto = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val from = parseDate(fromIso, default = Instant.now().minusSeconds(86400 * 30))
        val to = parseDate(toIso, default = Instant.now())

        val rows = ParkingSessionsT.selectAll()
            .where {
                (ParkingSessionsT.parkingId eq parkingUuid) and
                    (ParkingSessionsT.status eq "CLOSED")
            }
            .toList()
            .filter { r ->
                val exit = r[ParkingSessionsT.exitAt] ?: return@filter false
                !exit.isBefore(from) && !exit.isAfter(to)
            }

        val grouped = rows.groupBy { it[ParkingSessionsT.plate] }
        val out = grouped.map { (plate, group) ->
            val total = group.sumOf { it[ParkingSessionsT.totalCents] ?: 0L }
            val minutes = group.sumOf { r ->
                val entry = r[ParkingSessionsT.entryAt]
                val exit = r[ParkingSessionsT.exitAt] ?: entry
                (exit.toEpochMilli() - entry.toEpochMilli()) / 60_000
            }
            TopPlateRowDto(plate = plate, sessionsCount = group.size, totalCents = total, totalMinutes = minutes)
        }.sortedByDescending { it.totalCents }.take(limit)

        TopPlatesReportDto(parkingId = parkingId, fromIso = from.toString(), toIso = to.toString(), rows = out)
    }

    fun monthlyCustomersReport(parkingId: String): MonthlyCustomersReportDto = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val now = Instant.now()
        val soon = now.plusSeconds(7 * 86400L)
        val last30 = now.minusSeconds(30 * 86400L)

        val rows = MonthlyPaymentsT.selectAll()
            .where { MonthlyPaymentsT.parkingId eq parkingUuid }
            .toList()
        val active = rows.count {
            val to = it[MonthlyPaymentsT.validTo]
            !to.isBefore(now)
        }
        val expiringSoon = rows.count {
            val to = it[MonthlyPaymentsT.validTo]
            !to.isBefore(now) && to.isBefore(soon)
        }
        val expired = rows.count { it[MonthlyPaymentsT.validTo].isBefore(now) }
        val revenue = rows.filter { it[MonthlyPaymentsT.paidAt].isAfter(last30) }
            .sumOf { it[MonthlyPaymentsT.amountCents] }

        MonthlyCustomersReportDto(
            parkingId = parkingId,
            activeCount = active,
            expiringSoonCount = expiringSoon,
            expiredCount = expired,
            revenueLast30DaysCents = revenue,
        )
    }

    /** CSV simple del reporte de cierre de caja para export. */
    fun cashClosingCsv(report: CashClosingReportDto): String {
        val sb = StringBuilder()
        sb.appendLine("day,operator,sessions,total_cents,iva_cents")
        report.rows.forEach { r ->
            sb.appendLine("${r.dayIso},${r.operatorName.replace(",", " ")},${r.sessionsCount},${r.totalCents},${r.ivaCents}")
        }
        sb.appendLine("TOTAL,,${report.rows.sumOf { it.sessionsCount }},${report.grandTotalCents},${report.grandIvaCents}")
        return sb.toString()
    }

    fun cashClosingXlsx(report: CashClosingReportDto): ByteArray {
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet("Cierre de Caja")

        val headerStyle = wb.createCellStyle().apply {
            val font = wb.createFont()
            font.bold = true
            setFont(font)
        }
        val moneyStyle = wb.createCellStyle().apply {
            dataFormat = wb.createDataFormat().getFormat("#,##0")
        }

        val headerRow = sheet.createRow(0)
        listOf("Fecha", "Operador", "Sesiones", "Total (centavos)", "IVA (centavos)").forEachIndexed { i, title ->
            headerRow.createCell(i).apply {
                setCellValue(title)
                cellStyle = headerStyle
            }
        }

        report.rows.forEachIndexed { idx, r ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(r.dayIso)
            row.createCell(1).setCellValue(r.operatorName)
            row.createCell(2).setCellValue(r.sessionsCount.toDouble())
            row.createCell(3).apply { setCellValue(r.totalCents.toDouble()); cellStyle = moneyStyle }
            row.createCell(4).apply { setCellValue(r.ivaCents.toDouble()); cellStyle = moneyStyle }
        }

        val totalRow = sheet.createRow(report.rows.size + 1)
        totalRow.createCell(0).apply { setCellValue("TOTAL"); cellStyle = headerStyle }
        totalRow.createCell(2).apply {
            setCellValue(report.rows.sumOf { it.sessionsCount }.toDouble())
            cellStyle = headerStyle
        }
        totalRow.createCell(3).apply {
            setCellValue(report.grandTotalCents.toDouble())
            cellStyle = moneyStyle
        }
        totalRow.createCell(4).apply {
            setCellValue(report.grandIvaCents.toDouble())
            cellStyle = moneyStyle
        }

        repeat(5) { sheet.autoSizeColumn(it) }

        val out = ByteArrayOutputStream()
        wb.use { it.write(out) }
        return out.toByteArray()
    }

    fun topPlatesXlsx(report: TopPlatesReportDto): ByteArray {
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet("Top Placas")

        val headerStyle = wb.createCellStyle().apply {
            val font = wb.createFont()
            font.bold = true
            setFont(font)
        }
        val moneyStyle = wb.createCellStyle().apply {
            dataFormat = wb.createDataFormat().getFormat("#,##0")
        }

        val headerRow = sheet.createRow(0)
        listOf("Placa", "Sesiones", "Total (centavos)", "Minutos totales").forEachIndexed { i, title ->
            headerRow.createCell(i).apply {
                setCellValue(title)
                cellStyle = headerStyle
            }
        }

        report.rows.forEachIndexed { idx, r ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(r.plate)
            row.createCell(1).setCellValue(r.sessionsCount.toDouble())
            row.createCell(2).apply { setCellValue(r.totalCents.toDouble()); cellStyle = moneyStyle }
            row.createCell(3).setCellValue(r.totalMinutes.toDouble())
        }

        repeat(4) { sheet.autoSizeColumn(it) }

        val out = ByteArrayOutputStream()
        wb.use { it.write(out) }
        return out.toByteArray()
    }

    private fun parseDate(iso: String, default: Instant): Instant = try {
        when {
            iso.isBlank() -> default
            iso.length == 10 -> LocalDate.parse(iso).atStartOfDay(ZoneId.of("America/Bogota")).toInstant()
            else -> Instant.parse(iso)
        }
    } catch (_: Throwable) { default }
}

