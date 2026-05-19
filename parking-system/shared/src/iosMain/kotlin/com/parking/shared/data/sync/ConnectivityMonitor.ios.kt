package com.parking.shared.data.sync

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_global_queue
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT

/**
 * Implementación basada en NWPathMonitor (framework Network de iOS).
 * Detecta cambios de Wi-Fi, datos móviles y estados intermedios.
 */
@OptIn(ExperimentalForeignApi::class)
actual class ConnectivityMonitor {

    @Volatile private var lastKnown: Boolean = false

    actual fun observe(): Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_queue(monitor, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u))
        nw_path_monitor_set_update_handler(monitor) { path ->
            val online = nw_path_get_status(path) == nw_path_status_satisfied
            lastKnown = online
            trySend(online)
        }
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }.distinctUntilChanged()

    actual fun isOnline(): Boolean = lastKnown
}
