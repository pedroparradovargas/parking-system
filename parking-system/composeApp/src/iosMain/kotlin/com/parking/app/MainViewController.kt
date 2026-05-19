package com.parking.app

import androidx.compose.ui.window.ComposeUIViewController
import com.parking.app.di.appCommonModule
import com.parking.shared.data.local.DatabaseDriverFactory
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController

/**
 * Bridge expuesto a Swift: devuelve un `UIViewController` que aloja
 * la app Compose.  Swift lo embebe con `UIViewControllerRepresentable`.
 *
 * El startKoin se hace UNA sola vez — si Swift recrea el VC, no se
 * vuelve a inicializar Koin.
 */
private var koinStarted: Boolean = false

fun MainViewController(): UIViewController {
    if (!koinStarted) {
        Napier.base(DebugAntilog())
        startKoin {
            modules(
                module { single { DatabaseDriverFactory() } },
                appCommonModule(
                    baseUrl = "https://api.parking.example.com",
                    parkingId = "00000000-0000-0000-0000-000000000000",
                    deviceId = "iOS-${platform.UIKit.UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown"}",
                ),
            )
        }
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}
