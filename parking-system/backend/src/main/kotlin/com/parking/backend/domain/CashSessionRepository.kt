package com.parking.backend.routes
import com.parking.shared.data.api.dto.CashSessionDto
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

object CashSessionsT : Table("cash_sessions") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val operatorUserId = uuid("operator_user_id")
    val openedAt = timestamp("opened_at")
    val closedAt = timestamp("closed_at").nullable()
    val totalCashCents = long("total_cash_cents")
    val totalCardCents = long("total_card_cents")
    val totalOtherCents = long("total_other_cents")
    val sessionsCount = integer("sessions_count")
    val hashChain = varchar("hash_chain", 64).nullable()
    val notes = text("notes").nullable()
}

object CashSessionRepository {

    fun open(parkingId: String, operatorUserId: String, notes: String?): CashSessionDto = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val operatorUuid = UUID.fromString(operatorUserId)

        val existing = CashSessionsT.selectAll().where {
            (CashSessionsT.parkingId eq parkingUuid) and
                (CashSessionsT.operatorUserId eq operatorUuid) and
                (CashSessionsT.closedAt.isNull())
        }.firstOrNull()

        if (existing != null) {
            error("El operador ya tiene un turno abierto (id=${existing[CashSessionsT.id]})")
        }

        val sessionId = UUID.randomUUID()
        val now = Instant.now()
        CashSessionsT.insert {
            it[id] = sessionId
            it[CashSessionsT.parkingId] = parkingUuid
            it[CashSessionsT.operatorUserId] = operatorUuid
            it[openedAt] = now
            it[totalCashCents] = 0
            it[totalCardCents] = 0
            it[totalOtherCents] = 0
            it[sessionsCount] = 0
            it[CashSessionsT.notes] = notes
        }

        val operatorName = AdminUsersT.selectAll()
            .where { AdminUsersT.id eq operatorUuid }
            .firstOrNull()?.get(AdminUsersT.fullName) ?: ""

        runCatching {
            AuditRepository.append(parkingId, "cash_session.opened", "cash_session", sessionId.toString(), operatorUserId, "{}")
        }

        CashSessionDto(
            id = sessionId.toString(),
            parkingId = parkingId,
            operatorUserId = operatorUserId,
            operatorName = operatorName,
            openedAtMillis = now.toEpochMilli(),
            notes = notes,
        )
    }

    fun close(cashSessionId: String, parkingId: String, operatorUserId: String, notes: String?): CashSessionDto = transaction {
        val csUuid = UUID.fromString(cashSessionId)
        val parkingUuid = UUID.fromString(parkingId)
        val operatorUuid = UUID.fromString(operatorUserId)

        val row = CashSessionsT.selectAll().where {
            (CashSessionsT.id eq csUuid) and
                (CashSessionsT.parkingId eq parkingUuid)
        }.firstOrNull() ?: error("Turno no encontrado")

        if (row[CashSessionsT.closedAt] != null) {
            error("El turno ya fue cerrado")
        }

        val openedAt = row[CashSessionsT.openedAt]
        val now = Instant.now()

        val sessions = ParkingSessionsT.selectAll().where {
            (ParkingSessionsT.parkingId eq parkingUuid) and
                (ParkingSessionsT.operatorUserId eq operatorUuid) and
                (ParkingSessionsT.status eq "CLOSED")
        }.toList().filter { s ->
            val exitAt = s[ParkingSessionsT.exitAt] ?: return@filter false
            !exitAt.isBefore(openedAt) && !exitAt.isAfter(now)
        }

        val totalCash = sessions.sumOf { it[ParkingSessionsT.totalCents] ?: 0L }
        val count = sessions.size

        val finalNotes = listOfNotNull(row[CashSessionsT.notes], notes)
            .joinToString(" | ").ifBlank { null }

        CashSessionsT.update({ CashSessionsT.id eq csUuid }) {
            it[closedAt] = now
            it[totalCashCents] = totalCash
            it[totalCardCents] = 0
            it[totalOtherCents] = 0
            it[sessionsCount] = count
            it[CashSessionsT.notes] = finalNotes
        }

        val operatorName = AdminUsersT.selectAll()
            .where { AdminUsersT.id eq operatorUuid }
            .firstOrNull()?.get(AdminUsersT.fullName) ?: ""

        runCatching {
            AuditRepository.append(parkingId, "cash_session.closed", "cash_session", cashSessionId, operatorUserId,
                """{"sessions_count":$count,"total_cash_cents":$totalCash}""")
        }

        CashSessionDto(
            id = cashSessionId,
            parkingId = parkingId,
            operatorUserId = operatorUserId,
            operatorName = operatorName,
            openedAtMillis = openedAt.toEpochMilli(),
            closedAtMillis = now.toEpochMilli(),
            totalCashCents = totalCash,
            totalCardCents = 0,
            totalOtherCents = 0,
            sessionsCount = count,
            notes = finalNotes,
        )
    }

    fun currentForOperator(parkingId: String, operatorUserId: String): CashSessionDto? = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val operatorUuid = UUID.fromString(operatorUserId)

        val row = CashSessionsT.selectAll().where {
            (CashSessionsT.parkingId eq parkingUuid) and
                (CashSessionsT.operatorUserId eq operatorUuid) and
                (CashSessionsT.closedAt.isNull())
        }.firstOrNull() ?: return@transaction null

        val operatorName = AdminUsersT.selectAll()
            .where { AdminUsersT.id eq operatorUuid }
            .firstOrNull()?.get(AdminUsersT.fullName) ?: ""

        toDto(row, operatorName)
    }

    fun list(parkingId: String, limit: Int = 50, offset: Long = 0): List<CashSessionDto> = transaction {
        val parkingUuid = UUID.fromString(parkingId)

        val rows = CashSessionsT.selectAll()
            .where { CashSessionsT.parkingId eq parkingUuid }
            .orderBy(CashSessionsT.openedAt to SortOrder.DESC)
            .limit(limit).offset(offset)
            .toList()

        val operatorIds = rows.map { it[CashSessionsT.operatorUserId] }.distinct()
        val operatorNames: Map<UUID, String> = if (operatorIds.isEmpty()) emptyMap()
        else AdminUsersT.selectAll().where { AdminUsersT.id.inList(operatorIds) }
            .associate { it[AdminUsersT.id] to it[AdminUsersT.fullName] }

        rows.map { row -> toDto(row, operatorNames[row[CashSessionsT.operatorUserId]] ?: "") }
    }

    private fun toDto(row: org.jetbrains.exposed.sql.ResultRow, operatorName: String) = CashSessionDto(
        id = row[CashSessionsT.id].toString(),
        parkingId = row[CashSessionsT.parkingId].toString(),
        operatorUserId = row[CashSessionsT.operatorUserId].toString(),
        operatorName = operatorName,
        openedAtMillis = row[CashSessionsT.openedAt].toEpochMilli(),
        closedAtMillis = row[CashSessionsT.closedAt]?.toEpochMilli(),
        totalCashCents = row[CashSessionsT.totalCashCents],
        totalCardCents = row[CashSessionsT.totalCardCents],
        totalOtherCents = row[CashSessionsT.totalOtherCents],
        sessionsCount = row[CashSessionsT.sessionsCount],
        notes = row[CashSessionsT.notes],
    )
}
