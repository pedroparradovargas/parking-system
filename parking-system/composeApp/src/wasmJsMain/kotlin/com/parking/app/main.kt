package com.parking.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.browser.document

/**
 * Entry point Wasm/JS.  Compose Multiplatform 1.7 expone `ComposeViewport`,
 * que monta la composición en un `<div>` del DOM (típicamente `#root`).
 *
 * NOTA: Koin se inicializa actualmente desde `App.kt` cuando se cablee
 * el repositorio real.  La UI rediseñada usa `AppState` in-memory provisto
 * por CompositionLocal, así que no requiere Koin en el boot web.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Napier.base(DebugAntilog())
    // Montar sobre el div #root del index.html — body default tiene margin:8px y altura auto,
    // lo que hace que el viewport quede mal dimensionado y el contenido se salga del visible.
    val mount = document.getElementById("root") ?: document.body!!
    ComposeViewport(mount) { App() }
}
