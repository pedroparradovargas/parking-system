package com.parking.shared.data.api.dto

import com.parking.shared.domain.model.VehicleType
import kotlinx.serialization.Serializable

/**
 * Petición para crear/editar una tarifa.  Al editar, el backend cierra la
 * versión vigente y crea una nueva con `validFromMillis = ahora` (o el valor
 * indicado si es futuro).  Política de versionado: NUNCA se sobrescribe.
 */
@Serializable
data class UpsertTariffRequest(
    val vehicleType: VehicleType,
    val firstHourCents: Long,
    val subsequentHourCents: Long,
    val nightSurchargePercent: Int,
    val nightFromIso: String,        // "22:00" o "22:00:00"
    val nightToIso: String,          // "06:00"
    val graceMinutes: Int,
    val ivaPercent: Int,
    val validFromMillis: Long? = null,   // null => ahora
    val validToMillis: Long? = null,     // null => indefinido
)

/** Plan de mensualidad (mensual / trimestral / semestral / anual / custom). */
@Serializable
data class TariffPlanDto(
    val id: String,
    val parkingId: String,
    val name: String,
    val durationDays: Int,
    val priceCents: Long,
    val vehicleType: VehicleType,
    val enabled: Boolean,
    val createdAtMillis: Long,
)

/** Body para crear/editar un plan (parking_id + id vienen del path / generados). */
@Serializable
data class UpsertTariffPlanRequest(
    val name: String,
    val durationDays: Int,
    val priceCents: Long,
    val vehicleType: VehicleType,
    val enabled: Boolean = true,
)

/** Reglas de aplicación de una tarifa especial. */
@Serializable
enum class SpecialTariffRule { WEEKEND, HOLIDAY, DATE_RANGE, DAY_OF_WEEK }

/** Multiplicador aplicado al cobro base bajo una regla específica. */
@Serializable
data class SpecialTariffDto(
    val id: String,
    val parkingId: String,
    val name: String,
    val ruleType: SpecialTariffRule,
    val multiplier: Double,
    val dateFromIso: String? = null,  // "2026-12-25"
    val dateToIso: String? = null,
    val dayOfWeekCsv: String? = null, // "1,2,3,4,5" (1 = lunes)
    val enabled: Boolean,
    val createdAtMillis: Long,
)

/** Body para crear/editar una tarifa especial. */
@Serializable
data class UpsertSpecialTariffRequest(
    val name: String,
    val ruleType: SpecialTariffRule,
    val multiplier: Double,
    val dateFromIso: String? = null,
    val dateToIso: String? = null,
    val dayOfWeekCsv: String? = null,
    val enabled: Boolean = true,
)

/** Festivo editable por parking. */
@Serializable
data class HolidayDto(
    val id: String,
    val parkingId: String,
    val dateIso: String,    // "2026-12-25"
    val name: String,
    val createdAtMillis: Long,
)

@Serializable
data class UpsertHolidayRequest(
    val dateIso: String,
    val name: String,
)
