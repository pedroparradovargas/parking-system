package com.parking.shared.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RevenueReportDto(
    val parkingId: String,
    val fromIso: String,
    val toIso: String,
    val totalRevenueCents: Long,
    val totalIvaCents: Long,
    val sessionsCount: Int,
    val byVehicleType: Map<String, Long>,    // tipo → cents
    val byDay: List<RevenueBucket>,
)

@Serializable
data class RevenueBucket(val dayIso: String, val revenueCents: Long, val sessions: Int)

@Serializable
data class OccupancyReportDto(
    val parkingId: String,
    val capacityTotal: Int,
    val currentOccupancy: Int,
    val byZone: List<ZoneOccupancy>,
    val percentage: Double,
)

@Serializable
data class ZoneOccupancy(val zoneCode: String, val capacity: Int, val occupancy: Int)
