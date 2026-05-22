package com.parking.app.ui.screens.devices

import com.parking.app.peripherals.BarcodeScanner
import com.parking.app.peripherals.CashDrawer
import com.parking.app.peripherals.ThermalPrinter

/**
 * Implementación Desktop: enlaza con los periféricos reales bajo
 * `composeApp/desktopMain/peripherals/`.  Si la inicialización falla (sin
 * impresora física, sin acceso al sistema de printing), los `status*()`
 * devuelven `Error` y las acciones retornan mensajes descriptivos.
 *
 * NOTA: `scanOnce` aquí devuelve solo el estado del lector; la captura real
 * de un escaneo ocurre cuando una capa de UI superior intercepta los eventos
 * de teclado HID (lectores actúan como teclado) y llama a `scanner.publish(code)`.
 */
actual class DevicePort actual constructor() {

    private val printer: ThermalPrinter? = runCatching { ThermalPrinter() }.getOrNull()
    private val drawer: CashDrawer? = printer?.let { runCatching { CashDrawer(it) }.getOrNull() }
    private val scanner: BarcodeScanner? = runCatching { BarcodeScanner() }.getOrNull()

    actual fun printerStatus(): DeviceStatus =
        if (printer != null) DeviceStatus.Ready else DeviceStatus.Error

    actual fun cashDrawerStatus(): DeviceStatus =
        if (drawer != null) DeviceStatus.Ready else DeviceStatus.Error

    actual fun scannerStatus(): DeviceStatus =
        if (scanner != null) DeviceStatus.Ready else DeviceStatus.Error

    actual fun printTest(): String = runCatching {
        val p = printer ?: return@runCatching "Impresora no disponible."
        p.printReceipt(
            text = "*** PRUEBA DE IMPRESION ***\nParking System\n${java.time.LocalDateTime.now()}\n",
            openDrawer = false,
        )
        "Comando de impresion enviado."
    }.getOrElse { "Fallo la impresion: ${it.message}" }

    actual fun openCashDrawer(): String = runCatching {
        val d = drawer ?: return@runCatching "Cajon no disponible."
        d.open()
        "Cajon abierto."
    }.getOrElse { "Fallo apertura: ${it.message}" }

    actual fun scanOnce(): String =
        if (scanner != null) "Lector listo. Escanee un codigo (se captura via teclado HID en el campo enfocado)."
        else "Lector no disponible."
}
