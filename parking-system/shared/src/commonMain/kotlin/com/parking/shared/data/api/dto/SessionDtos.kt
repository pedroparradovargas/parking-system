package com.parking.shared.data.api.dto

import com.parking.shared.domain.model.SessionStatus
import com.parking.shared.domain.model.VehicleType
import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    val id: String,
    val localId: String,                 // idempotency key generada en cliente
    val parkingId: String,
    val plate: String,
    val vehicleType: VehicleType,
    val zoneId: String? = null,
    val entryAtMillis: Long,
    val exitAtMillis: Long? = null,
    val status: SessionStatus,
    val operatorUserId: String? = null,
    val entryGate: String? = null,
    val totalCents: Long? = null,
    val ivaCents: Long? = null,
)

@Serializable
data class ReceiptDto(
    val id: String,
    val sessionId: String,
    val localId: String,
    val parkingId: String,
    val sequenceLocal: String,
    val issuedAtMillis: Long,
    val subtotalCents: Long,
    val ivaCents: Long,
    val totalCents: Long,
    val cufe: String? = null,
)
