package com.parking.app.ui.screens.tariff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.components.CardSectionTitle
import com.parking.app.ui.components.ShadcnCard
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.dto.TariffDto
import com.parking.shared.domain.model.Money
import com.parking.shared.domain.model.VehicleType

/**
 * Las 3 tabs de TariffManagementScreen + el footer resumen.  Cada tab arma
 * una tabla con sus filas hardcoded (la persistencia + edición vienen en la
 * Fase B del plan del módulo Admin).
 */
/**
 * Tab Tarifas por Hora.  Si recibe un [vm] no nulo, opera contra el backend
 * (lista vigentes + botones Editar/Cerrar + diálogo crear).  Si [vm] es null,
 * cae al render hardcoded (modo demo offline-only).
 */
@Composable
internal fun HourlyTab(vm: AdminTariffViewModel? = null) {
    val spacing = LocalSpacing.current
    // Carga inicial cuando el VM se monta por primera vez.
    if (vm != null) {
        LaunchedEffect(vm) { vm.reload() }
    }

    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    CardSectionTitle(
                        title = "Tarifas por Hora",
                        description = "Configure las tarifas por tipo de vehículo.",
                        icon = Icons.Filled.Schedule,
                        palette = AccentPalette.Blue,
                    )
                }
                if (vm != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text("Histórico", style = MaterialTheme.typography.labelMedium)
                        Switch(checked = vm.historic.value, onCheckedChange = { vm.toggleHistoric() })
                        Spacer(Modifier.width(spacing.s))
                        Button(onClick = { vm.openCreate() }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(spacing.xs))
                            Text("Nueva")
                        }
                    }
                }
            }

            vm?.error?.value?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            val apiItems = vm?.items?.value ?: emptyList()
            if (vm == null || apiItems.isEmpty() && vm.error.value != null) {
                // Render hardcoded (modo demo o backend caído).
                TableHeaderRow(listOf("Tipo de Vehículo", "Tarifa/Hora", "Descripción", "Diaria (24h)"))
                hourlyRates.forEach { t ->
                    TableRow(
                        cells = listOf(
                            TableCell.Text(t.vehicleType, FontWeight.SemiBold),
                            TableCell.Text(Money(t.rateCents).formatCop(), FontWeight.SemiBold, accent = AccentPalette.Green),
                            TableCell.Text(t.description, FontWeight.Normal, muted = true),
                            TableCell.Text(Money(t.rateCents * 24).formatCop(), FontWeight.SemiBold, accent = AccentPalette.Blue),
                        ),
                    )
                }
            } else {
                // Render con datos del backend + acciones.
                ApiTariffTable(items = apiItems, vm = vm)
            }
        }
    }
}

@Composable
internal fun SubscriptionsTab(vm: AdminTariffPlanViewModel? = null) {
    val spacing = LocalSpacing.current
    if (vm != null) LaunchedEffect(vm) { vm.reload() }

    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    CardSectionTitle(
                        title = "Planes de Suscripción",
                        description = "Gestione los planes para clientes alquilados.",
                        icon = Icons.Filled.AttachMoney,
                        palette = AccentPalette.Green,
                    )
                }
                if (vm != null) {
                    Button(onClick = { vm.openCreate() }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(spacing.xs))
                        Text("Nuevo plan")
                    }
                }
            }
            vm?.error?.value?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            val apiItems = vm?.items?.value ?: emptyList()
            if (vm == null || (apiItems.isEmpty() && vm.error.value != null)) {
                // Fallback hardcoded (modo demo).
                TableHeaderRow(listOf("Plan", "Duración", "Precio", "Descuento", "Precio/Día", "Estado"))
                subscriptionPlans.forEach { p ->
                    val pricePerDay = if (p.days == 0) 0L else p.priceCents / p.days
                    TableRow(
                        cells = listOf(
                            TableCell.Text(p.name, FontWeight.SemiBold),
                            TableCell.Text("${p.days} días", FontWeight.Normal, muted = true),
                            TableCell.Text(Money(p.priceCents).formatCop(), FontWeight.SemiBold, accent = AccentPalette.Green),
                            TableCell.Badge(if (p.discountPct > 0) "${p.discountPct}% OFF" else "Sin descuento", if (p.discountPct > 0) StatusTone.Success else StatusTone.Neutral),
                            TableCell.Text(Money(pricePerDay).formatCop(), FontWeight.Normal, accent = AccentPalette.Blue),
                            TableCell.Badge(if (p.active) "Activo" else "Inactivo", if (p.active) StatusTone.Success else StatusTone.Neutral),
                        ),
                    )
                }
            } else {
                ApiPlanTable(apiItems, vm)
            }
        }
    }
    Spacer(Modifier.height(spacing.l))
    // Comparación
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            Text("Comparación de Planes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text("Análisis de rentabilidad vs. tarifas por hora.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), modifier = Modifier.fillMaxWidth()) {
                subscriptionPlans.forEach { p ->
                    PlanSavingsCard(p)
                }
            }
        }
    }
}

