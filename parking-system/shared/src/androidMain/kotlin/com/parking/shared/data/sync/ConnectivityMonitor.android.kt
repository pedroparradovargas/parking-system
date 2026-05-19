package com.parking.shared.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Detector de conectividad usando ConnectivityManager.NetworkCallback.
 * Requiere el permiso ACCESS_NETWORK_STATE declarado en el manifest.
 *
 * NOTA: Se construye con `Context` mediante Koin / inyección manual; el `actual`
 * mantiene el constructor sin args para que coincida con el `expect`, y el
 * Context se inyecta vía la propiedad estática `appContext` antes del primer uso.
 */
actual class ConnectivityMonitor {

    actual fun observe(): Flow<Boolean> = callbackFlow {
        val ctx = appContext ?: error("ConnectivityMonitor: appContext no inicializado")
        val manager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)
        trySend(isOnline())
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    actual fun isOnline(): Boolean {
        val ctx = appContext ?: return false
        val manager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        /** Inyectado por ParkingApplication.onCreate(). */
        var appContext: Context? = null
    }
}
