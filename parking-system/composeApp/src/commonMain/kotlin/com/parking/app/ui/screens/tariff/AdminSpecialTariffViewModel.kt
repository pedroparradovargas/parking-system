package com.parking.app.ui.screens.tariff

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.SpecialTariffDto
import com.parking.shared.data.api.dto.UpsertSpecialTariffRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel del tab "Tarifas Especiales".  Mismo patrón que los otros
 * VMs de admin — envuelve [ParkingApiClient] con loading + error.
 */
class AdminSpecialTariffViewModel(
    private val api: ParkingApiClient,
    private val parkingId: String,
    private val scope: CoroutineScope,
) {
    val items: MutableState<List<SpecialTariffDto>> = mutableStateOf(emptyList())
    val loading: MutableState<Boolean> = mutableStateOf(false)
    val error: MutableState<String?> = mutableStateOf(null)
    val showCreate: MutableState<Boolean> = mutableStateOf(false)
    val editing: MutableState<SpecialTariffDto?> = mutableStateOf(null)

    fun reload() {
        scope.launch {
            loading.value = true; error.value = null
            runCatching { api.adminListSpecialTariffs(parkingId) }
                .onSuccess { items.value = it }
                .onFailure { error.value = describe(it, "No se pudieron cargar las tarifas especiales") }
            loading.value = false
        }
    }

    fun openCreate() { showCreate.value = true }
    fun openEdit(s: SpecialTariffDto) { editing.value = s }
    fun closeDialogs() { showCreate.value = false; editing.value = null }

    fun submit(req: UpsertSpecialTariffRequest) {
        scope.launch {
            loading.value = true; error.value = null
            val target = editing.value
            val res = runCatching {
                if (target != null) api.adminUpdateSpecialTariff(parkingId, target.id, req)
                else api.adminCreateSpecialTariff(parkingId, req)
            }
            res.onSuccess { closeDialogs(); reload() }
                .onFailure { error.value = describe(it, "No se pudo guardar la tarifa especial") }
            loading.value = false
        }
    }

    fun delete(s: SpecialTariffDto) {
        scope.launch {
            loading.value = true; error.value = null
            runCatching { api.adminDeleteSpecialTariff(parkingId, s.id) }
                .onSuccess { reload() }
                .onFailure { error.value = describe(it, "No se pudo eliminar la tarifa especial") }
            loading.value = false
        }
    }

    private fun describe(t: Throwable, fallback: String) =
        "$fallback: ${t.message?.takeIf { it.isNotBlank() } ?: t::class.simpleName ?: "error"}"
}
