/*
 * parking-worker.js — Web Worker stub para SQLDelight WebWorkerDriver.
 *
 * STATUS: STUB.  Solo evita el error "worker not found" al cargar la PWA.
 * No implementa la lógica real de sql.js + IndexedDB.
 *
 * Para activar persistencia real en la PWA, reemplazar este stub por el
 * worker oficial de @cashapp/sqldelight-sqljs-worker (instalar como dep
 * npm en composeApp/build.gradle.kts vía `kotlin.js.npm` y re-emitir aquí).
 *
 * Mientras tanto, cualquier query a la DB en wasmJs lanzará un error visible
 * en la consola — la UI sigue funcionando porque la pantalla de login no
 * toca la base de datos.
 */
self.onmessage = function (event) {
    self.postMessage({
        id: event.data && event.data.id,
        error: "parking-worker.js stub — falta integrar @cashapp/sqldelight-sqljs-worker",
    });
};
