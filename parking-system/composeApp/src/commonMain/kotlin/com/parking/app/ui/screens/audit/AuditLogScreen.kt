package com.parking.app.ui.screens.audit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.BackToMenuButton
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.AuditEntryRowDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

private class AuditVm(val api: ParkingApiClient, val parkingId: String, val scope: CoroutineScope) {
    val items: MutableState<List<AuditEntryRowDto>> = mutableStateOf(emptyList())
    val entity: MutableState<String> = mutableStateOf("")
    val action: MutableState<String> = mutableStateOf("")
    val loading: MutableState<Boolean> = mutableStateOf(false)
    val error: MutableState<String?> = mutableStateOf(null)
    val verifyMsg: MutableState<String?> = mutableStateOf(null)

    fun reload() = scope.launch {
        loading.value = true; error.value = null
        runCatching {
            api.adminQueryAudit(parkingId,
                entity = entity.value.takeIf { it.isNotBlank() },
                action = action.value.takeIf { it.isNotBlank() },
                limit = 200,
            )
        }.onSuccess { items.value = it }
         .onFailure { error.value = "No se pudo cargar el audit log: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }

    fun verify() = scope.launch {
        loading.value = true; error.value = null
        runCatching { api.adminVerifyAudit(parkingId) }
            .onSuccess { resp ->
                verifyMsg.value = if (resp.corrupted.isEmpty())
                    "Cadena íntegra · ${resp.checked} entradas verificadas."
                else
                    "⚠ ${resp.corrupted.size} entradas corruptas detectadas: ${resp.corrupted.joinToString().take(120)}"
            }
            .onFailure { error.value = "No se pudo verificar: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }
}

@Composable
fun AuditLogScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    val appState = LocalAppState.current
    val api: ParkingApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val vm = remember { AuditVm(api, appState.parkingId, scope) }
    LaunchedEffect(vm) { vm.reload() }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            Box(modifier = Modifier.weight(1f)) {
                SectionHeader(
                    title = "Auditoría",
                    description = "Histórico inmutable de acciones críticas (hash chain SHA-256).",
                )
            }
            OutlinedButton(onClick = { vm.verify() }) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null)
                Spacer(Modifier.width(spacing.xs))
                Text("Verificar cadena")
            }
            Spacer(Modifier.width(spacing.s))
            Button(onClick = { vm.reload() }) { Text("Recargar") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
            OutlinedTextField(value = vm.entity.value, onValueChange = { vm.entity.value = it.take(40) },
                label = { Text("Entidad (e.g. tariff, zone, user)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = vm.action.value, onValueChange = { vm.action.value = it.take(40) },
                label = { Text("Acción (e.g. created, updated)") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { vm.reload() }) { Text("Filtrar") }
        }

        vm.error.value?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        vm.verifyMsg.value?.let { msg ->
            val isOk = msg.startsWith("Cadena")
            Text(msg, color = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }

        val items = vm.items.value
        if (items.isEmpty()) {
            Text("Sin entradas para los filtros actuales.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items.forEach { e -> AuditRow(e) }
    }
}

@Composable
private fun AuditRow(e: AuditEntryRowDto) {
    val spacing = LocalSpacing.current
    val tz = remember { runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC) }
    val ts = Instant.fromEpochMilliseconds(e.tsEpochMillis).toLocalDateTime(tz)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, contentDescription = null)
            Spacer(Modifier.width(spacing.s))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.action, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(spacing.s))
                    StatusPill(label = e.entity, tone = StatusTone.Info, showDot = false)
                    Spacer(Modifier.width(spacing.s))
                    Text("$ts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("entityId: ${e.entityId} · actor: ${e.actorUserId ?: "sistema"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(e.payloadJson, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("hash: ${e.currentHash.take(16)}…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
