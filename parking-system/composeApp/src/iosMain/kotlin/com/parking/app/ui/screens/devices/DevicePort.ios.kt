package com.parking.app.ui.screens.devices

actual class DevicePort actual constructor() {
    actual fun printerStatus(): DeviceStatus = DeviceStatus.Unavailable
    actual fun cashDrawerStatus(): DeviceStatus = DeviceStatus.Unavailable
    actual fun scannerStatus(): DeviceStatus = DeviceStatus.Unavailable
    actual fun printTest(): String = "Periferico no disponible en iOS."
    actual fun openCashDrawer(): String = "Periferico no disponible en iOS."
    actual fun scanOnce(): String = "Use la camara nativa para QR/placa en iOS."
}
