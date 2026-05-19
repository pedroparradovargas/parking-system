package com.parking.shared.data.sync

import kotlinx.coroutines.flow.Flow

/**
 * `expect` para detectar conectividad de red de forma multiplataforma.
 *
 * Implementaciones:
 *   - Android: ConnectivityManager.NetworkCallback
 *   - iOS:     NWPathMonitor (Network framework)
 *   - JVM:     polling con InetAddress.isReachable
 *   - Wasm:    navigator.onLine + eventos online/offline
 */
expect class ConnectivityMonitor() {
    /** Emite `true` cuando hay conexión (HTTP alcanzable), `false` cuando no. */
    fun observe(): Flow<Boolean>

    /** Lectura puntual del estado (puede ser cache reciente). */
    fun isOnline(): Boolean
}
