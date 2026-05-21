package com.parking.backend.routes

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * Repositorio mínimo de autenticación.  En producción se separa en su
 * propio package; aquí lo dejamos junto a las rutas para mantener el árbol
 * acotado, pero el archivo es exclusivamente "data layer".
 */
data class AuthUser(
    val id: String,
    val parkingId: String,
    val username: String,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val totpSecret: String?,
    val requires2fa: Boolean,
    val roles: List<String>,
)

private object UsersT : Table("users") {
    val id = uuid("id")
    val parkingId = uuid("parking_id")
    val username = varchar("username", 80)
    val fullName = varchar("full_name", 200)
    val email = varchar("email", 200)
    val passwordHash = varchar("password_hash", 255)
    val totpSecret = varchar("totp_secret", 128).nullable()
    val requires2fa = bool("requires_2fa")
    val enabled = bool("enabled")
    val lastLoginAt = timestamp("last_login_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

private object UserRolesT : Table("user_roles") {
    val userId = uuid("user_id")
    val roleId = uuid("role_id")
}

private object RolesT : Table("roles") {
    val id = uuid("id")
    val name = varchar("name", 50)
}

private object RefreshTokensT : Table("refresh_tokens") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val tokenHash = varchar("token_hash", 255)
    val expiresAt = timestamp("expires_at")
    val revokedAt = timestamp("revoked_at").nullable()
}

object AuthRepository {

    fun findByUsername(username: String): AuthUser? = transaction {
        val row = UsersT.selectAll().where { UsersT.username eq username }.singleOrNull() ?: return@transaction null
        val userId = row[UsersT.id]
        val roles = (UserRolesT innerJoin RolesT)
            .selectAll().where { UserRolesT.userId eq userId }
            .map { it[RolesT.name] }
        AuthUser(
            id = userId.toString(),
            parkingId = row[UsersT.parkingId].toString(),
            username = row[UsersT.username],
            fullName = row[UsersT.fullName],
            email = row[UsersT.email],
            passwordHash = row[UsersT.passwordHash],
            totpSecret = row[UsersT.totpSecret],
            requires2fa = row[UsersT.requires2fa],
            roles = roles,
        )
    }

    fun touchLastLogin(userId: String) = transaction {
        UsersT.update({ UsersT.id eq UUID.fromString(userId) }) {
            it[UsersT.lastLoginAt] = Instant.now()
        }
    }

    fun issueRefreshToken(userId: String, ttlSeconds: Long): String {
        val raw = randomToken()
        transaction {
            RefreshTokensT.insert {
                it[RefreshTokensT.userId] = UUID.fromString(userId)
                it[RefreshTokensT.tokenHash] = sha256(raw)
                it[RefreshTokensT.expiresAt] = Instant.now().plusSeconds(ttlSeconds)
            }
        }
        return raw
    }

    fun validateRefreshToken(raw: String): AuthUser? {
        val hash = sha256(raw)
        return transaction {
            val row = RefreshTokensT.selectAll().where { RefreshTokensT.tokenHash eq hash }.singleOrNull() ?: return@transaction null
            if (row[RefreshTokensT.revokedAt] != null) return@transaction null
            if (row[RefreshTokensT.expiresAt].isBefore(Instant.now())) return@transaction null
            findUserById(row[RefreshTokensT.userId].toString())
        }
    }

    fun rotateRefreshToken(oldRaw: String, userId: String, ttlSeconds: Long): String {
        transaction {
            RefreshTokensT.update({ RefreshTokensT.tokenHash eq sha256(oldRaw) }) {
                it[RefreshTokensT.revokedAt] = Instant.now()
            }
        }
        return issueRefreshToken(userId, ttlSeconds)
    }

    private fun findUserById(id: String): AuthUser? = transaction {
        val row = UsersT.selectAll().where { UsersT.id eq UUID.fromString(id) }.singleOrNull() ?: return@transaction null
        val roles = (UserRolesT innerJoin RolesT)
            .selectAll().where { UserRolesT.userId eq UUID.fromString(id) }
            .map { it[RolesT.name] }
        AuthUser(
            id = row[UsersT.id].toString(),
            parkingId = row[UsersT.parkingId].toString(),
            username = row[UsersT.username],
            fullName = row[UsersT.fullName],
            email = row[UsersT.email],
            passwordHash = row[UsersT.passwordHash],
            totpSecret = row[UsersT.totpSecret],
            requires2fa = row[UsersT.requires2fa],
            roles = roles,
        )
    }

    private fun randomToken(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
