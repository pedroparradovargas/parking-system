package com.parking.app.ui.screens.tariff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.parking.app.ui.components.VehicleTypeSelector
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.dto.TariffDto
import com.parking.shared.data.api.dto.UpsertTariffRequest
import com.parking.shared.domain.model.VehicleType

/**
 * Diálogo de creación / edición de tarifa.  Validaciones mínimas en cliente;
 * el backend re-valida y devuelve error si algo falla.
 *
 * Cuando [initial] es null → modo "crear".  Cuando trae un DTO → modo
 * "editar" (genera nueva versión y cierra la anterior, política del repo).
 */
@Composable
fun TariffEditDialog(
    initial: TariffDto?,
    onCancel: () -> Unit,
    onSubmit: (UpsertTariffRequest) -> Unit,
    submitting: Boolean = false,
) {
    val spacing = LocalSpacing.current
    var vehicleType by remember { mutableStateOf(initial?.vehicleType ?: VehicleType.CAR) }
    var firstHour by remember { mutableStateOf(initial?.firstHourCents?.let { centsToString(it) } ?: "") }
    var subsequent by remember { mutableStateOf(initial?.subsequentHourCents?.let { centsToString(it) } ?: "") }
    var nightFrom by remember { mutableStateOf(initial?.nightFromIso?.take(5) ?: "22:00") }
    var nightTo by remember { mutableStateOf(initial?.nightToIso?.take(5) ?: "06:00") }
    var nightPct by remember { mutableStateOf(initial?.nightSurchargePercent?.toString() ?: "20") }
    var grace by remember { mutableStateOf(initial?.graceMinutes?.toString() ?: "15") }
    var iva by remember { mutableStateOf(initial?.ivaPercent?.toString() ?: "19") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (initial == null) "Nueva tarifa por hora" else "Editar tarifa (nueva versión)",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s), modifier = Modifier.fillMaxWidth()) {
                Text("Tipo de vehículo", style = MaterialTheme.typography.labelMedium)
                VehicleTypeSelector(selected = vehicleType, onSelect = { vehicleType = it })

                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    OutlinedTextField(
                        value = firstHour, onValueChange = { firstHour = it.filter(Char::isDigit) },
                        label = { Text("1ª hora (COP)") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = subsequent, onValueChange = { subsequent = it.filter(Char::isDigit) },
                        label = { Text("Hora siguiente (COP)") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    OutlinedTextField(
                        value = grace, onValueChange = { grace = it.filter(Char::isDigit).take(3) },
                        label = { Text("Gracia (min)") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = iva, onValueChange = { iva = it.filter(Char::isDigit).take(3) },
                        label = { Text("IVA (%)") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(spacing.xs))
                Text("Recargo nocturno", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    OutlinedTextField(
                        value = nightFrom, onValueChange = { nightFrom = it.take(5) },
                        label = { Text("Desde (hh:mm)") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = nightTo, onValueChange = { nightTo = it.take(5) },
                        label = { Text("Hasta (hh:mm)") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = nightPct, onValueChange = { nightPct = it.filter(Char::isDigit).take(3) },
                        label = { Text("Recargo %") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val first = firstHour.toLongOrNull()
                    val sub = subsequent.toLongOrNull()
                    val pct = nightPct.toIntOrNull()
                    val gr = grace.toIntOrNull()
                    val ivaInt = iva.toIntOrNull()
                    validationError = when {
                        first == null || first < 0 -> "Primera hora inválida"
                        sub == null || sub < 0 -> "Hora siguiente inválida"
                        pct == null || pct !in 0..100 -> "Recargo % debe estar entre 0 y 100"
                        gr == null || gr !in 0..120 -> "Gracia debe estar entre 0 y 120 min"
                        ivaInt == null || ivaInt !in 0..100 -> "IVA % debe estar entre 0 y 100"
                        !nightFrom.matches(Regex("\\d{2}:\\d{2}")) -> "Hora 'desde' inválida (hh:mm)"
                        !nightTo.matches(Regex("\\d{2}:\\d{2}")) -> "Hora 'hasta' inválida (hh:mm)"
                        else -> null
                    }
                    if (validationError == null) {
                        onSubmit(
                            UpsertTariffRequest(
                                vehicleType = vehicleType,
                                firstHourCents = first!!,
                                subsequentHourCents = sub!!,
                                nightSurchargePercent = pct!!,
                                nightFromIso = nightFrom,
                                nightToIso = nightTo,
                                graceMinutes = gr!!,
                                ivaPercent = ivaInt!!,
                                validFromMillis = null,
                                validToMillis = null,
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

/** Convierte centavos COP a una cadena editable (sin separadores) — "1234500" para $12.345 con 2 decimales. */
private fun centsToString(cents: Long): String = cents.toString()
