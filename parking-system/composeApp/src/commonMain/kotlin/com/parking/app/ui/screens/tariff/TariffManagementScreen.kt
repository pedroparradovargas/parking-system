package com.parking.app.ui.screens.tariff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.components.BackToMenuButton
import com.parking.app.ui.components.CardSectionTitle
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.components.ShadcnCard
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.domain.model.Money

/**
 * Gestión de Tarifas — Port de `TariffManagement.tsx`.
 * Tabs: Tarifas por Hora · Planes de Suscripción · Tarifas Especiales.
 * Cada tab muestra una tabla (header + filas con bordes).
 */
@Composable
fun TariffManagementScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    var tab by remember { mutableStateOf(TariffTab.Hourly) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            SectionHeader(
                title = "Gestión de Tarifas",
                description = "Configure precios y políticas del parqueadero.",
            )
        }
        TabBar(active = tab, onChange = { tab = it })
        when (tab) {
            TariffTab.Hourly -> HourlyTab()
            TariffTab.Subscriptions -> SubscriptionsTab()
            TariffTab.Special -> SpecialTab()
        }
        SummaryFooter()
    }
}

private enum class TariffTab(val label: String) { Hourly("Tarifas por Hora"), Subscriptions("Planes de Suscripción"), Special("Tarifas Especiales") }

@Composable
private fun TabBar(active: TariffTab, onChange: (TariffTab) -> Unit) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        TariffTab.entries.forEach { t ->
            val isActive = t == active
            val bg = if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent
            val fg = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .clickable { onChange(t) }
                    .padding(vertical = spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(t.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium), color = fg)
            }
        }
    }
}

private data class HourlyTariff(val vehicleType: String, val rateCents: Long, val description: String)
private data class SubscriptionPlan(val name: String, val days: Int, val priceCents: Long, val discountPct: Int, val active: Boolean)
private data class SpecialRate(val name: String, val multiplier: Double, val description: String, val active: Boolean)

private val hourlyRates = listOf(
    HourlyTariff("Automóvil", 2_500_00L, "Vehículo estándar de 4 ruedas"),
    HourlyTariff("Motocicleta", 1_500_00L, "Vehículo de 2 ruedas"),
    HourlyTariff("Camioneta", 3_500_00L, "Pick-up, SUV y similares"),
    HourlyTariff("Van/Furgón", 4_000_00L, "Vehículos comerciales grandes"),
)
private val subscriptionPlans = listOf(
    SubscriptionPlan("Mensual", 30, 45_000_00L, 0, true),
    SubscriptionPlan("Trimestral", 90, 120_000_00L, 7, true),
    SubscriptionPlan("Semestral", 180, 230_000_00L, 10, true),
    SubscriptionPlan("Anual", 365, 420_000_00L, 17, true),
)
private val specialRates = listOf(
    SpecialRate("Fin de Semana", 1.2, "Sábados y domingos", true),
    SpecialRate("Nocturno", 0.8, "10:00 PM — 6:00 AM", true),
    SpecialRate("Festivos", 1.5, "Días festivos nacionales", false),
)

@Composable
private fun HourlyTab() {
    val spacing = LocalSpacing.current
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            CardSectionTitle(
                title = "Tarifas por Hora",
                description = "Configure las tarifas por tipo de vehículo.",
                icon = Icons.Filled.Schedule,
                palette = AccentPalette.Blue,
            )
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
        }
    }
}

