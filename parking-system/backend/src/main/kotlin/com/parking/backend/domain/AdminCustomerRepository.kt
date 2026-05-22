package com.parking.backend.routes

import com.parking.shared.data.api.dto.AdminCustomerDto
import com.parking.shared.data.api.dto.AssignMonthlyRequest
import com.parking.shared.data.api.dto.CustomerVehicleDto
import com.parking.shared.data.api.dto.MonthlyPaymentDto
import com.parking.shared.data.api.dto.UpsertCustomerRequest
import com.parking.shared.domain.model.VehicleType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

object CustomersT : Table("customers") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val fullName = varchar("full_name", 255)
    val documentNumber = varchar("document_number", 64)
    val email = varchar("email", 200).nullable()
    val phone = varchar("phone", 50).nullable()
    val createdAt = timestamp("created_at")
}

object CustomerVehiclesT : Table("customer_vehicles") {
    val id = uuid("id")
    val customerId = uuid("customer_id")
    val plate = varchar("plate", 16)
    val vehicleType = varchar("vehicle_type", 20)
    val isPrimary = bool("is_primary")
    val createdAt = timestamp("created_at")
}

object MonthlyPaymentsT : Table("monthly_payments") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val customerId = uuid("customer_id")
    val planId = uuid("plan_id").nullable()
    val amountCents = long("amount_cents")
    val paymentMethod = varchar("payment_method", 20)
    val validFrom = timestamp("valid_from")
    val validTo = timestamp("valid_to")
    val paidAt = timestamp("paid_at")
}

/**
 * Repositorio de clientes (mensualistas) y sus mensualidades.
 *
 *  - Un cliente puede tener N vehículos asociados (placas).
 *  - `hasActiveMonthly` se deriva de `monthly_payments` con `valid_to > now`.
 *  - Asignar plan registra un nuevo `monthly_payments`; renovar concatena
 *    vigencia (si hay una activa, la nueva empieza al expirar la actual).
 */
object AdminCustomerRepository {

