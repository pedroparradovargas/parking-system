package com.parking.shared.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Factory `expect` para crear el driver SQLDelight según plataforma.
 *
 * Implementaciones:
 *   - Android:   AndroidSqliteDriver (Context-based)
 *   - iOS:       NativeSqliteDriver (sqlite3.framework)
 *   - JVM:       JdbcSqliteDriver (file-based o in-memory)
 *   - Wasm:      WebWorkerDriver (sql.js + IndexedDB)
 *
 * NOTA: Cada `actual` recibe lo que necesite (Context, FilePath, etc.) por
 * inyección directa en el constructor — no usamos service locator.
 */
expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}
