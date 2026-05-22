package com.parking.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.parking.app.di.appCommonModule
import com.parking.app.di.wasmJsModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.browser.document
import kotlinx.browser.window
import org.koin.core.context.startKoin

/**
 * Entry point Wasm/JS.  Compose Multiplatform 1.7 expone `ComposeViewport`,
 * que monta la composición en un `<div>` del DOM (`#root`).
 *
 * Inicializa Koin con `wasmJsModule` (DatabaseDriverFactory web-worker) +
 * `appCommonModule` (LocalRepository, ParkingApiClient, SyncManager).  Esto
 * es indispensable después de Fase A — `App()` consume `koinInject()`.
 *
 * `baseUrl` apunta al backend Ktor: en dev asumimos `http://localhost:8080`;
 * el operador puede sobrescribir vía query param `?api=...` por si la PWA
 * se sirve desde otro host.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Napier.base(DebugAntilog())

    val apiOverride = runCatching { window.location.search }
        .getOrNull()
        ?.let { Regex("[?&]api=([^&]+)").find(it)?.groupValues?.getOrNull(1) }
    val baseUrl = apiOverride ?: "http://localhost:8080"

    startKoin {
        modules(
            wasmJsModule(),
            appCommonModule(
                baseUrl = baseUrl,
                parkingId = "00000000-0000-0000-0000-000000000001",
                deviceId = "WEB-${window.navigator.userAgent.take(12)}",
            ),
        )
    }

    // Montar sobre el div #root del index.html — body default tiene margin:8px
    // y altura auto, lo que hace que el viewport quede mal dimensionado y el
    // contenido se salga del visible.
    val mount = document.getElementById("root") ?: document.body!!
    ComposeViewport(mount) { App() }
}
