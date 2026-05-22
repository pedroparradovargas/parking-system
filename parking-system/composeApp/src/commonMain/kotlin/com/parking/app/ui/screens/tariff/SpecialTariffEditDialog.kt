package com.parking.app.ui.screens.tariff

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
import com.parking.shared.data.api.dto.SpecialTariffDto
import com.parking.shared.data.api.dto.SpecialTariffRule
import com.parking.shared.data.api.dto.UpsertSpecialTariffRequest

/**
 * Diálogo crear / editar tarifa especial.  La regla activa cambia qué
 * campos auxiliares son requeridos (`DATE_RANGE` requiere fechas,
 * `DAY_OF_WEEK` requiere CSV de días).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpecialTariffEditDialog(
    initial: SpecialTariffDto?,
    onCancel: () -> Unit,
    onSubmit: (UpsertSpecialTariffRequest) -> Unit,
    submitting: Boolean = false,
) {
    val spacing = LocalSpacing.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var ruleType by remember { mutableStateOf(initial?.ruleType ?: SpecialTariffRule.WEEKEND) }
    var multiplier by remember { mutableStateOf(initial?.multiplier?.toString() ?: "1.20") }
    var dateFrom by remember { mutableStateOf(initial?.dateFromIso ?: "") }
    var dateTo by remember { mutableStateOf(initial?.dateToIso ?: "") }
    var dayCsv by remember { mutableStateOf(initial?.dayOfWeekCsv ?: "1,2,3,4,5") }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (initial == null) "Nueva tarifa especial" else "Editar tarifa especial",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(80) },
                    label = { Text("Nombre (Fin de semana, Festivos...)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Tipo de regla", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    SpecialTariffRule.entries.forEach { r ->
                        FilterChip(
                            selected = r == ruleType,
                            onClick = { ruleType = r },
                            label = { Text(ruleLabel(r)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = multiplier, onValueChange = { multiplier = it.take(6) },
                    label = { Text("Multiplicador (1.20 = +20%)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (ruleType == SpecialTariffRule.DATE_RANGE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                        OutlinedTextField(
                            value = dateFrom, onValueChange = { dateFrom = it.take(10) },
                            label = { Text("Desde (YYYY-MM-DD)") }, singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = dateTo, onValueChange = { dateTo = it.take(10) },
                            label = { Text("Hasta (YYYY-MM-DD)") }, singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (ruleType == SpecialTariffRule.DAY_OF_WEEK) {
                    OutlinedTextField(
                        value = dayCsv, onValueChange = { dayCsv = it.take(32) },
                        label = { Text("Días (1=Lun, 7=Dom; ej. \"6,7\" para fin de semana)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                    Text("Activa", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = multiplier.toDoubleOrNull()
                    val isoRegex = Regex("\\d{4}-\\d{2}-\\d{2}")
                    validationError = when {
                        name.isBlank() -> "El nombre es obligatorio."
                        m == null || m < 0 || m > 10 -> "Multiplicador debe estar entre 0 y 10."
                        ruleType == SpecialTariffRule.DATE_RANGE && (!dateFrom.matches(isoRegex) || !dateTo.matches(isoRegex)) ->
                            "DATE_RANGE requiere fechas válidas (YYYY-MM-DD)."
                        ruleType == SpecialTariffRule.DAY_OF_WEEK && dayCsv.isBlank() ->
                            "DAY_OF_WEEK requiere lista de días."
                        else -> null
                    }
                    if (validationError == null) {
                        onSubmit(
                            UpsertSpecialTariffRequest(
                                name = name,
                                ruleType = ruleType,
                                multiplier = m!!,
                                dateFromIso = if (ruleType == SpecialTariffRule.DATE_RANGE) dateFrom else null,
                                dateToIso = if (ruleType == SpecialTariffRule.DATE_RANGE) dateTo else null,
                                dayOfWeekCsv = if (ruleType == SpecialTariffRule.DAY_OF_WEEK) dayCsv else null,
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

private fun ruleLabel(r: SpecialTariffRule): String = when (r) {
    SpecialTariffRule.WEEKEND -> "Fin de semana"
    SpecialTariffRule.HOLIDAY -> "Festivos"
    SpecialTariffRule.DATE_RANGE -> "Rango de fechas"
    SpecialTariffRule.DAY_OF_WEEK -> "Días específicos"
}