@Composable
private fun SubscriptionsTab() {
    val spacing = LocalSpacing.current
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            CardSectionTitle(
                title = "Planes de Suscripción",
                description = "Gestione los planes para clientes alquilados.",
                icon = Icons.Filled.AttachMoney,
                palette = AccentPalette.Green,
            )
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
                    val carRate = hourlyRates.first { it.vehicleType == "Automóvil" }.rateCents
                    val hourlyTotal = carRate * 24 * p.days
                    val savings = (hourlyTotal - p.priceCents).coerceAtLeast(0L)
                    val pct = if (hourlyTotal == 0L) 0 else (100.0 * savings / hourlyTotal).toInt()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LocalSemanticColors.current.blueContainer)
                            .padding(spacing.m),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        Text(p.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = LocalSemanticColors.current.onBlueContainer)
                        Row { Text("Plan:", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = LocalSemanticColors.current.onBlueContainer); Text(Money(p.priceCents).formatCop(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = LocalSemanticColors.current.onBlueContainer) }
                        Row { Text("Por horas:", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = LocalSemanticColors.current.onBlueContainer); Text(Money(hourlyTotal).formatCop(), style = MaterialTheme.typography.bodySmall, color = LocalSemanticColors.current.onBlueContainer) }
                        Row { Text("Ahorro:", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = LocalSemanticColors.current.onBlueContainer); Text("$pct%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = LocalSemanticColors.current.greenAccent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialTab() {
    val spacing = LocalSpacing.current
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            CardSectionTitle(
                title = "Tarifas Especiales",
                description = "Configure multiplicadores para horarios y fechas especiales.",
                icon = Icons.Filled.Tune,
                palette = AccentPalette.Purple,
            )
            TableHeaderRow(listOf("Nombre", "Multiplicador", "Descripción", "Ejemplo Auto", "Estado"))
            val baseRate = hourlyRates.first { it.vehicleType == "Automóvil" }.rateCents
            specialRates.forEach { r ->
                val price = (baseRate * r.multiplier).toLong()
                val tone = when { r.multiplier > 1 -> StatusTone.Danger; r.multiplier < 1 -> StatusTone.Info; else -> StatusTone.Neutral }
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
private fun SummaryFooter() {
    val spacing = LocalSpacing.current
    val active = subscriptionPlans.count { it.active }
    val activeSpecial = specialRates.count { it.active }
    val minRate = hourlyRates.minOf { it.rateCents }
    ShadcnCard {
        Row(modifier = Modifier.padding(spacing.l), horizontalArrangement = Arrangement.spacedBy(spacing.l)) {
            SummaryStat(value = hourlyRates.size.toString(), label = "Tipos de Vehículo", color = LocalSemanticColors.current.blueAccent, modifier = Modifier.weight(1f))
            SummaryStat(value = active.toString(), label = "Planes Activos", color = LocalSemanticColors.current.greenAccent, modifier = Modifier.weight(1f))
            SummaryStat(value = activeSpecial.toString(), label = "Tarifas Especiales", color = LocalSemanticColors.current.purpleAccent, modifier = Modifier.weight(1f))
            SummaryStat(value = Money(minRate).formatCop(), label = "Tarifa Mínima/Hora", color = LocalSemanticColors.current.orangeAccent, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Table primitives ---

private sealed interface TableCell {
    data class Text(val value: String, val weight: FontWeight, val muted: Boolean = false, val accent: AccentPalette? = null) : TableCell
    data class Badge(val value: String, val tone: StatusTone) : TableCell
}

@Composable
private fun TableHeaderRow(headers: List<String>) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = spacing.m, vertical = spacing.s),
    ) {
        headers.forEach { h ->
            Text(h, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TableRow(cells: List<TableCell>) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = spacing.m, vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEach { c ->
            Box(modifier = Modifier.weight(1f)) {
                when (c) {
                    is TableCell.Text -> {
                        val color = if (c.muted) MaterialTheme.colorScheme.onSurfaceVariant
                            else c.accent?.let { LocalSemanticColors.current.let { sc -> when (it) {
                                AccentPalette.Blue -> sc.blueAccent
                                AccentPalette.Green -> sc.greenAccent
                                AccentPalette.Purple -> sc.purpleAccent
                                AccentPalette.Orange -> sc.orangeAccent
                                AccentPalette.Red -> sc.redAccent
                                AccentPalette.Yellow -> sc.yellowAccent
                            } } } ?: MaterialTheme.colorScheme.onSurface
                        Text(c.value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = c.weight), color = color)
                    }
                    is TableCell.Badge -> StatusPill(label = c.value, tone = c.tone, showDot = false)
                }
            }
        }
    }
}

/** Versión sin handlers — se usa cuando se renderiza dentro del shell. */
@Composable
fun TariffManagementScreen() {
    TariffManagementScreen(onBack = { })
}
