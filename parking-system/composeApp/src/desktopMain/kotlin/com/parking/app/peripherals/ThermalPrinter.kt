package com.parking.app.peripherals

import io.github.aakira.napier.Napier
import java.awt.print.PrinterException
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.SimpleDoc
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.DocFlavor

/**
 * Impresora térmica ESC/POS.
 *
 * Estrategia: usar el subsistema de impresión de la JVM (`javax.print`) y
 * enviar bytes ESC/POS crudos a la impresora.  Esto funciona con la mayoría
 * de impresoras USB que exponen un driver virtual (EPSON TM-T20, etc.) y
 * con impresoras de red mediante el cola de impresión del SO.
 *
 * Para impresoras netas-IP (puerto 9100) directo, ver `RawNetworkPrinter`
 * en una versión posterior; aquí mantenemos la ruta más portable.
 */
class ThermalPrinter(private val printerName: String? = null) {

    private val service: PrintService? by lazy {
        val all = PrintServiceLookup.lookupPrintServices(null, null)
        printerName?.let { name -> all.firstOrNull { it.name.equals(name, ignoreCase = true) } }
            ?: all.firstOrNull { it.name.contains("EPSON", ignoreCase = true) || it.name.contains("TM-", ignoreCase = true) }
            ?: PrintServiceLookup.lookupDefaultPrintService()
    }

    fun isReady(): Boolean = service != null

    fun printReceipt(text: String, openDrawer: Boolean = false): Boolean {
        val svc = service ?: run { Napier.w("No hay impresora disponible"); return false }
        val bytes = buildEscPosPayload(text, openDrawer)
        return try {
            val job = svc.createPrintJob()
            val doc = SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null)
            job.print(doc, HashPrintRequestAttributeSet())
            true
        } catch (e: PrinterException) {
            Napier.e("Error imprimiendo recibo", e)
            false
        }
    }

    private fun buildEscPosPayload(text: String, openDrawer: Boolean): ByteArray {
        val out = mutableListOf<Byte>()
        // ESC @  — inicialización
        out += listOf(0x1B.toByte(), 0x40.toByte())
        // Centrar
        out += listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte())
        out += text.toByteArray(Charsets.UTF_8).toList()
        out += listOf(0x0A, 0x0A, 0x0A).map { it.toByte() }
        // Corte parcial
        out += listOf(0x1D.toByte(), 0x56.toByte(), 0x42.toByte(), 0x00.toByte())
        // Abrir cajón monedero si aplica (pulso al pin 2)
        if (openDrawer) {
            out += listOf(0x1B.toByte(), 0x70.toByte(), 0x00.toByte(), 0x32.toByte(), 0xFA.toByte())
        }
        return out.toByteArray()
    }
}
