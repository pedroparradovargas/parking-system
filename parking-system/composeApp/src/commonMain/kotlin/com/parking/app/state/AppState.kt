package com.parking.app.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.parking.shared.data.local.LocalRepository
import com.parking.shared.domain.model.ChargeBreakdown
import com.parking.shared.domain.model.ParkingSession
import com.parking.shared.domain.model.PlateNumber
import com.parking.shared.domain.model.SessionStatus
import com.parking.shared.domain.model.Tariff
import com.parking.shared.domain.model.VehicleType
import com.parking.shared.domain.model.Zone
import com.parking.shared.domain.tariff.TariffCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Estado global de la UI — ahora respaldado por SQLDelight vía [LocalRepository]
 * (Regla 9: offline-first).  La UI consume las listas como State observables;
 * cada escritura dispara una corutina en [scope] que persiste y los Flow
 * actualizan el State automáticamente.
 *
 * Convención: los métodos de mutación retornan un valor calculado al vuelo
 * (sesión recién creada, breakdown del cobro) y persisten en background.
 * La latencia de SQLite local es < 5 ms, así que la UI ve el cambio en el
 * siguiente frame.
 */
class AppState(
    private val repo: LocalRepository,
    private val scope: CoroutineScope,
    val parkingId: String,
    val parkingName: String,
) {
    /** Tarifa demo de fallback hasta que la DB emita la real (en práctica el Seeder garantiza una). */
    private var demoTariff: Tariff = MockData.demoTariff(parkingId)

    /** Tarifa vigente observada en SQLDelight. Si llega vacía, usamos `demoTariff`. */
    private val tariffsFlow: StateFlow<List<Tariff>> = repo.watchTariffs(parkingId)
        .stateIn(scope, SharingStarted.Eagerly, listOf(demoTariff))

    /** Usuario logueado.  En la versión real viene de AuthRepository. */
    var loggedInUser: String? by mutableStateOfDelegate(null)

    /** Rol activo — controla qué pestañas y MainMenu se muestran. */
    var userRole: UserRole by mutableStateOfDelegate(UserRole.Admin)

    /** Estado de red simulado para probar el OfflineBanner. */
    var isOnline: Boolean by mutableStateOfDelegate(true)

    /** Bandera de modo oscuro. */
    var darkTheme: Boolean by mutableStateOfDelegate(false)

    /** Sesiones activas observadas desde la DB. */
    private val _activeSessions: MutableState<List<ParkingSession>> = mutableStateOf(emptyList())
    val activeSessions: List<ParkingSession> get() = _activeSessions.value

    /** Zonas observadas desde la DB. */
    private val _zones: MutableState<List<Zone>> = mutableStateOf(emptyList())
    val zones: List<Zone> get() = _zones.value

    /** Histórico cerrado del día actual + breakdown calculado al vuelo. */
    private val _closedToday: MutableState<List<ClosedSessionEntry>> = mutableStateOf(emptyList())
    val closedToday: List<ClosedSessionEntry> get() = _closedToday.value

    init {
        // Suscripción a Flows del repo — actualizan State observables de Compose.
        scope.launch {
            repo.watchActiveSessions(parkingId).collect { list ->
                _activeSessions.value = list
            }
        }
        scope.launch {
            repo.watchZones(parkingId).collect { list ->
                _zones.value = list
            }
        }
        scope.launch {
            tariffsFlow.collect { list ->
                if (list.isNotEmpty()) demoTariff = list.first()
            }
        }
        scope.launch {
            // "Cerrados hoy" = sesiones con exit_at_ms >= inicio del día local.
            val tz = runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC)
            val startOfDay = startOfTodayMillis(tz)
            // Combinamos sesiones cerradas con la tarifa vigente para calcular breakdowns.
            repo.watchClosedSessionsSince(parkingId, startOfDay)
                .combine(tariffsFlow) { sessions, tariffs ->
                    val tariff = tariffs.firstOrNull() ?: demoTariff
                    sessions.mapNotNull { s ->
                        val exit = s.exitAt ?: return@mapNotNull null
                        val breakdown = TariffCalculator.calculate(
                            tariff = tariff,
                            entryAt = s.entryAt,
                            exitAt = exit,
                            timeZone = tz,
                        )
                        ClosedSessionEntry(s, breakdown)
                    }
                }
                .collect { _closedToday.value = it }
        }
    }

    /** Resuelve la tarifa aplicable para un tipo (placeholder hasta repo por tipo). */
    fun tariffFor(@Suppress("UNUSED_PARAMETER") type: VehicleType): Tariff = demoTariff

    /**
     * Registra entrada y persiste en SQLite en background.  El Flow `watchActiveSessions`
     * refrescará la lista en el siguiente frame de Compose.
     */
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
        scope.launch {
            repo.insertSession(session)
            zone?.id?.let { repo.incrementZoneOccupancy(it, +1) }
        }
        return session
    }

    /** Busca una sesión activa por placa (en memoria — el Flow mantiene la lista al día). */
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

    /**
     * Cierra la sesión: calcula breakdown sincrónicamente (para devolver a la UI),
     * y persiste el cierre + decremento de zona en background.
     */
    fun closeSession(session: ParkingSession): ChargeBreakdown {
        val exit = Clock.System.now()
        val breakdown = previewCharge(session, exit)
        scope.launch {
            repo.closeSession(session.id, exit)
            session.zoneId?.let { repo.incrementZoneOccupancy(it, -1) }
        }
        return breakdown
    }
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

/** Millis del inicio del día local (00:00:00 en la zona horaria del sistema). */
private fun startOfTodayMillis(tz: TimeZone): Long {
    val now = Clock.System.now().toLocalDateTime(tz)
    val startLocal = LocalDateTime(now.year, now.month, now.dayOfMonth, 0, 0, 0, 0)
    return startLocal.toInstant(tz).toEpochMilliseconds()
}

/** CompositionLocal raíz que expone el AppState a toda la jerarquía de UI. */
val LocalAppState = compositionLocalOf<AppState> {
    error("AppState no provisto. Envuelve el árbol con CompositionLocalProvider(LocalAppState provides ...).")
}
