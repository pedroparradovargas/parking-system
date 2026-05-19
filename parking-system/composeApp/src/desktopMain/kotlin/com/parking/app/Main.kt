package com.parking.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.parking.app.di.appCommonModule
import com.parking.app.di.desktopModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin

/**
 * Punto de entrada de la caja de escritorio.
 *
 * - Ventana mínima 1440×900 (caja de cobro).
 * - Modo full-screen kiosk activable por flag --kiosk.
 * - Inicializa Koin con módulo desktop (periféricos) + módulo común.
 */
fun main(args: Array<String>) {
    Napier.base(DebugAntilog())
    val kiosk = args.contains("--kiosk")

    startKoin {
        modules(
            desktopModule(),
            appCommonModule(
                baseUrl = System.getenv("PARKING_API_URL") ?: "http://localhost:8080",
                parkingId = System.getenv("PARKING_ID") ?: "00000000-0000-0000-0000-000000000000",
                deviceId = "DESKTOP-${java.net.InetAddress.getLocalHost().hostName}",
            ),
        )
    }

    application {
        val state: WindowState = rememberWindowState(size = DpSize(1440.dp, 900.dp))
        Window(
            onCloseRequest = ::exitApplication,
            title = "Parking System — Caja",
            state = state,
        ) {
            LaunchedEffect(kiosk) {
                if (kiosk) {
                    state.placement = androidx.compose.ui.window.WindowPlacement.Fullscreen
                }
            }
            App()
        }
    }
}
