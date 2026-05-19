package com.parking.backend.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

/**
 * Configura el endpoint `/ws/occupancy` para censo en vivo de ocupación.
 *
 * El backend emite eventos cada vez que cambia la ocupación.  Los clientes
 * (web admin, caja desktop) se suscriben y muestran un dashboard sin polling.
 *
 * Para escalar horizontalmente entre nodos backend se usa Redis Pub/Sub:
 * cada nodo publica al canal `occupancy` y todos los nodos reenvían a sus
 * clientes WebSocket suscritos.  En este archivo dejamos sólo el lado del
 * SharedFlow local — la integración Redis se activa con el flag
 * `realtime.redisEnabled` y se hace en un módulo aparte de runtime.
 */
private val occupancyBus = MutableSharedFlow<OccupancyEvent>(extraBufferCapacity = 64)
val occupancyEvents: SharedFlow<OccupancyEvent> = occupancyBus.asSharedFlow()

data class OccupancyEvent(val parkingId: String, val zoneCode: String, val occupancy: Int, val capacity: Int)

/** Empuja un evento al bus.  Llamado desde el servicio de sesiones al cerrar/abrir sesión. */
suspend fun publishOccupancy(event: OccupancyEvent) {
    occupancyBus.emit(event)
}

fun Application.configureWebsockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 60.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/occupancy") {
            // Heartbeat de "hola" inicial para que el cliente sepa que está conectado.
            send(Frame.Text("""{"type":"hello"}"""))

            // Recolecta y reenvía cada evento.  Si el cliente se desconecta,
            // la coroutine termina automáticamente.
            val job = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                occupancyEvents.collect { ev ->
                    send(Frame.Text("""{"type":"occupancy","parkingId":"${ev.parkingId}","zone":"${ev.zoneCode}","occupancy":${ev.occupancy},"capacity":${ev.capacity}}"""))
                }
            }
            try {
                // Mantener vivo hasta que el peer cierre.
                while (isActive) {
                    delay(10_000)
                }
            } finally {
                job.cancel()
            }
        }
    }
}
