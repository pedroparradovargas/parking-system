package com.parking.app.ui.screens.tariff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.parking.app.ui.components.VehicleTypeSelector
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.dto.TariffPlanDto
import com.parking.shared.data.api.dto.UpsertTariffPlanRequest
import com.parking.shared.domain.model.VehicleType

/**
 * Diálogo crear / editar plan de mensualidad.
 */
@Composable
fun TariffPlanEditDialog(
    initial: TariffPlanDto?,
    onCancel: () -> Unit,
    onSubmit: (UpsertTariffPlanRequest) -> Unit,
    submitting: Boolean = false,
) {
    val spacing = LocalSpacing.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var days by remember { mutableStateOf(initial?.durationDays?.toString() ?: "30") }
    var price by remember { mutableStateOf(initial?.priceCents?.toString() ?: "") }
    var vehicleType by remember { mutableStateOf(initial?.vehicleType ?: VehicleType.CAR) }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (initial == null) "Nuevo plan de mensualidad" else "Editar plan",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(80) },
                    label = { Text("Nombre (Mensual, Trimestral...)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    OutlinedTextField(
                        value = days, onValueChange = { days = it.filter(Char::isDigit).take(4) },
                        label = { Text("Duración (días)") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = price, onValueChange = { price = it.filter(Char::isDigit) },
                        label = { Text("Precio (COP)") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Tipo de vehículo", style = MaterialTheme.typography.labelMedium)
                VehicleTypeSelector(selected = vehicleType, onSelect = { vehicleType = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text("Plan activo", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = days.toIntOrNull()
                    val p = price.toLongOrNull()
                    validationError = when {
                        name.isBlank() -> "El nombre es obligatorio."
                        d == null || d <= 0 -> "Duración debe ser positiva."
                        d > 3650 -> "Duración demasiado larga."
                        p == null || p < 0 -> "Precio inválido."
                        else -> null
                    }
                    if (validationError == null) {
                        onSubmit(
                            UpsertTariffPlanRequest(
                                name = name,
                                durationDays = d!!,
                                priceCents = p!!,
                                vehicleType = vehicleType,
                                enabled = enabled,
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
