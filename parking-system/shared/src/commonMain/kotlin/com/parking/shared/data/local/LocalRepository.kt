package com.parking.shared.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.parking.shared.db.ParkingDatabase
import com.parking.shared.domain.model.Customer
import com.parking.shared.domain.model.ParkingSession
import com.parking.shared.domain.model.PlateNumber
import com.parking.shared.domain.model.Receipt
import com.parking.shared.domain.model.ReceiptSyncStatus
import com.parking.shared.domain.model.SessionStatus
import com.parking.shared.domain.model.Tariff
import com.parking.shared.domain.model.VehicleType
import com.parking.shared.domain.model.Zone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime

/**
 * Facade único sobre SQLDelight para todas las operaciones offline-first.
 *
 * Toda lectura devuelve Flow para reaccionar a cambios.  Toda escritura
 * encola en `receipts_queue` cuando aplica.
 *
 * Las apps usan ESTE repositorio como fuente primaria; el SyncManager
 * decide cuándo empujar al servidor.
 */
class LocalRepository(private val db: ParkingDatabase) {

    // ---- Tarifas ----
    fun watchTariffs(parkingId: String): Flow<List<Tariff>> =
        db.tariffsQueries.selectByParking(parkingId)
            .asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun upsertTariffs(tariffs: List<Tariff>) {
        db.transaction {
            tariffs.forEach { t ->
                db.tariffsQueries.upsert(
                    id = t.id,
                    parking_id = t.parkingId,
                    vehicle_type = t.vehicleType.name,
                    first_hour_cents = t.firstHourCents,
                    subsequent_hour_cents = t.subsequentHourCents,
                    night_surcharge_percent = t.nightSurchargePercent.toLong(),
                    night_from = t.nightFrom.toString(),
                    night_to = t.nightTo.toString(),
                    grace_minutes = t.graceMinutes.toLong(),
                    iva_percent = t.ivaPercent.toLong(),
                    valid_from_ms = t.validFrom.toEpochMilliseconds(),
                    valid_to_ms = t.validTo?.toEpochMilliseconds(),
                )
            }
        }
    }

    // ---- Zonas ----
    fun watchZones(parkingId: String): Flow<List<Zone>> =
        db.zonesQueries.selectByParking(parkingId)
            .asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun upsertZones(zones: List<Zone>) {
        db.transaction {
            zones.forEach { z ->
                db.zonesQueries.upsert(
                    id = z.id,
                    parking_id = z.parkingId,
                    code = z.code,
                    capacity = z.capacity.toLong(),
                    current_occupancy = z.currentOccupancy.toLong(),
                    allowed_types_csv = z.allowedVehicleTypes.joinToString(",") { it.name },
                )
            }
        }
    }

    // ---- Sesiones ----
    suspend fun insertSession(s: ParkingSession) {
        db.sessionsQueries.insert(
            id = s.id,
            parking_id = s.parkingId,
            plate = s.plate.normalized(),
            vehicle_type = s.vehicleType.name,
            zone_id = s.zoneId,
            entry_at_ms = s.entryAt.toEpochMilliseconds(),
            exit_at_ms = s.exitAt?.toEpochMilliseconds(),
            status = s.status.name,
            customer_id = s.customerId,
            operator_user_id = s.operatorUserId,
            entry_gate = s.entryGate,
        )
    }

    suspend fun closeSession(sessionId: String, exitAt: Instant) {
        db.sessionsQueries.close(
            exit_at_ms = exitAt.toEpochMilliseconds(),
            status = SessionStatus.CLOSED.name,
            id = sessionId,
        )
    }

