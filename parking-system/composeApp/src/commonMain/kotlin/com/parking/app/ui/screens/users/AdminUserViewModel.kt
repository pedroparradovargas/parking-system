package com.parking.app.ui.screens.users

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.AdminUserDto
import com.parking.shared.data.api.dto.CreateUserRequest
import com.parking.shared.data.api.dto.EnableTotpResponse
import com.parking.shared.data.api.dto.UpdateUserRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel para Admin > Usuarios.  Maneja CRUD + reset password + 2FA.
 */
class AdminUserViewModel(
    private val api: ParkingApiClient,
    private val parkingId: String,
    private val scope: CoroutineScope,
) {
    val items: MutableState<List<AdminUserDto>> = mutableStateOf(emptyList())
    val loading: MutableState<Boolean> = mutableStateOf(false)
    val error: MutableState<String?> = mutableStateOf(null)
    val showCreate: MutableState<Boolean> = mutableStateOf(false)
    val editing: MutableState<AdminUserDto?> = mutableStateOf(null)
    val tempPasswordToast: MutableState<String?> = mutableStateOf(null)
    val totpInfo: MutableState<EnableTotpResponse?> = mutableStateOf(null)

    fun reload() = scope.launch {
        loading.value = true; error.value = null
        runCatching { api.adminListUsers(parkingId) }
            .onSuccess { items.value = it }
            .onFailure { error.value = "${"No se pudieron cargar los usuarios"}: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }

    fun openCreate() { showCreate.value = true }
    fun openEdit(u: AdminUserDto) { editing.value = u }
    fun closeDialogs() { showCreate.value = false; editing.value = null }
    fun clearTempPassword() { tempPasswordToast.value = null }
    fun clearTotpInfo() { totpInfo.value = null }

    fun create(req: CreateUserRequest) = scope.launch {
        loading.value = true; error.value = null
        runCatching { api.adminCreateUser(parkingId, req) }
            .onSuccess { closeDialogs(); reload() }
            .onFailure { error.value = "No se pudo crear el usuario: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }

    fun update(userId: String, req: UpdateUserRequest) = scope.launch {
        loading.value = true; error.value = null
        runCatching { api.adminUpdateUser(parkingId, userId, req) }
            .onSuccess { closeDialogs(); reload() }
            .onFailure { error.value = "No se pudo actualizar: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }

    fun disable(u: AdminUserDto) = scope.launch {
        loading.value = true; error.value = null
        runCatching { api.adminDisableUser(parkingId, u.id) }
            .onSuccess { reload() }
            .onFailure { error.value = "No se pudo deshabilitar: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }

    fun resetPassword(u: AdminUserDto) = scope.launch {
        loading.value = true; error.value = null
        runCatching { api.adminResetPassword(parkingId, u.id) }
            .onSuccess { tempPasswordToast.value = it.temporaryPassword }
            .onFailure { error.value = "No se pudo resetear la contraseña: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }

    fun enable2fa(u: AdminUserDto) = scope.launch {
        loading.value = true; error.value = null
        runCatching { api.adminEnable2fa(parkingId, u.id) }
            .onSuccess { totpInfo.value = it; reload() }
            .onFailure { error.value = "No se pudo activar 2FA: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }

    fun disable2fa(u: AdminUserDto) = scope.launch {
        loading.value = true; error.value = null
        runCatching { api.adminDisable2fa(parkingId, u.id) }
            .onSuccess { reload() }
            .onFailure { error.value = "No se pudo desactivar 2FA: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }
}
