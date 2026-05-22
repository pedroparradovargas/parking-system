package com.parking.app.ui.screens.zones

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.parking.app.ui.components.label
import com.parking.app.ui.components.occupancyColor
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.ZoneDto
import org.koin.compose.koinInject

/**
 * Admin > Gestión de Zonas.  Usa `FlowRow` con cards de ancho mínimo 280dp
 * (no `LazyVerticalGrid`, porque el `verticalScroll` del [AppShell] rompe
 * los Lazy* containers con `maxHeight = ∞`).
 *
 * Si el backend no responde, muestra el error inline pero la pantalla sigue
 * navegable; reintentos manuales con el botón "Recargar".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminZonesScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    val appState = LocalAppState.current
    val api: ParkingApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val vm = remember { AdminZoneViewModel(api = api, parkingId = appState.parkingId, scope = scope) }

    LaunchedEffect(vm) { vm.reload() }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            Box(modifier = Modifier.weight(1f)) {
                SectionHeader(
                    title = "Gestión de Zonas",
                    description = "Crear, editar y configurar las zonas del parqueadero.",
                )
            }
            OutlinedButton(onClick = { vm.reload() }) { Text("Recargar") }
            Spacer(Modifier.width(spacing.s))
            Button(onClick = { vm.openCreate() }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(spacing.xs))
                Text("Nueva zona")
            }
        }

        vm.error.value?.let { msg ->
            Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        val items = vm.items.value
        if (items.isEmpty() && vm.error.value == null && !vm.loading.value) {
            Text(
                "No hay zonas configuradas todavía. Crea la primera con el botón \"Nueva zona\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            items.forEach { z ->
                Box(modifier = Modifier.widthIn(min = 280.dp, max = 360.dp)) {
                    ZoneAdminCard(z, onEdit = { vm.openEdit(z) }, onDelete = { vm.delete(z) })
                }
            }
        }
    }

    if (vm.showCreate.value || vm.editing.value != null) {
        ZoneEditDialog(
            initial = vm.editing.value,
            onCancel = { vm.closeDialogs() },
            onSubmit = { vm.submit(it) },
            submitting = vm.loading.value,
        )
    }
}

@Composable
private fun ZoneAdminCard(z: ZoneDto, onEdit: () -> Unit, onDelete: () -> Unit) {
    val spacing = LocalSpacing.current
    val fraction = if (z.capacity == 0) 0f else (z.currentOccupancy.toFloat() / z.capacity).coerceIn(0f, 1f)
    val accent = occupancyColor(fraction)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(z.code, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = accent)
                }
                Spacer(Modifier.width(spacing.s))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Zona ${z.code}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "${z.currentOccupancy} / ${z.capacity - z.underMaintenance} usados " +
                            (if (z.underMaintenance > 0) "(+${z.underMaintenance} mantto)" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val tone: StatusTone = when {
                    !z.enabled -> StatusTone.Neutral
                    fraction >= 0.9f -> StatusTone.Danger
                    fraction >= 0.65f -> StatusTone.Warning
                    else -> StatusTone.Success
                }
                StatusPill(label = if (!z.enabled) "Off" else "${(fraction * 100).toInt()}%", tone = tone, showDot = false)
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Garage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(spacing.xs))
                Text(
                    "Permite: ${z.allowedVehicleTypes.joinToString { it.label() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            z.notes?.takeIf { it.isNotBlank() }?.let { note ->
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(spacing.xs))
                    Text("Editar")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = z.currentOccupancy == 0,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(spacing.xs))
                    Text("Eliminar")
                }
            }
        }
    }
}

