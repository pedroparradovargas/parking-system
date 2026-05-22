package com.parking.app.ui.screens.tariff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.dto.SpecialTariffDto
import com.parking.shared.data.api.dto.TariffDto
import com.parking.shared.data.api.dto.TariffPlanDto
import com.parking.shared.domain.model.Money
import com.parking.shared.domain.model.VehicleType

/**
 * Tabla del backend para planes de mensualidad — botones Editar / Eliminar.
 */
@Composable
internal fun ApiPlanTable(items: List<TariffPlanDto>, vm: AdminTariffPlanViewModel) {
    val spacing = LocalSpacing.current
    TableHeaderRow(listOf("Plan", "Duración", "Tipo", "Precio", "Precio/Día", "Estado", "Acciones"))
    items.forEach { p ->
        val pricePerDay = if (p.durationDays == 0) 0L else p.priceCents / p.durationDays
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.m, vertical = spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.SemiBold) }
            Box(Modifier.weight(1f)) { Text("${p.durationDays} días", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Box(Modifier.weight(1f)) { Text(p.vehicleType.name) }
            Box(Modifier.weight(1f)) {
                Text(Money(p.priceCents).formatCop(), color = LocalSemanticColors.current.greenAccent, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.weight(1f)) {
                Text(Money(pricePerDay).formatCop(), color = LocalSemanticColors.current.blueAccent)
            }
            Box(Modifier.weight(1f)) {
                StatusPill(
                    label = if (p.enabled) "Activo" else "Inactivo",
                    tone = if (p.enabled) StatusTone.Success else StatusTone.Neutral,
                    showDot = false,
                )
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                OutlinedButton(onClick = { vm.openEdit(p) }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                OutlinedButton(onClick = { vm.delete(p) }) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * Tabla del backend para tarifas especiales — botones Editar / Eliminar.
 */
@Composable
internal fun ApiSpecialTariffTable(items: List<SpecialTariffDto>, vm: AdminSpecialTariffViewModel) {
    val spacing = LocalSpacing.current
    TableHeaderRow(listOf("Nombre", "Regla", "Multiplicador", "Vigencia", "Estado", "Acciones"))
    items.forEach { s ->
        val tone = when {
            s.multiplier > 1 -> StatusTone.Danger
            s.multiplier < 1 -> StatusTone.Info
            else -> StatusTone.Neutral
        }
        val vigencia = when (s.ruleType.name) {
            "DATE_RANGE" -> "${s.dateFromIso ?: ""} → ${s.dateToIso ?: ""}"
            "DAY_OF_WEEK" -> "Días: ${s.dayOfWeekCsv ?: ""}"
            "WEEKEND" -> "Sáb/Dom"
            "HOLIDAY" -> "Festivos del calendario"
            else -> ""
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.m, vertical = spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { Text(s.name, fontWeight = FontWeight.SemiBold) }
            Box(Modifier.weight(1f)) { Text(s.ruleType.name, style = MaterialTheme.typography.bodySmall) }
            Box(Modifier.weight(1f)) {
                StatusPill(label = "${s.multiplier}x", tone = tone, showDot = false)
            }
            Box(Modifier.weight(2f)) {
                Text(vigencia, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.weight(1f)) {
                StatusPill(
                    label = if (s.enabled) "Activa" else "Inactiva",
                    tone = if (s.enabled) StatusTone.Success else StatusTone.Neutral,
                    showDot = false,
                )
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                OutlinedButton(onClick = { vm.openEdit(s) }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                OutlinedButton(onClick = { vm.delete(s) }) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * Tabla de tarifas por hora vivas del backend con botones de acción por fila.
 */
@Composable
internal fun ApiTariffTable(items: List<TariffDto>, vm: AdminTariffViewModel) {
    val spacing = LocalSpacing.current
    TableHeaderRow(listOf("Tipo", "1ª hora", "Siguiente", "Recargo noct.", "Vigencia", "Estado", "Acciones"))
    items.forEach { t ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                Text(vehicleTypeLabel(t.vehicleType), fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.weight(1f)) {
                Text(Money(t.firstHourCents).formatCop(), color = LocalSemanticColors.current.greenAccent)
            }
            Box(Modifier.weight(1f)) {
                Text(Money(t.subsequentHourCents).formatCop())
            }
            Box(Modifier.weight(1f)) {
                Text(
                    "${t.nightSurchargePercent}% (${t.nightFromIso.take(5)}–${t.nightToIso.take(5)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.weight(1f)) {
                val text = if (t.validToMillis == null) "Vigente" else "Cerrada"
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.weight(1f)) {
                val isCurrent = t.validToMillis == null
                StatusPill(
                    label = if (isCurrent) "Vigente" else "Histórica",
                    tone = if (isCurrent) StatusTone.Success else StatusTone.Neutral,
                    showDot = false,
                )
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                OutlinedButton(onClick = { vm.openEdit(t) }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                if (t.validToMillis == null) {
                    OutlinedButton(onClick = { vm.closeTariff(t) }) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

internal fun vehicleTypeLabel(t: VehicleType): String = when (t) {
    VehicleType.CAR -> "Automóvil"
    VehicleType.MOTORCYCLE -> "Motocicleta"
    VehicleType.BICYCLE -> "Bicicleta"
    VehicleType.TRUCK -> "Camioneta"
    VehicleType.BUS -> "Bus"
}

