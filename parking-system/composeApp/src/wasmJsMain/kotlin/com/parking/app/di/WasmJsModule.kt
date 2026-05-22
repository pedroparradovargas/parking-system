package com.parking.app.di

import com.parking.shared.data.local.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Módulo Koin específico para Wasm (PWA).  Aquí se registra el
 * [DatabaseDriverFactory] que usa `WebWorkerDriver` sobre sql.js + IndexedDB.
 *
 * No hay equivalente de periféricos POS — las impresoras / cajón / lector
 * son específicos del kiosk desktop.
 */
fun wasmJsModule(): Module = module {
    single { DatabaseDriverFactory() }
}
