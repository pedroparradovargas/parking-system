package com.parking.shared.data.api.dto

import kotlinx.serialization.Serializable

/**
 * DTO para la vista admin de usuarios (extiende UserDto con campos administrativos).
 */
@Serializable
data class AdminUserDto(
    val id: String,
    val parkingId: String,
    val username: String,
    val fullName: String,
    val email: String,
    val roles: List<String>,
    val enabled: Boolean,
    val requires2fa: Boolean,
    val lastLoginAtMillis: Long? = null,
    val createdAtMillis: Long,
)

/**
 * Body para crear un usuario.  El password viene en claro; el backend lo
 * hashea con bcrypt cost 12 antes de persistir.
 */
@Serializable
data class CreateUserRequest(
    val username: String,
    val fullName: String,
    val email: String,
    val password: String,
    val roles: List<String> = listOf("CASHIER"),
)

/** Body para editar — NO incluye password (usa endpoint reset-password). */
@Serializable
data class UpdateUserRequest(
    val fullName: String,
    val email: String,
    val roles: List<String>,
    val enabled: Boolean,
)

/** Respuesta al disparar reset-password: token temporal de un solo uso. */
@Serializable
data class ResetPasswordResponse(val temporaryPassword: String)

/** Respuesta al habilitar 2FA: secret base32 + URI para QR. */
@Serializable
data class EnableTotpResponse(
    val secret: String,
    val otpAuthUri: String,  // otpauth://totp/Parking:user?secret=XXX&issuer=Parking
)