    fun watchActiveSessions(parkingId: String): Flow<List<ParkingSession>> =
        db.sessionsQueries.selectActive(parkingId)
            .asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomainSession() } }

    suspend fun findActiveByPlate(parkingId: String, plate: PlateNumber): ParkingSession? =
        db.sessionsQueries.selectActiveByPlate(parkingId, plate.normalized())
            .executeAsOneOrNull()?.toDomainSession()

    // ---- Recibos (outbox) ----
    suspend fun insertReceipt(r: Receipt) {
        db.receiptsQueries.insert(
            id = r.id,
            session_id = r.sessionId,
            parking_id = r.parkingId,
            local_id = r.localId,
            sequence_local = r.sequenceLocal,
            issued_at_ms = r.issuedAt.toEpochMilliseconds(),
            subtotal_cents = r.subtotalCents,
            iva_cents = r.ivaCents,
            total_cents = r.totalCents,
            cufe = r.cufe,
            sync_status = r.syncStatus.name,
            server_id = r.serverId,
        )
    }

    suspend fun pendingReceipts(parkingId: String): List<Receipt> =
        db.receiptsQueries.selectPending(parkingId, ReceiptSyncStatus.PENDING.name)
            .executeAsList().map { it.toDomain() }

    suspend fun markReceiptSynced(localId: String, serverId: String) {
        db.receiptsQueries.markSynced(serverId, ReceiptSyncStatus.SYNCED.name, localId)
    }

    suspend fun markReceiptConflict(localId: String, reason: String) {
        db.receiptsQueries.markConflict(reason, ReceiptSyncStatus.CONFLICT.name, localId)
    }

    // ---- Clientes ----
    suspend fun upsertCustomers(customers: List<Customer>) {
        db.transaction {
            customers.forEach { c ->
                db.customersQueries.upsert(
                    id = c.id,
                    parking_id = c.parkingId,
                    full_name = c.fullName,
                    document_number = c.documentNumber,
                    email = c.email,
                    phone = c.phone,
                    has_active_monthly = if (c.hasActiveMonthly) 1L else 0L,
                    monthly_expires_ms = c.monthlyExpiresOn?.toEpochMilliseconds(),
                )
            }
        }
    }

    suspend fun findCustomerByDocument(parkingId: String, document: String): Customer? =
        db.customersQueries.selectByDocument(parkingId, document)
            .executeAsOneOrNull()?.toDomain()

    // ---- Auditoría ----
    suspend fun appendAuditLog(action: String, entity: String, entityId: String, payload: String, previousHash: String?): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val toHash = "${previousHash.orEmpty()}|$now|$action|$entity|$entityId|$payload"
        val currentHash = simpleSha256(toHash)
        db.auditQueries.insert(
            id = "audit-$now-${entityId.take(6)}",
            ts_ms = now,
            action = action,
            entity = entity,
            entity_id = entityId,
            payload = payload,
            prev_hash = previousHash,
            current_hash = currentHash,
        )
        return currentHash
    }

    suspend fun lastAuditHash(): String? =
        db.auditQueries.selectLast().executeAsOneOrNull()?.current_hash
}

// ---- Helpers de mapeo SQLDelight → dominio (mantienen el dominio limpio) ----
private fun com.parking.shared.db.Tariffs.toDomain(): Tariff = Tariff(
    id = id,
    parkingId = parking_id,
    vehicleType = VehicleType.valueOf(vehicle_type),
    firstHourCents = first_hour_cents,
    subsequentHourCents = subsequent_hour_cents,
    nightSurchargePercent = night_surcharge_percent.toInt(),
    nightFrom = LocalTime.parse(night_from),
    nightTo = LocalTime.parse(night_to),
    graceMinutes = grace_minutes.toInt(),
    ivaPercent = iva_percent.toInt(),
    validFrom = Instant.fromEpochMilliseconds(valid_from_ms),
    validTo = valid_to_ms?.let { Instant.fromEpochMilliseconds(it) },
)

private fun com.parking.shared.db.Zones.toDomain(): Zone = Zone(
    id = id,
    parkingId = parking_id,
    code = code,
    capacity = capacity.toInt(),
    currentOccupancy = current_occupancy.toInt(),
    allowedVehicleTypes = allowed_types_csv.split(",")
        .filter { it.isNotBlank() }
        .map { VehicleType.valueOf(it) },
)

private fun com.parking.shared.db.Sessions.toDomainSession(): ParkingSession = ParkingSession(
    id = id,
    parkingId = parking_id,
    plate = PlateNumber(plate),
    vehicleType = VehicleType.valueOf(vehicle_type),
    zoneId = zone_id,
    entryAt = Instant.fromEpochMilliseconds(entry_at_ms),
    exitAt = exit_at_ms?.let { Instant.fromEpochMilliseconds(it) },
    status = SessionStatus.valueOf(status),
    customerId = customer_id,
    operatorUserId = operator_user_id,
    entryGate = entry_gate,
)

