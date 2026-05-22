package com.parking.backend.routes

import com.parking.shared.data.api.dto.TariffDto
import com.parking.shared.data.api.dto.UpsertTariffRequest
import com.parking.shared.domain.model.VehicleType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

/**
 * Tabla `tariffs` de la migración V1.  Se mantiene aquí para no acoplar el
 * acceso a la columna `created_at` (que `SyncRepository` no necesita y por
 * tanto no expone).
 */
object AdminTariffsT : Table("tariffs") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val vehicleType = varchar("vehicle_type", 20)
    val firstHourCents = long("first_hour_cents")
    val subsequentHourCents = long("subsequent_hour_cents")
    val nightSurchargePercent = integer("night_surcharge_percent")
    val nightFrom = time("night_from")
    val nightTo = time("night_to")
    val graceMinutes = integer("grace_minutes")
    val ivaPercent = integer("iva_percent")
    val validFrom = timestamp("valid_from")
    val validTo = timestamp("valid_to").nullable()
    val createdAt = timestamp("created_at")
}

/**
 * Repositorio de gestión de tarifas (vigentes + históricas).
 *
 * Política de versionado:
 *   - Una tarifa nunca se modifica in-place.  `update()` cierra `valid_to`
 *     de la versión vigente y crea una nueva fila con `valid_from = ahora`
 *     (o el `validFromMillis` indicado por el caller si es futuro).
 *   - `close()` simplemente pone `valid_to = ahora` (no crea nueva versión).
 *
 * Esto preserva la historia para auditoría DIAN y para recalcular cobros
 * pasados sin sorpresas.
 */
object AdminTariffRepository {

