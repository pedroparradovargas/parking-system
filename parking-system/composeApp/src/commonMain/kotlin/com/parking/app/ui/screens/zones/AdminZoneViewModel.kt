package com.parking.app.ui.screens.zones

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.UpsertZoneRequest
import com.parking.shared.data.api.dto.ZoneDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel del módulo Admin > Zonas.  Envuelve [ParkingApiClient] con
 * loading + error state.  Diseñado para vivir mientras la pantalla esté
 * compuesta (se crea con `remember`).
 */
class AdminZoneViewModel(
    private val api: ParkingApiClient,
    private val parkingId: String,
    private val scope: CoroutineScope,
) {
    val items: MutableState<List<ZoneDto>> = mutableStateOf(emptyList())
    val loading: MutableState<Boolean> = mutableStateOf(false)
    val error: MutableState<String?> = mutableStateOf(null)
    val showCreate: MutableState<Boolean> = mutableStateOf(false)
    val editing: MutableState<ZoneDto?> = mutableStateOf(null)

    fun reload() {
        scope.launch {
            loading.value = true
            error.value = null
            runCatching { api.adminListZones(parkingId) }
                .onSuccess { items.value = it }
                .onFailure { error.value = describe(it, "No se pudieron cargar las zonas") }
            loading.value = false
        }
    }

    fun openCreate() { showCreate.value = true }
    fun openEdit(z: ZoneDto) { editing.value = z }
    fun closeDialogs() { showCreate.value = false; editing.value = null }

    fun submit(req: UpsertZoneRequest) {
        scope.launch {
            loading.value = true
            error.value = null
            val target = editing.value
            val result = runCatching {
                if (target != null) api.adminUpdateZone(parkingId, target.id, req)
                else api.adminCreateZone(parkingId, req)
            }
            result.onSuccess {
                closeDialogs()
                reload()
            }.onFailure {
                error.value = describe(it, "No se pudo guardar la zona")
            }
            loading.value = false
        }
    }

    fun delete(z: ZoneDto) {
        scope.launch {
            loading.value = true
            error.value = null
            runCatching { api.adminDeleteZone(parkingId, z.id) }
                .onSuccess { reload() }
                .onFailure { error.value = describe(it, "No se pudo eliminar la zona") }
            loading.value = false
        }
    }

    private fun describe(t: Throwable, fallback: String): String {
        val msg = t.message?.takeIf { it.isNotBlank() } ?: t::class.simpleName ?: "error"
        return "$fallback: $msg"
    }
}
