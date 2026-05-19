package com.parking.shared.data.api.dto

import kotlinx.serialization.Serializable

/** Petición de login con soporte para 2FA opcional. */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val totpCode: String? = null,
)

/** Respuesta exitosa con par de tokens. */
@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    val roles: List<String>,
    val parkingId: String,
    val requires2fa: Boolean,
)