    /**
     * Lista las tarifas del parking.
     *  - `historic=true`: devuelve todas (vigentes + cerradas + futuras).
     *  - `historic=false`: solo las que aún no fueron cerradas (`valid_to IS NULL`).
     *
     * Nota: la semántica "vigente AHORA" (comparación con `Instant.now()`)
     * la decide el cliente, no el repo.  Esto evita falsos positivos por
     * imprecisión de timestamp entre el cierre y la siguiente lectura.
     */
    fun list(parkingId: String, historic: Boolean): List<TariffDto> = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        AdminTariffsT.selectAll()
            .where {
                if (historic) {
                    AdminTariffsT.parkingId eq parkingUuid
                } else {
                    (AdminTariffsT.parkingId eq parkingUuid) and AdminTariffsT.validTo.isNull()
                }
            }
            .orderBy(AdminTariffsT.vehicleType to SortOrder.ASC, AdminTariffsT.validFrom to SortOrder.DESC)
            .map { it.toDto() }
    }

    /**
     * Crea una tarifa nueva.  Si ya existe una vigente para el mismo
     * vehicleType, la cierra con `valid_to = nueva.validFrom`.
     */
    fun create(parkingId: String, req: UpsertTariffRequest): TariffDto = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val validFrom = req.validFromMillis?.let(Instant::ofEpochMilli) ?: Instant.now()
        val validTo = req.validToMillis?.let(Instant::ofEpochMilli)
        validateRequest(req, validFrom, validTo)

        // Cierra la vigente del mismo tipo, si existe.
        closeOpenVersion(parkingUuid, req.vehicleType.name, validFrom)

        val newId = UUID.randomUUID()
        val nightFromLt = parseLocalTime(req.nightFromIso)
        val nightToLt = parseLocalTime(req.nightToIso)
        AdminTariffsT.insert {
            it[id] = newId
            it[AdminTariffsT.parkingId] = parkingUuid
            it[vehicleType] = req.vehicleType.name
            it[firstHourCents] = req.firstHourCents
            it[subsequentHourCents] = req.subsequentHourCents
            it[nightSurchargePercent] = req.nightSurchargePercent
            it[nightFrom] = nightFromLt
            it[nightTo] = nightToLt
            it[graceMinutes] = req.graceMinutes
            it[ivaPercent] = req.ivaPercent
            it[AdminTariffsT.validFrom] = validFrom
            it[AdminTariffsT.validTo] = validTo
            it[createdAt] = Instant.now()
        }
        AdminTariffsT.selectAll().where { AdminTariffsT.id eq newId }.single().toDto()
    }

    /**
     * Edita una tarifa = crea una nueva versión con los nuevos campos y cierra
     * la versión [tariffId] poniendo `valid_to = nueva.validFrom`.  No
     * sobrescribe el registro existente.  Devuelve la nueva tarifa.
     */
    fun update(parkingId: String, tariffId: String, req: UpsertTariffRequest): TariffDto = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val targetUuid = UUID.fromString(tariffId)
        val validFrom = req.validFromMillis?.let(Instant::ofEpochMilli) ?: Instant.now()
        val validTo = req.validToMillis?.let(Instant::ofEpochMilli)
        validateRequest(req, validFrom, validTo)

        // Confirma que la tarifa pertenece al parking del caller (multi-tenant).
        val target = AdminTariffsT.selectAll()
            .where { (AdminTariffsT.id eq targetUuid) and (AdminTariffsT.parkingId eq parkingUuid) }
            .singleOrNull() ?: error("tariff_not_found")

        // Cierra la versión apuntada — sea exactamente la que el caller indicó.
        AdminTariffsT.update({ AdminTariffsT.id eq targetUuid }) {
            it[AdminTariffsT.validTo] = validFrom
        }
        // ALSO cierra cualquier otra vigente del mismo tipo (defensa contra
        // estados inconsistentes pre-migración).
        if (target[AdminTariffsT.vehicleType] != req.vehicleType.name) {
            closeOpenVersion(parkingUuid, req.vehicleType.name, validFrom)
        }

        // Crea la nueva versión usando exactamente el mismo `validFrom` con que
        // se cerró la anterior — esto garantiza el invariante:
        // `oldTariff.validTo == newTariff.validFrom`.
        create(parkingId, req.copy(validFromMillis = validFrom.toEpochMilli()))
    }

    /** Cierra anticipadamente la tarifa (sin crear nueva versión). */
    fun close(parkingId: String, tariffId: String): Int = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val targetUuid = UUID.fromString(tariffId)
        val now = Instant.now()
        AdminTariffsT.update({
            (AdminTariffsT.id eq targetUuid) and (AdminTariffsT.parkingId eq parkingUuid)
        }) {
            it[AdminTariffsT.validTo] = now
        }
    }

    // ─── Helpers internos ──────────────────────────────────────────────────

    private fun closeOpenVersion(parkingUuid: UUID, vehicleType: String, closeAt: Instant) {
        AdminTariffsT.update({
            (AdminTariffsT.parkingId eq parkingUuid) and
                (AdminTariffsT.vehicleType eq vehicleType) and
                (AdminTariffsT.validFrom less closeAt) and
                (AdminTariffsT.validTo.isNull() or (AdminTariffsT.validTo greaterEq closeAt))
        }) {
            it[AdminTariffsT.validTo] = closeAt
        }
    }

    private fun validateRequest(req: UpsertTariffRequest, validFrom: Instant, validTo: Instant?) {
        require(req.firstHourCents >= 0) { "first_hour_cents must be non-negative" }
        require(req.subsequentHourCents >= 0) { "subsequent_hour_cents must be non-negative" }
        require(req.nightSurchargePercent in 0..100) { "night_surcharge_percent out of range" }
        require(req.graceMinutes in 0..120) { "grace_minutes out of range" }
        require(req.ivaPercent in 0..100) { "iva_percent out of range" }
        if (validTo != null) require(validTo.isAfter(validFrom)) { "valid_to must be after valid_from" }
    }

    private fun parseLocalTime(iso: String): LocalTime =
        if (iso.length == 5) LocalTime.parse("$iso:00") else LocalTime.parse(iso)

    private fun org.jetbrains.exposed.sql.ResultRow.toDto(): TariffDto = TariffDto(
        id = this[AdminTariffsT.id].toString(),
        parkingId = this[AdminTariffsT.parkingId].toString(),
        vehicleType = VehicleType.valueOf(this[AdminTariffsT.vehicleType]),
        firstHourCents = this[AdminTariffsT.firstHourCents],
        subsequentHourCents = this[AdminTariffsT.subsequentHourCents],
        nightSurchargePercent = this[AdminTariffsT.nightSurchargePercent],
        nightFromIso = this[AdminTariffsT.nightFrom].toString(),
        nightToIso = this[AdminTariffsT.nightTo].toString(),
        graceMinutes = this[AdminTariffsT.graceMinutes],
        ivaPercent = this[AdminTariffsT.ivaPercent],
        validFromMillis = this[AdminTariffsT.validFrom].toEpochMilli(),
        validToMillis = this[AdminTariffsT.validTo]?.toEpochMilli(),
    )
}
