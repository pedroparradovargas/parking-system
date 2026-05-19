package com.parking.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

// Kotlin/Wasm exige que `js(code)` sea el cuerpo único de una función top-level
// o el inicializador de una propiedad — no permite anidarlo dentro de otras llamadas.
//
// URL relativa simple (no `new URL(import.meta.url)`) para evitar que webpack
// intente resolver el worker en build-time.  El navegador lo carga al lado de
// `index.html` en runtime — el archivo se sirve desde `resources/`.
private fun newWorker(): Worker = js("""new Worker("parking-worker.js")""")

/**
 * Driver SQLDelight para Wasm/JS — corre sql.js dentro de un Web Worker
 * y persiste en IndexedDB.
 *
 * El bundle web debe servir `parking-worker.js` (incluido por SQLDelight
 * `web-worker-driver`) al lado de `composeApp.js`.
 *
 * IMPORTANTE: A diferencia de JVM, en Wasm-JS no podemos crear el schema
 * de forma síncrona (no se puede bloquear en el hilo principal del navegador).
 * El caller debe invocar `ParkingDatabase.Schema.awaitCreate(driver)` o
 * `.create(driver).await()` antes de la primera query.
 */
actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver = WebWorkerDriver(newWorker())
}
