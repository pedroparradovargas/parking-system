package com.parking.shared.data.api.dto

import com.parking.shared.domain.model.VehicleType
import kotlinx.serialization.Serializable

/** Vehículo asociado a un cliente (placa + tipo). */
@Serializable
data class CustomerVehicleDto(
    val id: String,
    val plate: String,
    val vehicleType: VehicleType,
    val isPrimary: Boolean = false,
)

/** Vista admin de un cliente con sus vehículos + flag de mensualidad vigente. */
@Serializable
data class AdminCustomerDto(
    val id: String,
    val parkingId: String,
    val fullName: String,
    val documentNumber: String,
    val email: String?,
    val phone: String?,
    val hasActiveMonthly: Boolean,
    val monthlyExpiresAtMillis: Long?,
    val vehicles: List<CustomerVehicleDto> = emptyList(),
)

/** Crear o editar un cliente.  Las placas se reemplazan por completo en update. */
@Serializable
data class UpsertCustomerRequest(
    val fullName: String,
    val documentNumber: String,
    val email: String? = null,
    val phone: String? = null,
    val vehicles: List<CustomerVehicleDto> = emptyList(),
)

/** Body para asignar/renovar una mensualidad. */
@Serializable
data class AssignMonthlyRequest(
    val planId: String,                 // referencia a tariff_plans
    val paymentMethod: String,          // CASH | CARD | TRANSFER
    val amountPaidCents: Long,
    val startFromMillis: Long? = null,  // null = ahora o continúa al vencimiento actual
)

/** Mensualidad registrada para un cliente. */
@Serializable
data class MonthlyPaymentDto(
    val id: String,
    val customerId: String,
    val planName: String? = null,
    val amountCents: Long,
    val paymentMethod: String,
    val validFromMillis: Long,
    val validToMillis: Long,
    val paidAtMillis: Long,
)