@Composable
private fun PlanSavingsCard(p: SubscriptionPlan) {
    val spacing = LocalSpacing.current
    val sc = LocalSemanticColors.current
    val carRate = hourlyRates.first { it.vehicleType == "Automóvil" }.rateCents
    val hourlyTotal = carRate * 24 * p.days
    val savings = (hourlyTotal - p.priceCents).coerceAtLeast(0L)
    val pct = if (hourlyTotal == 0L) 0 else (100.0 * savings / hourlyTotal).toInt()
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(sc.blueContainer)
            .padding(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(p.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = sc.onBlueContainer)
        SavingsRow("Plan:", Money(p.priceCents).formatCop(), FontWeight.SemiBold, sc.onBlueContainer)
        SavingsRow("Por horas:", Money(hourlyTotal).formatCop(), FontWeight.Normal, sc.onBlueContainer)
        SavingsRow("Ahorro:", "$pct%", FontWeight.Bold, sc.greenAccent)
    }
}

@Composable
private fun SavingsRow(label: String, value: String, weight: FontWeight, color: Color) {
    Row {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = color)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = weight), color = color)
    }
}

@Composable
internal fun SpecialTab(vm: AdminSpecialTariffViewModel? = null) {
    val spacing = LocalSpacing.current
    if (vm != null) LaunchedEffect(vm) { vm.reload() }
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    CardSectionTitle(
                        title = "Tarifas Especiales",
                        description = "Configure multiplicadores para horarios y fechas especiales.",
                        icon = Icons.Filled.Tune,
                        palette = AccentPalette.Purple,
                    )
                }
                if (vm != null) {
                    Button(onClick = { vm.openCreate() }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(spacing.xs))
                        Text("Nueva")
                    }
                }
            }
            vm?.error?.value?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            val apiItems = vm?.items?.value ?: emptyList()
            if (vm != null && (apiItems.isNotEmpty() || vm.error.value == null)) {
                ApiSpecialTariffTable(apiItems, vm)
                return@Column
            }
            // Fallback hardcoded.
            TableHeaderRow(listOf("Nombre", "Multiplicador", "Descripción", "Ejemplo Auto", "Estado"))
            val baseRate = hourlyRates.first { it.vehicleType == "Automóvil" }.rateCents
            specialRates.forEach { r ->
                val price = (baseRate * r.multiplier).toLong()
                val tone = when {
                    r.multiplier > 1 -> StatusTone.Danger
                    r.multiplier < 1 -> StatusTone.Info
                    else -> StatusTone.Neutral
                }
                TableRow(
                    cells = listOf(
                        TableCell.Text(r.name, FontWeight.SemiBold),
                        TableCell.Badge("${r.multiplier}x", tone),
                        TableCell.Text(r.description, FontWeight.Normal, muted = true),
                        TableCell.Text("${Money(price).formatCop()}/hora", FontWeight.SemiBold),
                        TableCell.Badge(if (r.active) "Activo" else "Inactivo", if (r.active) StatusTone.Success else StatusTone.Neutral),
                    ),
                )
            }
        }
    }
}

@Composable
internal fun SummaryFooter() {
    val spacing = LocalSpacing.current
    val sc = LocalSemanticColors.current
    val active = subscriptionPlans.count { it.active }
    val activeSpecial = specialRates.count { it.active }
    val minRate = hourlyRates.minOf { it.rateCents }
    ShadcnCard {
        Row(modifier = Modifier.padding(spacing.l), horizontalArrangement = Arrangement.spacedBy(spacing.l)) {
            SummaryStat(hourlyRates.size.toString(), "Tipos de Vehículo", sc.blueAccent, Modifier.weight(1f))
            SummaryStat(active.toString(), "Planes Activos", sc.greenAccent, Modifier.weight(1f))
            SummaryStat(activeSpecial.toString(), "Tarifas Especiales", sc.purpleAccent, Modifier.weight(1f))
            SummaryStat(Money(minRate).formatCop(), "Tarifa Mínima/Hora", sc.orangeAccent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
