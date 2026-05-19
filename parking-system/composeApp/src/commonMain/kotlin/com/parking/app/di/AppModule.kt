package com.parking.app.di

import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.applyCommonConfig
import com.parking.shared.data.api.createPlatformHttpClient
import com.parking.shared.data.local.DatabaseDriverFactory
import com.parking.shared.data.local.LocalRepository
import com.parking.shared.data.sync.ConnectivityMonitor
import com.parking.shared.data.sync.SyncManager
import com.parking.shared.db.ParkingDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Módulo Koin compartido entre todas las plataformas.
 *
 * Cada plataforma combina este módulo con su propio (Android añade
 * `Context`, Desktop añade impresora/datafono, etc.).
 */
fun appCommonModule(baseUrl: String, parkingId: String, deviceId: String): Module = module {
    single {
        val driver = get<DatabaseDriverFactory>().create()
        ParkingDatabase(driver)
    }
    single { LocalRepository(get()) }

    single {
        val http = createPlatformHttpClient { applyCommonConfig(baseUrl = baseUrl) }
        ParkingApiClient(http)
    }

    single { ConnectivityMonitor() }

    single { SyncManager(get(), get(), get(), deviceId = deviceId, parkingId = parkingId) }
}
