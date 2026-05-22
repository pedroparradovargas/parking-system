package com.parking.app.ui.screens.zones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.dto.UpsertZoneRequest
import com.parking.shared.data.api.dto.ZoneDto
import com.parking.shared.domain.model.VehicleType

/**
 * Diálogo crear / editar zona.  Si [initial] es null → modo "crear";
 * si trae un DTO → modo "editar".  El backend valida nuevamente los datos
 * en cualquier caso (defensa en profundidad).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZoneEditDialog(
    initial: ZoneDto?,
    onCancel: () -> Unit,
    onSubmit: (UpsertZoneRequest) -> Unit,
    submitting: Boolean = false,
) {
    val spacing = LocalSpacing.current
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var capacity by remember { mutableStateOf(initial?.capacity?.toString() ?: "10") }
    var underMaintenance by remember { mutableStateOf(initial?.underMaintenance?.toString() ?: "0") }
    var displayOrder by remember { mutableStateOf(initial?.displayOrder?.toString() ?: "0") }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    val typesState = remember {
        mutableStateOf(initial?.allowedVehicleTypes?.toSet() ?: setOf(VehicleType.CAR))
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (initial == null) "Nueva zona" else "Editar zona ${initial.code}",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    OutlinedTextField(
                        value = code, onValueChange = { code = it.uppercase().take(16) },
                        label = { Text("Código (A1, VIP...)") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = capacity, onValueChange = { capacity = it.filter(Char::isDigit).take(5) },
                        label = { Text("Capacidad") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    OutlinedTextField(
                        value = underMaintenance, onValueChange = { underMaintenance = it.filter(Char::isDigit).take(4) },
                        label = { Text("Cupos en mantenimiento") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = displayOrder, onValueChange = { displayOrder = it.filter(Char::isDigit).take(4) },
                        label = { Text("Orden de visualización") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text("Tipos permitidos", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    VehicleType.entries.forEach { vt ->
                        val selected = vt in typesState.value
                        FilterChip(
                            selected = selected,
                            onClick = {
                                typesState.value = if (selected) typesState.value - vt
                                else typesState.value + vt
                            },
                            label = { Text(typeLabel(vt)) },
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text("Zona habilitada", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it.take(500) },
                    label = { Text("Notas (opcional)") }, singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cap = capacity.toIntOrNull()
                    val maint = underMaintenance.toIntOrNull()
                    val order = displayOrder.toIntOrNull()
                    validationError = when {
                        code.isBlank() -> "El código es obligatorio."
                        cap == null || cap < 0 -> "La capacidad debe ser un número no negativo."
                        maint == null || maint < 0 -> "Cupos en mantenimiento inválido."
                        maint > cap -> "Mantenimiento no puede exceder la capacidad."
                        order == null || order < 0 -> "Orden de visualización inválido."
                        typesState.value.isEmpty() -> "Selecciona al menos un tipo de vehículo."
                        else -> null
                    }
                    if (validationError == null) {
                        onSubmit(
                            UpsertZoneRequest(
                                code = code,
                                capacity = cap!!,
                                allowedVehicleTypes = typesState.value.toList(),
                                underMaintenance = maint!!,
                                enabled = enabled,
                                displayOrder = order!!,
                                notes = notes.takeIf { it.isNotBlank() },
                            )
                        )
                    }
                },
                enabled = !submitting,
            ) { Text(if (submitting) "Guardando…" else "Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel, enabled = !submitting) { Text("Cancelar") }
        },
    )
}

private fun typeLabel(t: VehicleType): String = when (t) {
    VehicleType.CAR -> "Auto"
    VehicleType.MOTORCYCLE -> "Moto"
    VehicleType.BICYCLE -> "Bici"
    VehicleType.TRUCK -> "Camioneta"
    VehicleType.BUS -> "Bus"
}
