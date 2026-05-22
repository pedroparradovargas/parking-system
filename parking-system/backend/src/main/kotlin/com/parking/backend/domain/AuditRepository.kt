package com.parking.backend.routes

import kotlinx.serialization.json.JsonElement
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Auditoría inmutable con hash chain SHA-256 (Regla 6).
 *
 * La cadena es **por parking** (multi-tenant): cada parking_id tiene su
 * propia secuencia de hashes.  Esto permite verificar integridad sin tener
 * que recorrer todos los registros del sistema.
 *
 * Fórmula:
 *   current_hash = SHA256(prev_hash || '|' || ts_millis || '|' || action ||
 *                          '|' || entity || '|' || entity_id ||
 *                          '|' || actor_user_id_or_empty || '|' || payload_json)
 *
 * Para el primer registro de cada parking, prev_hash es "0" * 64.
 */
object AuditLogT : Table("audit_log") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val ts = timestamp("ts")
    val action = varchar("action", 80)
    val entity = varchar("entity", 80)
    val entityId = varchar("entity_id", 80)
    val actorUserId = uuid("actor_user_id").nullable()
    val payload = jsonb("payload")  // ver helper más abajo
    val prevHash = varchar("prev_hash", 64).nullable()
    val currentHash = varchar("current_hash", 64)
}

/** Entrada de auditoría tal como sale de la DB (uso interno + endpoints de consulta). */
data class AuditEntry(
    val id: String,
    val parkingId: String,
    val tsEpochMillis: Long,
    val action: String,
    val entity: String,
    val entityId: String,
    val actorUserId: String?,
    val payloadJson: String,
    val prevHash: String?,
    val currentHash: String,
)

object AuditRepository {

    private val GENESIS_HASH = "0".repeat(64)

