package com.parking.shared.data.api.dto

import com.parking.shared.domain.model.VehicleType
import kotlinx.serialization.Serializable

@Serializable
data class TariffDto(
    val id: String,
    val parkingId: String,
    val vehicleType: VehicleType,
    val firstHourCents: Long,
    val subsequentHourCents: Long,
    val nightSurchargePercent: Int,
    val nightFromIso: String,        // "22:00:00"
    val nightToIso: String,          // "06:00:00"
    val graceMinutes: Int,
    val ivaPercent: Int,
    val validFromMillis: Long,
    val validToMillis: Long?,
)

@Serializable
data class ZoneDto(
    val id: String,
    val parkingId: String,
    val code: String,
    val capacity: Int,
    val currentOccupancy: Int,
    val allowedVehicleTypes: List<VehicleType>,
    // V2 — extensiones del módulo Admin.  Defaults a 0/true/0/null para que
    // la deserialización siga funcionando con respuestas legacy.
    val underMaintenance: Int = 0,
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    val notes: String? = null,
)

/** Body para crear/editar una zona desde el módulo Admin. */
@Serializable
data class UpsertZoneRequest(
    val code: String,
    val capacity: Int,
    val allowedVehicleTypes: List<VehicleType>,
    val underMaintenance: Int = 0,
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    val notes: String? = null,
)
