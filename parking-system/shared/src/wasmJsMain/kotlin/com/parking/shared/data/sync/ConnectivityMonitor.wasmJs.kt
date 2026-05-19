package com.parking.shared.data.sync

import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.w3c.dom.events.Event

/**
 * Usa `navigator.onLine` y los eventos `online`/`offline` del navegador.
 * Atención: `navigator.onLine` puede dar falsos positivos (red local sin
 * salida a internet); el SyncManager debe tolerar fallos en los pushes.
 */
actual class ConnectivityMonitor {

    actual fun observe(): Flow<Boolean> = callbackFlow {
        val onlineHandler: (Event) -> Unit = { trySend(true) }
        val offlineHandler: (Event) -> Unit = { trySend(false) }
        window.addEventListener("online", onlineHandler)
        window.addEventListener("offline", offlineHandler)
        trySend(window.navigator.onLine)
        awaitClose {
            window.removeEventListener("online", onlineHandler)
            window.removeEventListener("offline", offlineHandler)
        }
    }.distinctUntilChanged()

    actual fun isOnline(): Boolean = window.navigator.onLine
}