    /**
     * Añade una entrada al audit log encadenándola con el último hash del parking.
     * Devuelve el `current_hash` resultante.
     */
    fun append(
        parkingId: String,
        action: String,
        entity: String,
        entityId: String,
        actorUserId: String?,
        payloadJson: String,
    ): String = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val (prevHash, prevTs) = lastEntryFor(parkingUuid)
            ?: (GENESIS_HASH to null)
        // Garantía de monotonicidad ESTRICTA con precisión de milisegundos:
        // truncamos a ms (la DB tiene esa precisión efectiva) y, si el reloj
        // chocó con el último registro, incrementamos 1 ms.  Esto evita que
        // dos entradas terminen con el mismo TS, lo cual rompería `verifyChain`
        // por orden indeterminado al hacer ORDER BY ts.
        val nowMs = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val prevTsMs = prevTs?.truncatedTo(ChronoUnit.MILLIS)
        val ts = if (prevTsMs != null && !nowMs.isAfter(prevTsMs)) prevTsMs.plusMillis(1) else nowMs
        val canonical = "$prevHash|${ts.toEpochMilli()}|$action|$entity|$entityId|${actorUserId.orEmpty()}|$payloadJson"
        val currentHash = sha256Hex(canonical)
        AuditLogT.insert {
            it[id] = UUID.randomUUID()
            it[AuditLogT.parkingId] = parkingUuid
            it[AuditLogT.ts] = ts
            it[AuditLogT.action] = action
            it[AuditLogT.entity] = entity
            it[AuditLogT.entityId] = entityId
            it[AuditLogT.actorUserId] = actorUserId?.let(UUID::fromString)
            it[AuditLogT.payload] = payloadJson
            it[AuditLogT.prevHash] = prevHash
            it[AuditLogT.currentHash] = currentHash
        }
        currentHash
    }

    /**
     * Versión que acepta un [JsonElement] de kotlinx.serialization en lugar de String —
     * útil cuando ya tienes el payload como árbol JSON serializable.
     */
    fun append(
        parkingId: String,
        action: String,
        entity: String,
        entityId: String,
        actorUserId: String?,
        payload: JsonElement,
    ): String = append(parkingId, action, entity, entityId, actorUserId, payload.toString())

    /** Lista entradas para consulta de un parking, con filtros opcionales. Paginado. */
    fun query(
        parkingId: String,
        entity: String? = null,
        action: String? = null,
        fromMillis: Long? = null,
        toMillis: Long? = null,
        actorUserId: String? = null,
        limit: Int = 100,
        offset: Long = 0,
    ): List<AuditEntry> = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val rows = AuditLogT.selectAll().where {
            var cond = AuditLogT.parkingId eq parkingUuid
            entity?.let { cond = cond and (AuditLogT.entity eq it) }
            action?.let { cond = cond and (AuditLogT.action eq it) }
            actorUserId?.let { cond = cond and (AuditLogT.actorUserId eq UUID.fromString(it)) }
            fromMillis?.let { cond = cond and (AuditLogT.ts greaterEq Instant.ofEpochMilli(it)) }
            toMillis?.let { cond = cond and (AuditLogT.ts lessEq Instant.ofEpochMilli(it)) }
            cond
        }.orderBy(AuditLogT.ts to SortOrder.DESC)
            .limit(limit).offset(offset)
            .toList()
        rows.map { row ->
            AuditEntry(
                id = row[AuditLogT.id].toString(),
                parkingId = row[AuditLogT.parkingId].toString(),
                tsEpochMillis = row[AuditLogT.ts].toEpochMilli(),
                action = row[AuditLogT.action],
                entity = row[AuditLogT.entity],
                entityId = row[AuditLogT.entityId],
                actorUserId = row[AuditLogT.actorUserId]?.toString(),
                payloadJson = row[AuditLogT.payload],
                prevHash = row[AuditLogT.prevHash],
                currentHash = row[AuditLogT.currentHash],
            )
        }
    }

    /**
     * Recorre la cadena del parking en orden cronológico y verifica que
     * cada `current_hash` coincida con el SHA-256 calculado.  Devuelve
     * una lista de IDs corruptos (vacía si la cadena está íntegra).
     */
    fun verifyChain(parkingId: String): List<String> = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val rows = AuditLogT.selectAll()
            .where { AuditLogT.parkingId eq parkingUuid }
            .orderBy(AuditLogT.ts to SortOrder.ASC)
            .toList()
        val corrupted = mutableListOf<String>()
        var expectedPrev = GENESIS_HASH
        rows.forEach { row ->
            val payload = row[AuditLogT.payload]
            val actor = row[AuditLogT.actorUserId]?.toString().orEmpty()
            val canonical = "$expectedPrev|${row[AuditLogT.ts].toEpochMilli()}|" +
                "${row[AuditLogT.action]}|${row[AuditLogT.entity]}|${row[AuditLogT.entityId]}|" +
                "$actor|$payload"
            val expectedHash = sha256Hex(canonical)
            if (expectedHash != row[AuditLogT.currentHash]) {
                corrupted += row[AuditLogT.id].toString()
            }
            expectedPrev = row[AuditLogT.currentHash]
        }
        corrupted
    }

    private fun lastEntryFor(parkingUuid: UUID): Pair<String, Instant>? =
        AuditLogT.select(AuditLogT.currentHash, AuditLogT.ts)
            .where { AuditLogT.parkingId eq parkingUuid }
            .orderBy(AuditLogT.ts to SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.let { it[AuditLogT.currentHash] to it[AuditLogT.ts] }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Exposed no incluye `jsonb` nativo para PostgreSQL en exposed-core; el módulo
 * `exposed-json` lo provee.  Aquí definimos un alias mínimo que asume el tipo
 * en la DB (`payload JSONB`) y serializa/deserializa como String — suficiente
 * porque ya nos llega un JSON canónico desde el caller.
 */
private fun Table.jsonb(name: String) =
    registerColumn<String>(name, org.jetbrains.exposed.sql.TextColumnType())
