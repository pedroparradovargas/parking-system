package com.parking.backend.config

import io.ktor.server.config.ApplicationConfig

/**
 * Configuración tipada construida a partir de `application.conf`.
 *
 * Una sola raíz `AppConfig` simplifica DI y testing: cualquier subsistema
 * recibe lo que necesita sin acoplarse al sistema de configuración de Ktor.
 */
data class AppConfig(
    val database: DatabaseConfig,
    val security: SecurityConfig,
    val ai: AiConfig,
    val realtime: RealtimeConfig,
    val rateLimit: RateLimitConfig,
    val cors: CorsConfig,
) {
    companion object {
        fun from(c: ApplicationConfig): AppConfig = AppConfig(
            database = DatabaseConfig(
                jdbcUrl = c.property("database.jdbcUrl").getString(),
                username = c.property("database.username").getString(),
                password = c.property("database.password").getString(),
                poolMin = c.property("database.poolMin").getString().toInt(),
                poolMax = c.property("database.poolMax").getString().toInt(),
                sslMode = c.property("database.sslMode").getString(),
                readReplicaUrl = c.propertyOrNull("database.readReplicaUrl")?.getString().orEmpty(),
                runMigrations = c.property("database.runMigrations").getString().toBooleanStrict(),
            ),
            security = SecurityConfig(
                jwtSecret = c.property("security.jwtSecret").getString(),
                jwtIssuer = c.property("security.jwtIssuer").getString(),
                jwtAudience = c.property("security.jwtAudience").getString(),
                accessTokenTtlSeconds = c.property("security.accessTokenTtlSeconds").getString().toLong(),
                refreshTokenTtlSeconds = c.property("security.refreshTokenTtlSeconds").getString().toLong(),
                bcryptCost = c.property("security.bcryptCost").getString().toInt(),
                enableHsts = c.property("security.enableHsts").getString().toBooleanStrict(),
            ),
            ai = AiConfig(
                baseUrl = c.property("ai.baseUrl").getString(),
                timeoutMillis = c.property("ai.timeoutMillis").getString().toLong(),
                circuitBreakerThreshold = c.property("ai.circuitBreakerThreshold").getString().toInt(),
            ),
            realtime = RealtimeConfig(
                redisHost = c.property("realtime.redisHost").getString(),
                redisPort = c.property("realtime.redisPort").getString().toInt(),
                redisEnabled = c.property("realtime.redisEnabled").getString().toBooleanStrict(),
            ),
            rateLimit = RateLimitConfig(
                globalRequestsPerMinute = c.property("rateLimit.globalRequestsPerMinute").getString().toInt(),
                authRequestsPerMinute = c.property("rateLimit.authRequestsPerMinute").getString().toInt(),
            ),
            cors = CorsConfig(
                allowedHosts = c.property("cors.allowedHosts").getList(),
            ),
        )
    }
}

data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val poolMin: Int,
    val poolMax: Int,
    val sslMode: String,
    val readReplicaUrl: String,
    val runMigrations: Boolean,
)

data class SecurityConfig(
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val accessTokenTtlSeconds: Long,
    val refreshTokenTtlSeconds: Long,
    val bcryptCost: Int,
    val enableHsts: Boolean,
)

data class AiConfig(val baseUrl: String, val timeoutMillis: Long, val circuitBreakerThreshold: Int)
data class RealtimeConfig(val redisHost: String, val redisPort: Int, val redisEnabled: Boolean)
data class RateLimitConfig(val globalRequestsPerMinute: Int, val authRequestsPerMinute: Int)
data class CorsConfig(val allowedHosts: List<String>)

/**
 * Configuración del módulo Koin para el backend.  Registra el AppConfig y
 * cualquier servicio singleton que se necesite (DI mínima — no se abusa).
 */
fun io.ktor.server.application.Application.configureKoin(cfg: AppConfig) {
    install(org.koin.ktor.plugin.Koin) {
        org.koin.logger.slf4jLogger()
        modules(
            org.koin.dsl.module {
                single { cfg }
                single { cfg.database }
                single { cfg.security }
            }
        )
    }
}
