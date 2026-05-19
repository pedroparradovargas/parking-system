package com.parking.shared.data.sync

import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetAddress

/**
 * En JVM no hay un API nativo de conectividad: hacemos polling cada 5s
 * intentando alcanzar un host bien conocido.
 *
 * Se usa `1.1.1.1` (Cloudflare DNS) porque responde rápido y rara vez está
 * filtrado en redes corporativas con DNS hijack.
 */
actual class ConnectivityMonitor {

    @Volatile private var lastKnown: Boolean = false

    actual fun observe(): Flow<Boolean> = callbackFlow {
        val job = launch {
            while (isActive) {
                val online = checkReachability()
                if (online != lastKnown) {
                    lastKnown = online
                    trySend(online)
                }
                kotlinx.coroutines.delay(5_000)
            }
        }
        trySend(lastKnown)
        awaitClose { job.cancel() }
    }.flowOn(Dispatchers.IO).distinctUntilChanged()

    actual fun isOnline(): Boolean = lastKnown

    private fun checkReachability(): Boolean = try {
        InetAddress.getByName("1.1.1.1").isReachable(2_000)
    } catch (t: Throwable) {
        Napier.v("Connectivity probe falló: ${t.message}")
        false
    }
}
