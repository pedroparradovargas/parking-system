package com.parking.app.di

import com.parking.app.peripherals.BarcodeScanner
import com.parking.app.peripherals.CashDrawer
import com.parking.app.peripherals.ThermalPrinter
import com.parking.shared.data.local.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Módulo Koin específico para Desktop.  Aquí se registran los periféricos
 * POS y el `DatabaseDriverFactory` para JVM.
 */
fun desktopModule(): Module = module {
    single { DatabaseDriverFactory() }
    single { ThermalPrinter() }
    single { BarcodeScanner() }
    single { CashDrawer() }
}