    fun list(parkingId: String): List<AdminCustomerDto> = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val now = Instant.now()
        CustomersT.selectAll()
            .where { CustomersT.parkingId eq parkingUuid }
            .orderBy(CustomersT.fullName to SortOrder.ASC)
            .map { row ->
                val customerId = row[CustomersT.id]
                val vehicles = CustomerVehiclesT.selectAll()
                    .where { CustomerVehiclesT.customerId eq customerId }
                    .map { v ->
                        CustomerVehicleDto(
                            id = v[CustomerVehiclesT.id].toString(),
                            plate = v[CustomerVehiclesT.plate],
                            vehicleType = VehicleType.valueOf(v[CustomerVehiclesT.vehicleType]),
                            isPrimary = v[CustomerVehiclesT.isPrimary],
                        )
                    }
                val active = MonthlyPaymentsT.selectAll()
                    .where {
                        (MonthlyPaymentsT.customerId eq customerId) and
                            (MonthlyPaymentsT.validTo greaterEq now)
                    }
                    .orderBy(MonthlyPaymentsT.validTo to SortOrder.DESC)
                    .firstOrNull()
                row.toDto(vehicles, active)
            }
    }

    fun create(parkingId: String, req: UpsertCustomerRequest): AdminCustomerDto = transaction {
        validate(req)
        val parkingUuid = UUID.fromString(parkingId)
        val newId = UUID.randomUUID()
        CustomersT.insert {
            it[id] = newId
            it[CustomersT.parkingId] = parkingUuid
            it[fullName] = req.fullName
            it[documentNumber] = req.documentNumber
            it[email] = req.email
            it[phone] = req.phone
            it[createdAt] = Instant.now()
        }
        replaceVehicles(newId, req.vehicles)
        loadOne(newId) ?: error("customer_creation_failed")
    }

    fun update(parkingId: String, customerId: String, req: UpsertCustomerRequest): AdminCustomerDto = transaction {
        validate(req)
        val parkingUuid = UUID.fromString(parkingId)
        val targetUuid = UUID.fromString(customerId)
        val updated = CustomersT.update({
            (CustomersT.id eq targetUuid) and (CustomersT.parkingId eq parkingUuid)
        }) {
            it[fullName] = req.fullName
            it[documentNumber] = req.documentNumber
            it[email] = req.email
            it[phone] = req.phone
        }
        if (updated == 0) error("customer_not_found")
        replaceVehicles(targetUuid, req.vehicles)
        loadOne(targetUuid) ?: error("customer_not_found")
    }

    fun assignMonthly(parkingId: String, customerId: String, req: AssignMonthlyRequest, planDurationDays: Int, planPriceCents: Long): MonthlyPaymentDto = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val customerUuid = UUID.fromString(customerId)
        val now = Instant.now()

        // Si hay mensualidad activa, la nueva arranca al expirar la actual.
        val activeNow = MonthlyPaymentsT.selectAll()
            .where {
                (MonthlyPaymentsT.customerId eq customerUuid) and
                    (MonthlyPaymentsT.validTo greaterEq now)
            }
            .orderBy(MonthlyPaymentsT.validTo to SortOrder.DESC)
            .firstOrNull()
        val from = req.startFromMillis?.let(Instant::ofEpochMilli)
            ?: activeNow?.get(MonthlyPaymentsT.validTo)
            ?: now
        val to = from.plusSeconds(planDurationDays.toLong() * 86400L)

        val newId = UUID.randomUUID()
        MonthlyPaymentsT.insert {
            it[id] = newId
            it[MonthlyPaymentsT.parkingId] = parkingUuid
            it[MonthlyPaymentsT.customerId] = customerUuid
            it[planId] = UUID.fromString(req.planId)
            it[amountCents] = req.amountPaidCents.takeIf { it > 0 } ?: planPriceCents
            it[paymentMethod] = req.paymentMethod
            it[validFrom] = from
            it[validTo] = to
            it[paidAt] = now
        }
        MonthlyPaymentsT.selectAll().where { MonthlyPaymentsT.id eq newId }.single()
            .toMonthlyDto(null)
    }

    fun expiringSoon(parkingId: String, withinDays: Int = 7): List<AdminCustomerDto> = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val now = Instant.now()
        val limit = now.plusSeconds(withinDays.toLong() * 86400L)
        val customerIds = MonthlyPaymentsT.selectAll()
            .where {
                (MonthlyPaymentsT.parkingId eq parkingUuid) and
                    (MonthlyPaymentsT.validTo greaterEq now) and
                    (MonthlyPaymentsT.validTo lessEq limit)
            }
            .map { it[MonthlyPaymentsT.customerId] }
            .toSet()
        list(parkingId).filter { UUID.fromString(it.id) in customerIds }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private fun validate(req: UpsertCustomerRequest) {
        require(req.fullName.isNotBlank()) { "full_name required" }
        require(req.documentNumber.isNotBlank()) { "document_number required" }
        req.vehicles.forEach { v ->
            require(v.plate.length in 4..16) { "plate length out of range" }
        }
    }

    private fun replaceVehicles(customerId: UUID, vehicles: List<CustomerVehicleDto>) {
        CustomerVehiclesT.deleteWhere { CustomerVehiclesT.customerId eq customerId }
        val now = Instant.now()
        vehicles.forEach { v ->
            CustomerVehiclesT.insert {
                it[id] = UUID.randomUUID()
                it[CustomerVehiclesT.customerId] = customerId
                it[plate] = v.plate.uppercase()
                it[vehicleType] = v.vehicleType.name
                it[isPrimary] = v.isPrimary
                it[createdAt] = now
            }
        }
    }

    private fun loadOne(customerId: UUID): AdminCustomerDto? {
        val row = CustomersT.selectAll().where { CustomersT.id eq customerId }.singleOrNull() ?: return null
        val vehicles = CustomerVehiclesT.selectAll()
            .where { CustomerVehiclesT.customerId eq customerId }
            .map {
                CustomerVehicleDto(
                    id = it[CustomerVehiclesT.id].toString(),
                    plate = it[CustomerVehiclesT.plate],
                    vehicleType = VehicleType.valueOf(it[CustomerVehiclesT.vehicleType]),
                    isPrimary = it[CustomerVehiclesT.isPrimary],
                )
            }
        val active = MonthlyPaymentsT.selectAll()
            .where {
                (MonthlyPaymentsT.customerId eq customerId) and
                    (MonthlyPaymentsT.validTo greaterEq Instant.now())
            }
            .orderBy(MonthlyPaymentsT.validTo to SortOrder.DESC)
            .firstOrNull()
        return row.toDto(vehicles, active)
    }

    private fun ResultRow.toDto(vehicles: List<CustomerVehicleDto>, activeRow: ResultRow?): AdminCustomerDto =
        AdminCustomerDto(
            id = this[CustomersT.id].toString(),
            parkingId = this[CustomersT.parkingId].toString(),
            fullName = this[CustomersT.fullName],
            documentNumber = this[CustomersT.documentNumber],
            email = this[CustomersT.email],
            phone = this[CustomersT.phone],
            hasActiveMonthly = activeRow != null,
            monthlyExpiresAtMillis = activeRow?.get(MonthlyPaymentsT.validTo)?.toEpochMilli(),
            vehicles = vehicles,
        )

    private fun ResultRow.toMonthlyDto(planName: String?): MonthlyPaymentDto = MonthlyPaymentDto(
        id = this[MonthlyPaymentsT.id].toString(),
        customerId = this[MonthlyPaymentsT.customerId].toString(),
        planName = planName,
        amountCents = this[MonthlyPaymentsT.amountCents],
        paymentMethod = this[MonthlyPaymentsT.paymentMethod],
        validFromMillis = this[MonthlyPaymentsT.validFrom].toEpochMilli(),
        validToMillis = this[MonthlyPaymentsT.validTo].toEpochMilli(),
        paidAtMillis = this[MonthlyPaymentsT.paidAt].toEpochMilli(),
    )
}

// Operadores Exposed para comparar Instant nullable.
private infix fun org.jetbrains.exposed.sql.Column<java.time.Instant>.greaterEq(value: java.time.Instant) =
    org.jetbrains.exposed.sql.GreaterEqOp(this, org.jetbrains.exposed.sql.QueryParameter(value, this.columnType))

private infix fun org.jetbrains.exposed.sql.Column<java.time.Instant>.lessEq(value: java.time.Instant) =
    org.jetbrains.exposed.sql.LessEqOp(this, org.jetbrains.exposed.sql.QueryParameter(value, this.columnType))
