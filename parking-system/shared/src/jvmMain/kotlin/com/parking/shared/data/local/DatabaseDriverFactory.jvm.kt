package com.parking.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.parking.shared.db.ParkingDatabase
import java.io.File
import java.util.Properties

/**
 * Driver JDBC SQLite para JVM (desktop y backend en pruebas).
 *
 * Por defecto usa un archivo en `${user.home}/.parking-system/local.db` para
 * que las cajas físicas conserven la outbox tras reinicios.  En tests puede
 * usarse `JdbcSqliteDriver.IN_MEMORY` directamente desde el caller.
 */
actual class DatabaseDriverFactory(private val dbPath: String? = null) {
    actual fun create(): SqlDriver {
        val path = dbPath ?: defaultPath()
        File(path).parentFile?.mkdirs()
        val driver = JdbcSqliteDriver("jdbc:sqlite:$path", Properties())
        // Aplica el schema completo la primera vez.
        ensureSchema(driver)
        return driver
    }

    private fun ensureSchema(driver: JdbcSqliteDriver) {
        val current = driver.executeQuery(null, "PRAGMA user_version", { c ->
            app.cash.sqldelight.db.QueryResult.Value(if (c.next().value) c.getLong(0) ?: 0L else 0L)
        }, 0, null).value
        if (current == 0L) {
            ParkingDatabase.Schema.create(driver).value
            driver.execute(null, "PRAGMA user_version = ${ParkingDatabase.Schema.version}", 0)
        }
    }

    private fun defaultPath(): String {
        val home = System.getProperty("user.home") ?: "."
        return File(home, ".parking-system/local.db").absolutePath
    }
}
