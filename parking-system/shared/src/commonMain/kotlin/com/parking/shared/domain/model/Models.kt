package com.parking.shared.domain.model

import kotlin.jvm.JvmInline
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/*
 * Modelos del dominio.  REGLAS:
 *   - Inmutables (data class / value class).
 *   - NO conocen Ktor ni SQLDelight (clean architecture).
 *   - Identificadores en inglés, comentarios en español.
 */

/**
 * Placa de vehículo.  Se modela como value class para evitar pasar Strings
 * sueltos por todo el código y aplicar validación centralizada.
 */
@JvmInline
@Serializable
value class PlateNumber(val value: String) {
    init {
        require(value.isNotBlank()) { "La placa no puede ser vacía." }
        require(value.length in 5..10) { "Largo de placa fuera de rango." }
    }

    /** Devuelve la placa normalizada (mayúsculas, sin espacios). */
    fun normalized(): String = value.uppercase().replace(" ", "").replace("-", "")
}

/**
 * Importe monetario en CENTAVOS.  NUNCA usamos Double / Float para dinero:
 * los errores de redondeo en cobros son contables y legales en Colombia.
 *
 * Internamente almacenamos `Long` (puede pasar de 9 billones de pesos, suficiente).
 */
@JvmInline
@Serializable
value class Money(val cents: Long) {
    operator fun plus(other: Money) = Money(cents + other.cents)
    operator fun minus(other: Money) = Money(cents - other.cents)
    operator fun times(scalar: Int): Money = Money(cents * scalar)
    operator fun times(scalar: Long): Money = Money(cents * scalar)
    operator fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    /** Aplica un porcentaje (ej.: 19 para IVA). Redondeo HALF_UP a centavo. */
    fun percent(rate: Int): Money {
        require(rate in 0..1000) { "Porcentaje fuera de rango." }
        // (cents * rate + 50) / 100   →  HALF_UP en aritmética entera
        val scaled = cents * rate
        return Money((scaled + 50) / 100)
    }

    /** Formato "$ 12.500" estilo Colombia. */
    fun formatCop(): String {
        val pesos = cents / 100
        val sb = StringBuilder()
        val str = pesos.toString()
        for ((i, c) in str.withIndex()) {
            val left = str.length - i
            sb.append(c)
            if (left > 1 && left % 3 == 1) sb.append('.')
        }
        return "$ $sb"
    }

    companion object {
        val ZERO = Money(0)
        fun ofPesos(pesos: Long): Money = Money(pesos * 100)
    }
}

/** Tipos de vehículo con tarifa diferenciada. */
@Serializable
enum class VehicleType { CAR, MOTORCYCLE, BICYCLE, TRUCK, BUS }

/** Estados de una sesión de parqueo. */
@Serializable
enum class SessionStatus { ACTIVE, CLOSED, CANCELLED, RESERVED }

/** Estados de un recibo dentro del outbox de sincronización. */
@Serializable
enum class ReceiptSyncStatus { PENDING, SYNCED, CONFLICT, FAILED }

/**
 * Tarifa aplicable a un tipo de vehículo en un rango horario / fecha.
 *
 * - `firstHourCents` cubre la primera hora completa (mínimo).
 * - `subsequentHourCents` se cobra por cada hora completa adicional.
 * - `nightSurchargePercent` se aplica si la sesión cae dentro de
 *   [nightFrom..nightTo].
 * - `graceMinutes` no se cobran si el conductor sale dentro de ese rango.
 */
@Serializable
data class Tariff(
    val id: String,
    val parkingId: String,
    val vehicleType: VehicleType,
    val firstHourCents: Long,
    val subsequentHourCents: Long,
    val nightSurchargePercent: Int = 0,
    val nightFrom: LocalTime = LocalTime(22, 0),
    val nightTo: LocalTime = LocalTime(6, 0),
    val graceMinutes: Int = 15,
    val ivaPercent: Int = 19,
    val validFrom: Instant,
    val validTo: Instant? = null,
)

/** Zona física dentro del parqueadero (A-H, etc.). */
@Serializable
data class Zone(
    val id: String,
    val parkingId: String,
    val code: String,
    val capacity: Int,
    val currentOccupancy: Int,
    val allowedVehicleTypes: List<VehicleType>,
) {
    val isFull: Boolean get() = currentOccupancy >= capacity
    val availableSlots: Int get() = (capacity - currentOccupancy).coerceAtLeast(0)
}

/** Vehículo conocido por el sistema (catálogo, no obligatorio). */
@Serializable
data class Vehicle(
    val id: String,
    val parkingId: String,
    val plate: PlateNumber,
    val type: VehicleType,
    val ownerName: String? = null,
    val ownerDocument: String? = null,
)

/** Cliente con mensualidad activa. */
@Serializable
data class Customer(
    val id: String,
    val parkingId: String,
    val fullName: String,
    val documentNumber: String,
    val email: String? = null,
    val phone: String? = null,
    val hasActiveMonthly: Boolean = false,
    val monthlyExpiresOn: Instant? = null,
)

/** Una sesión de parqueo: entrada (+ salida). */
@Serializable
data class ParkingSession(
    val id: String,
    val parkingId: String,
    val plate: PlateNumber,
    val vehicleType: VehicleType,
    val zoneId: String?,
    val entryAt: Instant,
    val exitAt: Instant? = null,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val customerId: String? = null,
    val operatorUserId: String? = null,
    val entryGate: String? = null,
)

/** Recibo emitido al cierre de una sesión. */
@Serializable
data class Receipt(
    val id: String,
    val sessionId: String,
    val parkingId: String,
    val localId: String,                 // UUID generado en el cliente (idempotency)
    val sequenceLocal: String,           // "C01-000123", prefijo por caja
    val issuedAt: Instant,
    val subtotalCents: Long,
    val ivaCents: Long,
    val totalCents: Long,
    val cufe: String? = null,            // CUFE DIAN cuando hay factura electrónica
    val syncStatus: ReceiptSyncStatus = ReceiptSyncStatus.PENDING,
    val serverId: String? = null,
) {
    val subtotal: Money get() = Money(subtotalCents)
    val iva: Money get() = Money(ivaCents)
    val total: Money get() = Money(totalCents)
}

/** Detalle de cobro: cómo se llegó al total (transparencia y auditoría). */
@Serializable
data class ChargeBreakdown(
    val billedMinutes: Long,
    val billedHours: Long,
    val baseCents: Long,
    val nightSurchargeCents: Long,
    val subtotalCents: Long,
    val ivaCents: Long,
    val totalCents: Long,
    val appliedMonthly: Boolean,
    val withinGrace: Boolean,
) {
    val total: Money get() = Money(totalCents)
}
