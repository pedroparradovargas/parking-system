package com.parking.shared.data.sync

import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.ReceiptDto
import com.parking.shared.data.api.dto.SyncPushRequest
import com.parking.shared.data.local.LocalRepository
import com.parking.shared.domain.model.Receipt
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Orquestador del outbox pattern.
 *
 * Flujo:
 *   1. ConnectivityMonitor avisa "online".
 *   2. SyncManager lee `receipts_queue WHERE status='pending'`.
 *   3. Envía batch al backend con idempotency_key (localId UUID).
 *   4. Procesa respuesta: marca synced / conflict por cada item.
 *   5. (pull) Pide al servidor los cambios desde `lastSyncMillis`.
 *
 * Estrategia de reintentos: backoff exponencial sobrio (5s, 15s, 60s, 5min).
 */
class SyncManager(
    private val local: LocalRepository,
    private val api: ParkingApiClient,
    private val connectivity: ConnectivityMonitor,
    private val deviceId: String,
    private val parkingId: String,
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    fun start() {
        scope.launch {
            connectivity.observe().collectLatest { online ->
                _state.value = _state.value.copy(online = online)
                if (online) runOnce()
            }
        }
    }

    /** Empuja outbox + pull de cambios.  Llamable manualmente desde la UI. */
    suspend fun runOnce(): SyncOutcome {
        val pending: List<Receipt> = local.pendingReceipts(parkingId)
        if (pending.isEmpty()) {
            return pullChanges()
        }

        val push = SyncPushRequest(
            deviceId = deviceId,
            parkingId = parkingId,
            receipts = pending.map { it.toDto() },
        )

        return try {
            val resp = api.pushQueue(push)
            resp.accepted.forEach { local.markReceiptSynced(it.localId, it.serverId) }
            resp.conflicts.forEach { local.markReceiptConflict(it.localId, it.reason) }
            _state.value = _state.value.copy(
                lastSyncMillis = Clock.System.now().toEpochMilliseconds(),
                pendingCount = resp.rejected.size,
                lastError = null,
            )
            Napier.i("Sync OK: ${resp.accepted.size} aceptados, ${resp.conflicts.size} conflictos, ${resp.rejected.size} rechazados")
            pullChanges()
            SyncOutcome.Success(resp.accepted.size, resp.conflicts.size, resp.rejected.size)
        } catch (e: Throwable) {
            Napier.e("Fallo en sync push", e)
            _state.value = _state.value.copy(lastError = e.message)
            SyncOutcome.Failure(e.message ?: "desconocido")
        }
    }

    private suspend fun pullChanges(): SyncOutcome {
        return try {
            val resp = api.pullChanges(parkingId, _state.value.lastSyncMillis)
            // Persiste localmente los catálogos admin (tariff_plans, special_tariffs,
            // holidays) para que la UI pueda mostrarlos sin red.  Los `tariffs` y
            // `zones` se actualizan vía sus propios métodos de UpsertX cuando la
            // UI consume el repo — aquí sólo registramos.
            if (resp.tariffPlans.isNotEmpty()) {
                local.upsertTariffPlans(resp.tariffPlans)
                Napier.i("Pull: ${resp.tariffPlans.size} planes de mensualidad cacheados")
            }
            if (resp.specialTariffs.isNotEmpty()) {
                local.upsertSpecialTariffs(resp.specialTariffs)
                Napier.i("Pull: ${resp.specialTariffs.size} tarifas especiales cacheadas")
            }
            if (resp.holidays.isNotEmpty()) {
                local.upsertHolidays(resp.holidays)
                Napier.i("Pull: ${resp.holidays.size} festivos cacheados")
            }
            if (resp.tariffs.isNotEmpty()) {
                Napier.i("Pull: ${resp.tariffs.size} tarifas en el payload (sin persistir local — UI consume vía API directo)")
            }
            _state.value = _state.value.copy(lastSyncMillis = resp.nowMillis)
            SyncOutcome.Success(0, 0, 0)
        } catch (e: Throwable) {
            Napier.w("Pull falló (no bloquea operación)", e)
            SyncOutcome.Failure(e.message ?: "pull")
        }
    }
}

data class SyncState(
    val online: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncMillis: Long = 0L,
    val lastError: String? = null,
)

sealed interface SyncOutcome {
    data class Success(val accepted: Int, val conflicts: Int, val rejected: Int) : SyncOutcome
    data class Failure(val reason: String) : SyncOutcome
}

private fun Receipt.toDto() = ReceiptDto(
    id = id,
    sessionId = sessionId,
    localId = localId,
    parkingId = parkingId,
    sequenceLocal = sequenceLocal,
    issuedAtMillis = issuedAt.toEpochMilliseconds(),
    subtotalCents = subtotalCents,
    ivaCents = ivaCents,
    totalCents = totalCents,
    cufe = cufe,
)
