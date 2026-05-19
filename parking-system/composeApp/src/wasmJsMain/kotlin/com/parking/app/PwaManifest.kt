package com.parking.app

/**
 * Generación del manifest.json de PWA en runtime (build-time idealmente, pero
 * en este monorepo lo dejamos accesible desde Kotlin/Wasm para que un script
 * pueda emitirlo al `resources/` antes del bundle).
 *
 * Para que la app sea instalable como PWA se requiere:
 *   - manifest.json en /resources con name, short_name, icons, theme_color
 *   - serviceWorker.js para cache offline (puede convivir con SQLDelight)
 *   - index.html con <link rel="manifest"> apuntando al archivo
 */
object PwaManifest {
    const val NAME = "Parking System"
    const val SHORT_NAME = "Parking"
    const val THEME_COLOR = "#0F3D5C"
    const val BACKGROUND_COLOR = "#F5F7FA"
    const val DISPLAY = "standalone"
    const val START_URL = "/index.html"
}
