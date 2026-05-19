package com.parking.app.peripherals

import io.github.aakira.napier.Napier

/**
 * Cajón monedero.
 *
 * En la mayoría de instalaciones el cajón está conectado AL POS THROUGH la
 * impresora térmica (RJ-12 entre impresora y cajón).  Por eso `open()`
 * delega a `ThermalPrinter` enviándole el comando ESC/POS de pulso.
 *
 * Si el cajón está conectado por USB directo, esta clase se reemplaza por
 * una implementación que abra el puerto VCP correspondiente.
 */
class CashDrawer(private val printer: ThermalPrinter = ThermalPrinter()) {
    fun open(): Boolean {
        Napier.i("Solicitando apertura de cajón monedero")
        return printer.printReceipt(text = "", openDrawer = true)
    }
}
