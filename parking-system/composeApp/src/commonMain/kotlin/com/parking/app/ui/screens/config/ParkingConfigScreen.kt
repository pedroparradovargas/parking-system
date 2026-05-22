package com.parking.app.ui.screens.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
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
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.BackToMenuButton
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.ParkingConfigDto
import com.parking.shared.data.api.dto.UpsertParkingConfigRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private class ConfigVm(val api: ParkingApiClient, val parkingId: String, val scope: CoroutineScope) {
    val current: MutableState<ParkingConfigDto?> = mutableStateOf(null)
    val loading: MutableState<Boolean> = mutableStateOf(false)
    val error: MutableState<String?> = mutableStateOf(null)
    val saved: MutableState<Boolean> = mutableStateOf(false)

    fun reload() = scope.launch {
        loading.value = true; error.value = null; saved.value = false
        runCatching { api.adminGetConfig(parkingId) }
            .onSuccess { current.value = it }
            .onFailure { error.value = "No se pudo cargar la configuración: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }

    fun save(req: UpsertParkingConfigRequest) = scope.launch {
        loading.value = true; error.value = null
        runCatching { api.adminUpdateConfig(parkingId, req) }
            .onSuccess { current.value = it; saved.value = true }
            .onFailure { error.value = "No se pudo guardar: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }
}

@Composable
fun ParkingConfigScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    val appState = LocalAppState.current
    val api: ParkingApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val vm = remember { ConfigVm(api, appState.parkingId, scope) }
    LaunchedEffect(vm) { vm.reload() }

    val cur = vm.current.value
    var legalName by remember(cur) { mutableStateOf(cur?.legalName ?: "") }
    var taxId by remember(cur) { mutableStateOf(cur?.taxId ?: "") }
    var address by remember(cur) { mutableStateOf(cur?.legalAddress ?: "") }
    var city by remember(cur) { mutableStateOf(cur?.city ?: "") }
    var dianRes by remember(cur) { mutableStateOf(cur?.dianResolution ?: "") }
    var invoiceSeries by remember(cur) { mutableStateOf(cur?.invoiceSeries ?: "") }
    var nextSeq by remember(cur) { mutableStateOf(cur?.invoiceNextSequence?.toString() ?: "1") }
    var timezone by remember(cur) { mutableStateOf(cur?.timezone ?: "America/Bogota") }
    var operatingMode by remember(cur) { mutableStateOf(cur?.operatingMode ?: "24x7") }
    var aiUrl by remember(cur) { mutableStateOf(cur?.aiServiceUrl ?: "") }
    var notifEmail by remember(cur) { mutableStateOf(cur?.notificationEmail ?: "") }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            Box(modifier = Modifier.weight(1f)) {
                SectionHeader(
                    title = "Configuración del parqueadero",
                    description = "Datos fiscales (DIAN), horarios, integraciones externas.",
                )
            }
            OutlinedButton(onClick = { vm.reload() }) { Text("Recargar") }
            Spacer(Modifier.width(spacing.s))
            Button(onClick = {
                vm.save(
                    UpsertParkingConfigRequest(
                        legalName = legalName.takeIf { it.isNotBlank() },
                        taxId = taxId.takeIf { it.isNotBlank() },
                        legalAddress = address.takeIf { it.isNotBlank() },
                        city = city.takeIf { it.isNotBlank() },
                        dianResolution = dianRes.takeIf { it.isNotBlank() },
                        invoiceSeries = invoiceSeries.takeIf { it.isNotBlank() },
                        invoiceNextSequence = nextSeq.toLongOrNull(),
                        timezone = timezone,
                        operatingMode = operatingMode,
                        aiServiceUrl = aiUrl.takeIf { it.isNotBlank() },
                        notificationEmail = notifEmail.takeIf { it.isNotBlank() },
                    )
                )
            }, enabled = !vm.loading.value) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(Modifier.width(spacing.xs))
                Text(if (vm.loading.value) "Guardando…" else "Guardar")
            }
        }

        vm.error.value?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (vm.saved.value) Text("Configuración guardada.", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)

        // Datos fiscales
        Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            Text("Datos fiscales (DIAN)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                OutlinedTextField(value = legalName, onValueChange = { legalName = it.take(200) }, label = { Text("Razón social") }, singleLine = true, modifier = Modifier.weight(2f))
                OutlinedTextField(value = taxId, onValueChange = { taxId = it.take(40) }, label = { Text("NIT") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = address, onValueChange = { address = it.take(300) }, label = { Text("Dirección") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                OutlinedTextField(value = city, onValueChange = { city = it.take(120) }, label = { Text("Ciudad") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = dianRes, onValueChange = { dianRes = it.take(120) }, label = { Text("Resolución DIAN") }, singleLine = true, modifier = Modifier.weight(2f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                OutlinedTextField(value = invoiceSeries, onValueChange = { invoiceSeries = it.take(20) }, label = { Text("Serie facturación") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = nextSeq, onValueChange = { nextSeq = it.filter(Char::isDigit).take(10) }, label = { Text("Próximo número") }, singleLine = true, modifier = Modifier.weight(1f))
            }
        }

        // Operación
        Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            Text("Operación", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                OutlinedTextField(value = timezone, onValueChange = { timezone = it.take(60) }, label = { Text("Zona horaria") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = operatingMode, onValueChange = { operatingMode = it.uppercase().take(20) }, label = { Text("Modo (24x7 | CUSTOM)") }, singleLine = true, modifier = Modifier.weight(1f))
            }
        }

        // Integraciones
        Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            Text("Integraciones", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = aiUrl, onValueChange = { aiUrl = it.take(300) }, label = { Text("AI service URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = notifEmail, onValueChange = { notifEmail = it.take(200) }, label = { Text("Email de notificaciones") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }
}
