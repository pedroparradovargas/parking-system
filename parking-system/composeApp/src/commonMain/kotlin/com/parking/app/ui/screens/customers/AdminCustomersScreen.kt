package com.parking.app.ui.screens.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.BackToMenuButton
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.AdminCustomerDto
import com.parking.shared.data.api.dto.CustomerVehicleDto
import com.parking.shared.data.api.dto.UpsertCustomerRequest
import com.parking.shared.domain.model.VehicleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** ViewModel inline para no inflar el alcance. Maneja list/create/update. */
private class CustomersVm(val api: ParkingApiClient, val parkingId: String, val scope: CoroutineScope) {
    val items: MutableState<List<AdminCustomerDto>> = mutableStateOf(emptyList())
    val loading: MutableState<Boolean> = mutableStateOf(false)
    val error: MutableState<String?> = mutableStateOf(null)
    val showCreate: MutableState<Boolean> = mutableStateOf(false)
    val editing: MutableState<AdminCustomerDto?> = mutableStateOf(null)
    val expiringFilter: MutableState<Boolean> = mutableStateOf(false)

    fun reload() = scope.launch {
        loading.value = true; error.value = null
        runCatching {
            if (expiringFilter.value) api.adminListExpiringCustomers(parkingId, 7)
            else api.adminListCustomers(parkingId)
        }.onSuccess { items.value = it }
         .onFailure { error.value = "No se pudo cargar clientes: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }

    fun submit(req: UpsertCustomerRequest) = scope.launch {
        loading.value = true; error.value = null
        runCatching {
            val target = editing.value
            if (target != null) api.adminUpdateCustomer(parkingId, target.id, req)
            else api.adminCreateCustomer(parkingId, req)
        }.onSuccess {
            showCreate.value = false; editing.value = null; reload()
        }.onFailure { error.value = "No se pudo guardar: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }
}

@Composable
fun AdminCustomersScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    val appState = LocalAppState.current
    val api: ParkingApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val vm = remember { CustomersVm(api, appState.parkingId, scope) }

    LaunchedEffect(vm) { vm.reload() }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            Box(modifier = Modifier.weight(1f)) {
                SectionHeader(
                    title = "Gestión de Clientes",
                    description = "Registrar mensualistas, vehículos asociados y vigencias.",
                )
            }
            OutlinedButton(onClick = {
                vm.expiringFilter.value = !vm.expiringFilter.value
                vm.reload()
            }) {
                Text(if (vm.expiringFilter.value) "Todos" else "Por vencer (7d)")
            }
            Spacer(Modifier.width(spacing.s))
            Button(onClick = { vm.showCreate.value = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(spacing.xs))
                Text("Nuevo cliente")
            }
        }

        vm.error.value?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        val items = vm.items.value
        if (items.isEmpty() && vm.error.value == null && !vm.loading.value) {
            Text(
                "No hay clientes ${if (vm.expiringFilter.value) "por vencer" else "todavía"}. Crea el primero con \"Nuevo cliente\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items.forEach { c -> CustomerCard(c, onEdit = { vm.editing.value = c }) }
    }

    if (vm.showCreate.value || vm.editing.value != null) {
        CustomerEditDialog(
            initial = vm.editing.value,
            onCancel = { vm.showCreate.value = false; vm.editing.value = null },
            onSubmit = { vm.submit(it) },
            submitting = vm.loading.value,
        )
    }
}

@Composable
private fun CustomerCard(c: AdminCustomerDto, onEdit: () -> Unit) {
    val spacing = LocalSpacing.current
    val sc = LocalSemanticColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(sc.purpleContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PersonOutline, contentDescription = null, tint = sc.purpleAccent)
            }
            Spacer(Modifier.width(spacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.fullName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.width(spacing.s))
                    if (c.hasActiveMonthly) {
                        StatusPill(label = "Mensualidad activa", tone = StatusTone.Success, showDot = false)
                    } else {
                        StatusPill(label = "Sin mensualidad", tone = StatusTone.Neutral, showDot = false)
                    }
                }
                Text("Doc. ${c.documentNumber} · ${c.email ?: "sin email"} · ${c.phone ?: "sin teléfono"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (c.vehicles.isNotEmpty()) {
                    Text("Placas: ${c.vehicles.joinToString { it.plate }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(spacing.xs))
                Text("Editar")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomerEditDialog(
    initial: AdminCustomerDto?,
    onCancel: () -> Unit,
    onSubmit: (UpsertCustomerRequest) -> Unit,
    submitting: Boolean,
) {
    val spacing = LocalSpacing.current
    var fullName by remember { mutableStateOf(initial?.fullName ?: "") }
    var docNumber by remember { mutableStateOf(initial?.documentNumber ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var platesText by remember { mutableStateOf(initial?.vehicles?.joinToString(",") { it.plate } ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(if (initial == null) "Nuevo cliente" else "Editar cliente",
                fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it.take(255) },
                    label = { Text("Nombre completo") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = docNumber, onValueChange = { docNumber = it.take(64) },
                    label = { Text("Documento") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    OutlinedTextField(value = email, onValueChange = { email = it.take(200) },
                        label = { Text("Email") }, singleLine = true,
                        modifier = Modifier.weight(1f))
                    OutlinedTextField(value = phone, onValueChange = { phone = it.take(40) },
                        label = { Text("Teléfono") }, singleLine = true,
                        modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = platesText,
                    onValueChange = { platesText = it.uppercase().take(200) },
                    label = { Text("Placas (separadas por coma)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    validationError = when {
                        fullName.isBlank() -> "Nombre obligatorio."
                        docNumber.isBlank() -> "Documento obligatorio."
                        else -> null
                    }
                    if (validationError == null) {
                        val vehicles = platesText
                            .split(",")
                            .map { it.trim() }
                            .filter { it.length in 4..16 }
                            .mapIndexed { idx, p ->
                                CustomerVehicleDto(
                                    id = "new",
                                    plate = p,
                                    vehicleType = VehicleType.CAR,
                                    isPrimary = idx == 0,
                                )
                            }
                        onSubmit(
                            UpsertCustomerRequest(
                                fullName = fullName,
                                documentNumber = docNumber,
                                email = email.takeIf { it.isNotBlank() },
                                phone = phone.takeIf { it.isNotBlank() },
                                vehicles = vehicles,
                            )
                        )
                    }
                },
                enabled = !submitting,
            ) { Text(if (submitting) "Guardando…" else "Guardar") }
        },
        dismissButton = { OutlinedButton(onClick = onCancel, enabled = !submitting) { Text("Cancelar") } },
    )
}
