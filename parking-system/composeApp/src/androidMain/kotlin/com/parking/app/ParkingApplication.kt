package com.parking.app

import android.app.Application
import com.parking.app.di.appCommonModule
import com.parking.shared.data.local.DatabaseDriverFactory
import com.parking.shared.data.sync.ConnectivityMonitor
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Application Android — inicializa Koin y Napier una sola vez.
 *
 * El módulo Android añade:
 *  - DatabaseDriverFactory con Context.
 *  - Context global para ConnectivityMonitor.
 *  - Base URL y parkingId iniciales (se ajustan en Settings).
 */
class ParkingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        ConnectivityMonitor.appContext = this

        startKoin {
            androidContext(this@ParkingApplication)
            modules(
                module {
                    single { DatabaseDriverFactory(applicationContext) }
                },
                appCommonModule(
                    baseUrl = "https://api.parking.example.com",
                    parkingId = "00000000-0000-0000-0000-000000000000",
                    deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown",
                ),
            )
        }
    }
}
