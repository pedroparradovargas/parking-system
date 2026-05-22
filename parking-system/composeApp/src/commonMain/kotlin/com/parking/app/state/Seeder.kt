package com.parking.app.state

import com.parking.shared.data.local.LocalRepository
import com.parking.shared.domain.model.ParkingSession
import com.parking.shared.domain.model.Receipt
import com.parking.shared.domain.model.ReceiptSyncStatus
import com.parking.shared.domain.model.SessionStatus
import com.parking.shared.domain.tariff.TariffCalculator
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

/**
 * Sembrador idempotente.  En el primer arranque (zonas vacías + sin sesiones),
 * persiste en SQLite los datos demo de [MockData] para que la app tenga algo
 * que mostrar.  En arranques posteriores, no hace nada — los datos vienen
 * de la DB o del backend vía SyncManager.
 */
internal suspend fun seedDatabaseIfEmpty(
    repo: LocalRepository,
    parkingId: String,
) {
    val zonesCount = repo.countZones(parkingId)
    val sessionsCount = repo.countSessions(parkingId)
    if (zonesCount > 0 && sessionsCount > 0) return

    // Tarifa demo (única).
    val tariff = MockData.demoTariff(parkingId)
    repo.upsertTariffs(listOf(tariff))

    // Zonas con su ocupación inicial.
    repo.upsertZones(MockData.seedZones(parkingId))

    // Sesiones activas.
    MockData.seedActiveSessions().forEach { session ->
        repo.insertSession(session)
    }

    // Sesiones cerradas hoy + sus recibos (alimentan el dashboard).
    val tz = runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC)
    val now = Clock.System.now()
    MockData.seedClosedToday().forEach { entry ->
        val closed = entry.session
        repo.insertSession(
            ParkingSession(
                id = closed.id,
                parkingId = closed.parkingId,
                plate = closed.plate,
                vehicleType = closed.vehicleType,
                zoneId = closed.zoneId,
                entryAt = closed.entryAt,
                exitAt = closed.exitAt,
                status = SessionStatus.CLOSED,
                customerId = closed.customerId,
                operatorUserId = closed.operatorUserId,
                entryGate = closed.entryGate,
            ),
        )
        // Recibo asociado (estado SYNCED para no llenar el outbox con demo data).
        val breakdown = TariffCalculator.calculate(
            tariff = tariff,
            entryAt = closed.entryAt,
            exitAt = closed.exitAt ?: now,
            timeZone = tz,
        )
        repo.insertReceipt(
            Receipt(
                id = "receipt-${closed.id}",
                sessionId = closed.id,
                parkingId = closed.parkingId,
                localId = "local-${closed.id}",
                sequenceLocal = "SEQ-${closed.id.takeLast(4)}",
                issuedAt = closed.exitAt ?: now,
                subtotalCents = breakdown.subtotalCents,
                ivaCents = breakdown.ivaCents,
                totalCents = breakdown.totalCents,
                cufe = null,
                syncStatus = ReceiptSyncStatus.SYNCED,
                serverId = null,
            ),
        )
    }
}
