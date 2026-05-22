package com.parking.shared.data.api.dto

import kotlinx.serialization.Serializable

/** Configuración del parqueadero (datos fiscales, horarios, conexiones externas). */
@Serializable
data class ParkingConfigDto(
    val parkingId: String,
    val legalName: String? = null,
    val taxId: String? = null,
    val legalAddress: String? = null,
    val city: String? = null,
    val dianResolution: String? = null,
    val dianResolutionFromIso: String? = null,
    val dianResolutionToIso: String? = null,
    val invoiceSeries: String? = null,
    val invoiceNextSequence: Long = 1,
    val timezone: String = "America/Bogota",
    val operatingMode: String = "24x7",       // 24x7 | CUSTOM
    val operatingScheduleJson: String? = null, // {"mon":[{"from":"08:00","to":"22:00"}], ...}
    val aiServiceUrl: String? = null,
    val notificationEmail: String? = null,
    val updatedAtMillis: Long,
)

/** Body para actualizar la configuración. */
@Serializable
data class UpsertParkingConfigRequest(
    val legalName: String? = null,
    val taxId: String? = null,
    val legalAddress: String? = null,
    val city: String? = null,
    val dianResolution: String? = null,
    val dianResolutionFromIso: String? = null,
    val dianResolutionToIso: String? = null,
    val invoiceSeries: String? = null,
    val invoiceNextSequence: Long? = null,
    val timezone: String? = null,
    val operatingMode: String? = null,
    val operatingScheduleJson: String? = null,
    val aiServiceUrl: String? = null,
    val notificationEmail: String? = null,
)

/** Entrada de auditoría leída desde el servidor. */
@Serializable
data class AuditEntryRowDto(
    val id: String,
    val parkingId: String,
    val tsEpochMillis: Long,
    val action: String,
    val entity: String,
    val entityId: String,
    val actorUserId: String? = null,
    val payloadJson: String,
    val prevHash: String? = null,
    val currentHash: String,
)

/** Resultado de verificar la cadena hash. */
@Serializable
data class AuditVerifyResponse(
    val parkingId: String,
    val checked: Int,
    val corrupted: List<String>,  // IDs corruptos; vacía → cadena íntegra
)
