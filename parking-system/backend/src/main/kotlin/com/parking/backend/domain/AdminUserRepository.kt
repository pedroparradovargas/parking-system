package com.parking.backend.routes

import at.favre.lib.crypto.bcrypt.BCrypt
import com.parking.shared.data.api.dto.AdminUserDto
import com.parking.shared.data.api.dto.CreateUserRequest
import com.parking.shared.data.api.dto.UpdateUserRequest
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

/** Tabla extendida (cubre todas las columnas V1 que AuthRepository no proyecta). */
object AdminUsersT : Table("users") {
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
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

object AdminRolesT : Table("roles") {
    val id = uuid("id")
    val name = varchar("name", 50)
}

object AdminUserRolesT : Table("user_roles") {
    val userId = uuid("user_id")
    val roleId = uuid("role_id")
    override val primaryKey = PrimaryKey(userId, roleId)
}

/**
 * CRUD admin sobre `users` + asignación de roles + 2FA.
 *
 *  - `create()` hashea el password con bcrypt cost 12.
 *  - `update()` NO modifica password ni totpSecret (endpoints separados).
 *  - `disable()` soft-disable: marca `enabled=false`.  Preserva el historial.
 *  - `resetPassword()` genera un password temporal aleatorio (base64url 18b)
 *    y lo retorna al admin — el operador debe cambiarlo en su próximo login.
 *  - `enable2fa()` genera un secret TOTP base32 random; el admin debe escanear
 *    el QR antes de que pase el TTL del próximo login.
 */
object AdminUserRepository {

    private val rng = SecureRandom()

    fun list(parkingId: String): List<AdminUserDto> = transaction {
        val parkingUuid = UUID.fromString(parkingId)
        val rows = AdminUsersT.selectAll()
            .where { AdminUsersT.parkingId eq parkingUuid }
            .orderBy(AdminUsersT.username to SortOrder.ASC)
            .toList()
        rows.map { it.toDto(rolesFor(it[AdminUsersT.id])) }
    }

    fun create(parkingId: String, req: CreateUserRequest): AdminUserDto = transaction {
        validate(req)
        val parkingUuid = UUID.fromString(parkingId)
        val newId = UUID.randomUUID()
        val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
        val now = Instant.now()
        AdminUsersT.insert {
            it[id] = newId
            it[AdminUsersT.parkingId] = parkingUuid
            it[username] = req.username
            it[fullName] = req.fullName
            it[email] = req.email
            it[passwordHash] = hash
            it[totpSecret] = null
            it[requires2fa] = false
            it[enabled] = true
            it[createdAt] = now
            it[updatedAt] = now
        }
        assignRoles(newId, req.roles)
        AdminUsersT.selectAll().where { AdminUsersT.id eq newId }.single()
            .toDto(rolesFor(newId))
    }

    fun update(parkingId: String, userId: String, req: UpdateUserRequest): AdminUserDto = transaction {
        require(req.fullName.isNotBlank()) { "full_name required" }
        require(req.email.contains("@")) { "email invalid" }
        val parkingUuid = UUID.fromString(parkingId)
        val targetUuid = UUID.fromString(userId)
        val updated = AdminUsersT.update({
            (AdminUsersT.id eq targetUuid) and (AdminUsersT.parkingId eq parkingUuid)
        }) {
            it[fullName] = req.fullName
            it[email] = req.email
            it[enabled] = req.enabled
            it[updatedAt] = Instant.now()
        }
        if (updated == 0) error("user_not_found")
        // Reemplaza el set de roles.
        AdminUserRolesT.deleteWhere { AdminUserRolesT.userId eq targetUuid }
        assignRoles(targetUuid, req.roles)
        AdminUsersT.selectAll().where { AdminUsersT.id eq targetUuid }.single()
            .toDto(rolesFor(targetUuid))
    }

    fun disable(parkingId: String, userId: String): Int = transaction {
        AdminUsersT.update({
            (AdminUsersT.id eq UUID.fromString(userId)) and
                (AdminUsersT.parkingId eq UUID.fromString(parkingId))
        }) {
            it[enabled] = false
            it[updatedAt] = Instant.now()
        }
    }

