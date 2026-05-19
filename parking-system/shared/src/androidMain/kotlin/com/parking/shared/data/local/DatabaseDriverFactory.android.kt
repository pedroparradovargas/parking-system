package com.parking.shared.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.parking.shared.db.ParkingDatabase

/**
 * Driver SQLDelight para Android.
 *
 * Usa AndroidSqliteDriver (sobre android.database.sqlite).
 * Requiere `Context` para resolver la ruta de la DB en el almacenamiento privado.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver =
        AndroidSqliteDriver(
            schema = ParkingDatabase.Schema,
            context = context.applicationContext,
            name = "parking_offline.db",
        )
}
