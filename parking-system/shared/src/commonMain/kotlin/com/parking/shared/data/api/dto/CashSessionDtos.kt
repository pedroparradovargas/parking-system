package com.parking.shared.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CashSessionDto(
    val id: String,
    val parkingId: String,
    val operatorUserId: String,
    val operatorName: String? = null,
    val openedAtMillis: Long,
    val closedAtMillis: Long? = null,
    val totalCashCents: Long = 0,
    val totalCardCents: Long = 0,
    val totalOtherCents: Long = 0,
    val sessionsCount: Int = 0,
    val notes: String? = null,
) {
    val totalCents: Long get() = totalCashCents + totalCardCents + totalOtherCents
}

@Serializable
data class OpenCashSessionRequest(
    val notes: String? = null,
)

@Serializable
data class CloseCashSessionRequest(
    val notes: String? = null,
)
