package com.parking.app.peripherals

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lector de código de barras.
 *
 * La mayoría de lectores comerciales actúan como **teclado HID**: cuando se
 * escanea un código, "tipean" rápidamente la cadena seguida de Enter.
 *
 * En consecuencia, no hay que abrir puertos serial: simplemente se captura
 * el foco del campo de placa o se intercepta el teclado a nivel ventana.
 *
 * Este wrapper expone un SharedFlow al que los componentes UI pueden
 * suscribirse y reaccionar a códigos escaneados (cuando una capa superior
 * detecta el patrón "ráfaga + Enter" característico de los scanners).
 */
class BarcodeScanner {

    private val _scans = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val scans: SharedFlow<String> = _scans.asSharedFlow()

    /** Publica un código escaneado.  Llamado desde el listener de teclado. */
    suspend fun publish(code: String) {
        if (code.isNotBlank()) _scans.emit(code.trim())
    }
}
