package com.parking.backend

import com.parking.backend.config.AppConfig
import com.parking.backend.config.configureDatabase
import com.parking.backend.config.configureKoin
import com.parking.backend.config.configureSecurity
import com.parking.backend.config.configureWebsockets
import com.parking.backend.routes.registerAdminConfigAuditRoutes
import com.parking.backend.routes.registerAdminCustomerRoutes
import com.parking.backend.routes.registerAdminReportsRoutes
import com.parking.backend.routes.registerCashSessionRoutes
import com.parking.backend.routes.registerAdminTariffRoutes
import com.parking.backend.routes.registerAdminUserRoutes
import com.parking.backend.routes.registerAdminZoneRoutes
import com.parking.backend.routes.registerAuthRoutes
import com.parking.backend.routes.registerOtherRoutes
import com.parking.backend.routes.registerSessionRoutes
import com.parking.backend.routes.registerSyncRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import kotlinx.serialization.json.Json

/**
 * Entry point del backend.  Ktor invoca `Application.module()` por reflexión
 * según el `modules = [...]` declarado en `application.conf`.
 */
fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val cfg = AppConfig.from(environment.config)

    install(DefaultHeaders) {
        // Headers de seguridad: HSTS, X-Frame, X-Content-Type, Referrer-Policy.
        header("X-Frame-Options", "DENY")
        header("X-Content-Type-Options", "nosniff")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'")
        if (cfg.security.enableHsts) {
            header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        }
    }
    install(CallLogging)
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        })
    }

    configureKoin(cfg)
    configureDatabase(cfg.database)
    configureSecurity(cfg)
    configureWebsockets()

    registerAuthRoutes()
    registerSessionRoutes()
    registerSyncRoutes()
    registerOtherRoutes()
    registerAdminTariffRoutes()
    registerAdminZoneRoutes()
    registerAdminUserRoutes()
    registerAdminCustomerRoutes()
    registerAdminReportsRoutes()
    registerAdminConfigAuditRoutes()
    registerCashSessionRoutes()
}
