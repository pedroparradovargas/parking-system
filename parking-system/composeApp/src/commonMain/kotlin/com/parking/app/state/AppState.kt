package com.parking.app.state

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.parking.shared.domain.model.ChargeBreakdown
import com.parking.shared.domain.model.ParkingSession
import com.parking.shared.domain.model.PlateNumber
import com.parking.shared.domain.model.SessionStatus
import com.parking.shared.domain.model.Tariff
import com.parking.shared.domain.model.VehicleType
import com.parking.shared.domain.model.Zone
import com.parking.shared.domain.tariff.TariffCalculator
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Estado global mínimo de la UI — fuente única de verdad para sesiones activas,
 * zonas y tarifa demo mientras el repositorio real (LocalRepository + SyncManager)
 * no está cableado en la UI.
 *
 * Cumple Regla 7 (tariff calc vía shared.TariffCalculator) y Regla 9 (offline-first:
 * todo opera en memoria sin requerir backend).  Cuando se conecte el repo real,
 * los métodos `registerEntry`/`closeSession` se delegan a [com.parking.shared.data.local.LocalRepository].
 */
class AppState {

    /** Identificador del parqueadero actual.  En multi-tenant viene del JWT (Regla 4). */
    val parkingId: String = MockData.PARKING_ID
    val parkingName: String = MockData.PARKING_NAME

    /** Usuario logueado.  En la versión real viene de AuthRepository. */
    var loggedInUser: String? by mutableStateOfDelegate(null)

    /** Rol activo — controla qué pestañas y MainMenu se muestran. */
    var userRole: UserRole by mutableStateOfDelegate(UserRole.Admin)

    /** Estado de red simulado para probar el OfflineBanner. */
    var isOnline: Boolean by mutableStateOfDelegate(true)

    /** Sesiones activas en memoria (lista observable por Compose). */
    val activeSessions: SnapshotStateList<ParkingSession> = mutableStateListOf<ParkingSession>().apply {
        addAll(MockData.seedActiveSessions())
    }

    /** Histórico cerrado del día para alimentar Dashboard. */
    val closedToday: SnapshotStateList<ClosedSessionEntry> = mutableStateListOf<ClosedSessionEntry>().apply {
        addAll(MockData.seedClosedToday())
    }

    /** Zonas con ocupación viva (ZonesScreen + Cashier indicator). */
    val zones: SnapshotStateList<Zone> = mutableStateListOf<Zone>().apply {
        addAll(MockData.seedZones(parkingId))
    }

    /** Tarifa demo única — en producción viene de TariffsRepository por tipo. */
    private val demoTariff: Tariff = MockData.demoTariff(parkingId)

    /** Resuelve la tarifa aplicable para un tipo (placeholder hasta cablear repo). */
    fun tariffFor(@Suppress("UNUSED_PARAMETER") type: VehicleType): Tariff = demoTariff

    /** Registra entrada de un vehículo y crea sesión ACTIVA. */
    fun registerEntry(plate: PlateNumber, type: VehicleType, zoneCode: String? = null): ParkingSession {
        val zone = zoneCode?.let { code -> zones.firstOrNull { it.code == code } }
        val session = ParkingSession(
            id = generateLocalId(),
            parkingId = parkingId,
            plate = plate,
            vehicleType = type,
            zoneId = zone?.id,
            entryAt = Clock.System.now(),
            status = SessionStatus.ACTIVE,
        )
        activeSessions.add(0, session)
        if (zone != null) {
            val idx = zones.indexOf(zone)
            zones[idx] = zone.copy(currentOccupancy = zone.currentOccupancy + 1)
        }
        return session
    }

    /** Busca una sesión activa por placa (normalizada). */
    fun findActiveByPlate(query: String): ParkingSession? {
        val q = query.uppercase().trim()
        return activeSessions.firstOrNull { it.plate.normalized() == q }
    }

    /** Calcula previsualización del cobro sin cerrar la sesión. */
    fun previewCharge(session: ParkingSession, exitAt: Instant = Clock.System.now()): ChargeBreakdown =
        TariffCalculator.calculate(
            tariff = tariffFor(session.vehicleType),
            entryAt = session.entryAt,
            exitAt = exitAt,
        )

    /** Cierra la sesión, registra el cobro y libera el cupo en la zona. */
    fun closeSession(session: ParkingSession): ChargeBreakdown {
        val exit = Clock.System.now()
        val breakdown = previewCharge(session, exit)
        activeSessions.removeAll { it.id == session.id }
        closedToday.add(0, ClosedSessionEntry(session.copy(exitAt = exit, status = SessionStatus.CLOSED), breakdown))
        session.zoneId?.let { zid ->
            val zone = zones.firstOrNull { it.id == zid } ?: return@let
            val idx = zones.indexOf(zone)
            zones[idx] = zone.copy(currentOccupancy = (zone.currentOccupancy - 1).coerceAtLeast(0))
        }
        return breakdown
    }

    /** Bandera de modo oscuro (no persistido — sería preferencia local). */
    var darkTheme: Boolean by mutableStateOfDelegate(false)
}

/** Entrada del histórico cerrado del día — Sesión + breakdown del cobro. */
data class ClosedSessionEntry(val session: ParkingSession, val breakdown: ChargeBreakdown)

/**
 * Rol del usuario en la UI.  El mockup Figma diferencia 3 modos —
 * Admin ve todo, Rented es cliente con plaza fija, Eventual es cliente
 * ocasional con pago por uso.
 */
enum class UserRole(val label: String) {
    Admin("Administrador"),
    Rented("Cliente Alquilado"),
    Eventual("Cliente Eventual"),
}

/**
 * Genera un identificador local UUID-like sin depender de java.util.UUID
 * (no disponible en wasmJs).  Suficientemente único para el outbox local;
 * el servidor asigna el ID definitivo al sincronizar (Regla 10).
 */
internal fun generateLocalId(): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val rand = kotlin.random.Random.nextLong(0L, Long.MAX_VALUE)
    return "L-${now.toString(16)}-${rand.toString(16).padStart(12, '0').takeLast(12)}"
}

/** Helper para usar `by` con `mutableStateOf` en propiedades de clase. */
private fun <T> mutableStateOfDelegate(initial: T): androidx.compose.runtime.MutableState<T> =
    mutableStateOf(initial)

/** CompositionLocal raíz que expone el AppState a toda la jerarquía de UI. */
val LocalAppState = compositionLocalOf<AppState> {
    error("AppState no provisto. Envuelve el árbol con CompositionLocalProvider(LocalAppState provides ...).")
}
