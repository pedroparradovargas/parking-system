package com.parking.app.ui.screens.devices

actual class DevicePort actual constructor() {
    actual fun printerStatus(): DeviceStatus = DeviceStatus.Unavailable
    actual fun cashDrawerStatus(): DeviceStatus = DeviceStatus.Unavailable
    actual fun scannerStatus(): DeviceStatus = DeviceStatus.Unavailable
    actual fun printTest(): String = "Use window.print() del navegador."
    actual fun openCashDrawer(): String = "Periferico no disponible en navegador."
    actual fun scanOnce(): String = "Use la camara via getUserMedia() para QR en navegador."
}
