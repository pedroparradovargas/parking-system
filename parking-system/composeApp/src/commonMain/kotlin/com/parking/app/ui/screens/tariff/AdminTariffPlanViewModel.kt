package com.parking.app.ui.screens.tariff

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.TariffPlanDto
import com.parking.shared.data.api.dto.UpsertTariffPlanRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * ViewModel del tab "Planes de Suscripción".  Mismo patrón que
 * [AdminTariffViewModel] — envuelve [ParkingApiClient] con loading + error.
 */
class AdminTariffPlanViewModel(
    private val api: ParkingApiClient,
    private val parkingId: String,
    private val scope: CoroutineScope,
) {
    val items: MutableState<List<TariffPlanDto>> = mutableStateOf(emptyList())
    val loading: MutableState<Boolean> = mutableStateOf(false)
    val error: MutableState<String?> = mutableStateOf(null)
    val showCreate: MutableState<Boolean> = mutableStateOf(false)
    val editing: MutableState<TariffPlanDto?> = mutableStateOf(null)

    fun reload() {
        scope.launch {
            loading.value = true; error.value = null
            runCatching { api.adminListTariffPlans(parkingId) }
                .onSuccess { items.value = it }
                .onFailure { error.value = describe(it, "No se pudieron cargar los planes") }
            loading.value = false
        }
    }

    fun openCreate() { showCreate.value = true }
    fun openEdit(p: TariffPlanDto) { editing.value = p }
    fun closeDialogs() { showCreate.value = false; editing.value = null }

    fun submit(req: UpsertTariffPlanRequest) {
        scope.launch {
            loading.value = true; error.value = null
            val target = editing.value
            val res = runCatching {
                if (target != null) api.adminUpdateTariffPlan(parkingId, target.id, req)
                else api.adminCreateTariffPlan(parkingId, req)
            }
            res.onSuccess { closeDialogs(); reload() }
                .onFailure { error.value = describe(it, "No se pudo guardar el plan") }
            loading.value = false
        }
    }

    fun delete(p: TariffPlanDto) {
        scope.launch {
            loading.value = true; error.value = null
            runCatching { api.adminDeleteTariffPlan(parkingId, p.id) }
                .onSuccess { reload() }
                .onFailure { error.value = describe(it, "No se pudo eliminar el plan") }
            loading.value = false
        }
    }

    private fun describe(t: Throwable, fallback: String) =
        "$fallback: ${t.message?.takeIf { it.isNotBlank() } ?: t::class.simpleName ?: "error"}"
}
