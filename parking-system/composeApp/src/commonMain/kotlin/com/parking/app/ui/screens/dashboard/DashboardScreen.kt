package com.parking.app.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.AccentKpi
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.domain.model.Money

/**
 * Reportes y Estadísticas — sigue el layout Figma `ReportsStatistics`:
 *   1. SectionHeader con título "Reportes y Estadísticas".
 *   2. Strip 4 KPIs (ingresos / vehículos / promedio diario / tiempo promedio).
 *   3. Tabs (Ingresos / Uso / Vehículos / Análisis) con contenido distinto.
 *
 * Los tabs viven en `DashboardTabs.kt` y las gráficas en `DashboardCharts.kt`
 * para mantener este archivo por debajo del límite de 300 líneas (Regla 3).
 */
@Composable
fun DashboardScreen() {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current

    val totalRevenueCents = remember(app.closedToday.size) { app.closedToday.sumOf { it.breakdown.totalCents } }
    val totalVehicles = app.closedToday.size + app.activeSessions.size
    val avgDailyRevenue = if (app.closedToday.isEmpty()) 0L else totalRevenueCents / app.closedToday.size
    val avgMinutes = if (app.closedToday.isEmpty()) 0L else app.closedToday.sumOf { it.breakdown.billedMinutes } / app.closedToday.size

    var activeTab by remember { mutableStateOf(ReportTab.Revenue) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        SectionHeader(
            title = "Reportes y Estadísticas",
            description = "Análisis detallado del uso del parqueadero en el día corriente.",
        )

        // KPI strip
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), modifier = Modifier.fillMaxWidth()) {
            AccentKpi(label = "Ingresos totales", value = Money(totalRevenueCents).formatCop(), icon = Icons.Filled.AttachMoney, palette = AccentPalette.Green, modifier = Modifier.weight(1f))
            AccentKpi(label = "Total vehículos", value = totalVehicles.toString(), icon = Icons.Filled.DirectionsCar, palette = AccentPalette.Blue, modifier = Modifier.weight(1f))
            AccentKpi(label = "Promedio por cobro", value = Money(avgDailyRevenue).formatCop(), icon = Icons.AutoMirrored.Filled.TrendingUp, palette = AccentPalette.Purple, modifier = Modifier.weight(1f))
            AccentKpi(label = "Tiempo promedio", value = formatAvgMinutes(avgMinutes), icon = Icons.Filled.Schedule, palette = AccentPalette.Orange, modifier = Modifier.weight(1f))
        }

        TabBar(active = activeTab, onChange = { activeTab = it })

        when (activeTab) {
            ReportTab.Revenue -> RevenueTab(app.closedToday)
            ReportTab.Usage -> UsageTab(app.closedToday)
            ReportTab.Vehicles -> VehiclesTab(app.closedToday)
            ReportTab.Analysis -> AnalysisTab()
        }
    }
}

internal enum class ReportTab(val label: String) { Revenue("Ingresos"), Usage("Uso"), Vehicles("Vehículos"), Analysis("Análisis") }

@Composable
private fun TabBar(active: ReportTab, onChange: (ReportTab) -> Unit) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        ReportTab.entries.forEach { tab ->
            val isActive = tab == active
            val bg = if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent
            val fg = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .clickable { onChange(tab) }
                    .padding(vertical = spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(tab.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium), color = fg)
            }
        }
    }
}

/** Card de gráfico reutilizada por todos los tabs. */
@Composable
internal fun ChartCard(title: String, description: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(spacing.l)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(spacing.m))
            content()
        }
    }
}

private fun formatAvgMinutes(mins: Long): String = when {
    mins <= 0L -> "—"
    mins < 60 -> "$mins min"
    else -> "${mins / 60}h ${mins % 60}m"
}
