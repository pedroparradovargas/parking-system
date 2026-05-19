package com.parking.backend.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

/**
 * Configura HikariCP + Flyway + Exposed en el backend.
 *
 * Política:
 *  - HikariCP es la única fuente de DataSource (pool compartido).
 *  - Flyway se aplica al startup si runMigrations=true (default).
 *  - Exposed se conecta SOBRE el DataSource de Hikari — nunca abre su
 *    propio pool, para evitar conexiones huérfanas.
 */
fun Application.configureDatabase(cfg: DatabaseConfig) {
    log.info("Configurando datasource Hikari hacia ${cfg.jdbcUrl}")

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = cfg.jdbcUrl
        username = cfg.username
        password = cfg.password
        driverClassName = "org.postgresql.Driver"
        minimumIdle = cfg.poolMin
        maximumPoolSize = cfg.poolMax
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
        addDataSourceProperty("sslmode", cfg.sslMode)
        // Tuning sensato para cargas mixtas de OLTP.
        addDataSourceProperty("cachePrepStmts", "true")
        addDataSourceProperty("prepStmtCacheSize", "250")
        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    }
    val dataSource = HikariDataSource(hikariConfig)

    if (cfg.runMigrations) {
        log.info("Aplicando migraciones Flyway")
        Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    } else {
        log.warn("Flyway desactivado (DB_RUN_MIGRATIONS=false)")
    }

    Database.connect(dataSource)
    log.info("Exposed conectado al pool Hikari (min=${cfg.poolMin}, max=${cfg.poolMax})")
}