    fun resetPassword(parkingId: String, userId: String): String = transaction {
        val tempPassword = randomBase64Url(18)
        val hash = BCrypt.withDefaults().hashToString(12, tempPassword.toCharArray())
        val updated = AdminUsersT.update({
            (AdminUsersT.id eq UUID.fromString(userId)) and
                (AdminUsersT.parkingId eq UUID.fromString(parkingId))
        }) {
            it[passwordHash] = hash
            it[updatedAt] = Instant.now()
        }
        if (updated == 0) error("user_not_found")
        tempPassword
    }

    fun enable2fa(parkingId: String, userId: String, issuer: String = "Parking"): Pair<String, String> = transaction {
        val secret = randomBase32(20)
        val updated = AdminUsersT.update({
            (AdminUsersT.id eq UUID.fromString(userId)) and
                (AdminUsersT.parkingId eq UUID.fromString(parkingId))
        }) {
            it[totpSecret] = secret
            it[requires2fa] = true
            it[updatedAt] = Instant.now()
        }
        if (updated == 0) error("user_not_found")
        val user = AdminUsersT.selectAll()
            .where { AdminUsersT.id eq UUID.fromString(userId) }
            .single()
        val uri = "otpauth://totp/$issuer:${user[AdminUsersT.username]}?secret=$secret&issuer=$issuer"
        secret to uri
    }

    fun disable2fa(parkingId: String, userId: String): Int = transaction {
        AdminUsersT.update({
            (AdminUsersT.id eq UUID.fromString(userId)) and
                (AdminUsersT.parkingId eq UUID.fromString(parkingId))
        }) {
            it[totpSecret] = null
            it[requires2fa] = false
            it[updatedAt] = Instant.now()
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private fun validate(req: CreateUserRequest) {
        require(req.username.isNotBlank() && req.username.length in 3..80) { "username invalid" }
        require(req.fullName.isNotBlank()) { "full_name required" }
        require(req.email.contains("@")) { "email invalid" }
        require(req.password.length >= 8) { "password too short (min 8)" }
        require(req.roles.isNotEmpty()) { "at least one role required" }
    }

    private fun rolesFor(userId: UUID): List<String> =
        AdminUserRolesT.join(AdminRolesT, org.jetbrains.exposed.sql.JoinType.INNER,
            additionalConstraint = { AdminUserRolesT.roleId eq AdminRolesT.id })
            .select(AdminRolesT.name)
            .where { AdminUserRolesT.userId eq userId }
            .map { it[AdminRolesT.name] }

    private fun assignRoles(userId: UUID, roleNames: List<String>) {
        if (roleNames.isEmpty()) return
        val roleIds = AdminRolesT.selectAll()
            .where { AdminRolesT.name inList roleNames }
            .map { it[AdminRolesT.id] }
        roleIds.forEach { rid ->
            AdminUserRolesT.insert {
                it[AdminUserRolesT.userId] = userId
                it[AdminUserRolesT.roleId] = rid
            }
        }
    }

    private fun randomBase64Url(bytes: Int): String {
        val buf = ByteArray(bytes).also(rng::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf)
    }

    private fun randomBase32(bytes: Int): String {
        val buf = ByteArray(bytes).also(rng::nextBytes)
        // RFC 4648 base32 (sin librería extra: alfabeto + padding manual).
        val alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val sb = StringBuilder()
        var bits = 0; var value = 0
        buf.forEach { b ->
            value = (value shl 8) or (b.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                bits -= 5
                sb.append(alpha[(value shr bits) and 0x1F])
            }
        }
        if (bits > 0) sb.append(alpha[(value shl (5 - bits)) and 0x1F])
        return sb.toString()
    }

    private fun ResultRow.toDto(roles: List<String>): AdminUserDto = AdminUserDto(
        id = this[AdminUsersT.id].toString(),
        parkingId = this[AdminUsersT.parkingId].toString(),
        username = this[AdminUsersT.username],
        fullName = this[AdminUsersT.fullName],
        email = this[AdminUsersT.email],
        roles = roles,
        enabled = this[AdminUsersT.enabled],
        requires2fa = this[AdminUsersT.requires2fa],
        lastLoginAtMillis = this[AdminUsersT.lastLoginAt]?.toEpochMilli(),
        createdAtMillis = this[AdminUsersT.createdAt].toEpochMilli(),
    )
}
