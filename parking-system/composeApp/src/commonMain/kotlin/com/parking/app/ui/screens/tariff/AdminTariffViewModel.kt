package com.parking.app.ui.screens.tariff

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.TariffDto
import com.parking.shared.data.api.dto.UpsertTariffRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel ligero para Admin > Tarifas.  Envuelve [ParkingApiClient] con
 * manejo de loading + error.  No depende de androidx.lifecycle.ViewModel para
 * ser compatible con todas las plataformas KMP.
 *
 * Estado expuesto como `MutableState`:
 *  - [items]:     lista de tarifas (vigentes o histórica).
 *  - [loading]:   true mientras hay una petición en curso.
 *  - [error]:     último mensaje de error, null si OK.
 *  - [editing]:   tarifa en edición; null = formulario cerrado o creando nueva.
 */
class AdminTariffViewModel(
    private val api: ParkingApiClient,
    private val parkingId: String,
    private val scope: CoroutineScope,
) {
    val items: MutableState<List<TariffDto>> = mutableStateOf(emptyList())
    val loading: MutableState<Boolean> = mutableStateOf(false)
    val error: MutableState<String?> = mutableStateOf(null)
    val showCreate: MutableState<Boolean> = mutableStateOf(false)
    val editing: MutableState<TariffDto?> = mutableStateOf(null)
    val historic: MutableState<Boolean> = mutableStateOf(false)

    /** Recarga la lista desde el backend; si falla, mantiene la lista actual. */
    fun reload() {
        scope.launch {
            loading.value = true
            error.value = null
            runCatching { api.adminListTariffs(parkingId, historic = historic.value) }
                .onSuccess { items.value = it }
                .onFailure { error.value = mapError(it, "No se pudo cargar tarifas") }
            loading.value = false
        }
    }

    fun toggleHistoric() {
        historic.value = !historic.value
        reload()
    }

    fun openCreate() { showCreate.value = true }
    fun openEdit(t: TariffDto) { editing.value = t }
    fun closeDialogs() { showCreate.value = false; editing.value = null }

    /** Crea o actualiza según `editing`.  Cierra el diálogo en caso OK. */
    fun submit(req: UpsertTariffRequest) {
        scope.launch {
            loading.value = true
            error.value = null
            val result = runCatching {
                val target = editing.value
                if (target != null) {
                    api.adminUpdateTariff(parkingId, target.id, req)
                } else {
                    api.adminCreateTariff(parkingId, req)
                }
            }
            result.onSuccess {
                closeDialogs()
                reload()
            }.onFailure {
                error.value = mapError(it, "No se pudo guardar la tarifa")
            }
            loading.value = false
        }
    }

    fun closeTariff(tariff: TariffDto) {
        scope.launch {
            loading.value = true
            error.value = null
            runCatching { api.adminCloseTariff(parkingId, tariff.id) }
                .onSuccess { reload() }
                .onFailure { error.value = mapError(it, "No se pudo cerrar la tarifa") }
            loading.value = false
        }
    }

    private fun mapError(t: Throwable, fallback: String): String {
        val msg = t.message?.takeIf { it.isNotBlank() } ?: t::class.simpleName ?: "error"
        return "$fallback: $msg"
    }
}
