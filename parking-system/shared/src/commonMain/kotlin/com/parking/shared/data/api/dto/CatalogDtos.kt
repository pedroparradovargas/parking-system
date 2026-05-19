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
)
