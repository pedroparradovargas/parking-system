package com.parking.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.parking.shared.db.ParkingDatabase

/**
 * Driver SQLDelight para iOS — usa sqlite3 del sistema vía NativeSqliteDriver.
 * El archivo .db queda en el sandbox de la app (Documents/).
 */
actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(
            schema = ParkingDatabase.Schema,
            name = "parking_offline.db",
        )
}
