package com.parking.shared.data.api.dto

import kotlinx.serialization.Serializable

/** Resumen de cierre de caja por operador y día. */
@Serializable
data class CashClosingRowDto(
    val operatorUserId: String,
    val operatorName: String,
    val dayIso: String,
    val sessionsCount: Int,
    val totalCents: Long,
    val ivaCents: Long,
)

/** Reporte agregado de cierres de caja en un rango. */
@Serializable
data class CashClosingReportDto(
    val parkingId: String,
    val fromIso: String,
    val toIso: String,
    val rows: List<CashClosingRowDto>,
    val grandTotalCents: Long,
    val grandIvaCents: Long,
)

/** Placa con más sesiones / mayor recaudo en un rango. */
@Serializable
data class TopPlateRowDto(
    val plate: String,
    val sessionsCount: Int,
    val totalCents: Long,
    val totalMinutes: Long,
)

@Serializable
data class TopPlatesReportDto(
    val parkingId: String,
    val fromIso: String,
    val toIso: String,
    val rows: List<TopPlateRowDto>,
)

/** Reporte de mensualistas: vigentes, próximas a vencer y vencidas. */
@Serializable
data class MonthlyCustomersReportDto(
    val parkingId: String,
    val activeCount: Int,
    val expiringSoonCount: Int,    // próximas 7 días
    val expiredCount: Int,
    val revenueLast30DaysCents: Long,
)
