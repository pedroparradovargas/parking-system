package com.parking.shared.data.api.dto

import kotlinx.serialization.Serializable

/** Envoltorio del outbox del cliente hacia el servidor. */
@Serializable
data class SyncPushRequest(
    val deviceId: String,
    val parkingId: String,
    val sessions: List<SessionDto> = emptyList(),
    val receipts: List<ReceiptDto> = emptyList(),
)

/** Resultado por cada `localId` enviado. */
@Serializable
data class SyncPushResponse(
    val accepted: List<SyncAccepted>,
    val conflicts: List<SyncConflict>,
    val rejected: List<SyncRejected>,
)

@Serializable
data class SyncAccepted(val localId: String, val serverId: String)

@Serializable
data class SyncConflict(val localId: String, val reason: String, val serverState: SessionDto?)

@Serializable
data class SyncRejected(val localId: String, val errorCode: String, val message: String)

/** Cambios del servidor hacia el cliente desde un timestamp. */
@Serializable
data class SyncPullResponse(
    val sinceMillis: Long,
    val nowMillis: Long,
    val tariffs: List<TariffDto> = emptyList(),
    val zones: List<ZoneDto> = emptyList(),
    val customersChanged: Int = 0,
)