private fun com.parking.shared.db.Receipts.toDomain(): Receipt = Receipt(
    id = id,
    sessionId = session_id,
    parkingId = parking_id,
    localId = local_id,
    sequenceLocal = sequence_local,
    issuedAt = Instant.fromEpochMilliseconds(issued_at_ms),
    subtotalCents = subtotal_cents,
    ivaCents = iva_cents,
    totalCents = total_cents,
    cufe = cufe,
    syncStatus = ReceiptSyncStatus.valueOf(sync_status),
    serverId = server_id,
)

private fun com.parking.shared.db.Customers.toDomain(): Customer = Customer(
    id = id,
    parkingId = parking_id,
    fullName = full_name,
    documentNumber = document_number,
    email = email,
    phone = phone,
    hasActiveMonthly = has_active_monthly == 1L,
    monthlyExpiresOn = monthly_expires_ms?.let { Instant.fromEpochMilliseconds(it) },
)

/**
 * SHA-256 mínima multiplataforma (implementación pura Kotlin para no
 * arrastrar dependencias nativas).  Suficiente para hash chain de auditoría.
 *
 * Basada en el algoritmo estándar (FIPS 180-4).
 */
private fun simpleSha256(input: String): String {
    val bytes = input.encodeToByteArray()
    val k = intArrayOf(
        0x428a2f98.toInt(), 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(), 0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
        0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
        0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(), 0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
        0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(), 0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(), 0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt(),
    )
    val h = intArrayOf(
        0x6a09e667, 0xbb67ae85.toInt(), 0x3c6ef372, 0xa54ff53a.toInt(),
        0x510e527f, 0x9b05688c.toInt(), 0x1f83d9ab, 0x5be0cd19,
    )
    val ml = bytes.size.toLong() * 8L
    val padLen = ((bytes.size + 9 + 63) / 64) * 64
    val padded = ByteArray(padLen)
    bytes.copyInto(padded)
    padded[bytes.size] = 0x80.toByte()
    for (i in 0..7) {
        padded[padded.size - 1 - i] = (ml ushr (8 * i) and 0xFF).toByte()
    }
    val w = IntArray(64)
    var offset = 0
    while (offset < padded.size) {
        for (i in 0..15) {
            val o = offset + i * 4
            w[i] = ((padded[o].toInt() and 0xFF) shl 24) or
                ((padded[o + 1].toInt() and 0xFF) shl 16) or
                ((padded[o + 2].toInt() and 0xFF) shl 8) or
                (padded[o + 3].toInt() and 0xFF)
        }
        for (i in 16..63) {
            val s0 = (w[i - 15] rotr 7) xor (w[i - 15] rotr 18) xor (w[i - 15] ushr 3)
            val s1 = (w[i - 2] rotr 17) xor (w[i - 2] rotr 19) xor (w[i - 2] ushr 10)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }
        var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
        var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
        for (i in 0..63) {
            val s1 = (e rotr 6) xor (e rotr 11) xor (e rotr 25)
            val ch = (e and f) xor (e.inv() and g)
            val t1 = hh + s1 + ch + k[i] + w[i]
            val s0 = (a rotr 2) xor (a rotr 13) xor (a rotr 22)
            val mj = (a and b) xor (a and c) xor (b and c)
            val t2 = s0 + mj
            hh = g; g = f; f = e; e = d + t1
            d = c; c = b; b = a; a = t1 + t2
        }
        h[0] += a; h[1] += b; h[2] += c; h[3] += d
        h[4] += e; h[5] += f; h[6] += g; h[7] += hh
        offset += 64
    }
    val sb = StringBuilder()
    for (v in h) {
        sb.append(v.toUInt().toString(16).padStart(8, '0'))
    }
    return sb.toString()
}

private infix fun Int.rotr(n: Int): Int = (this ushr n) or (this shl (32 - n))
