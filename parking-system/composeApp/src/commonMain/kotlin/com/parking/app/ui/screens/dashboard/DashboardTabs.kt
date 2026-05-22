package com.parking.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.parking.app.state.ClosedSessionEntry
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.LocalSpacing

/**
 * Tabs del Dashboard.  Cada función arma su propia composición usando los
 * charts de `DashboardCharts.kt`.  Los componentes auxiliares `KvRow` /
 * `KvBadgeRow` viven aquí porque sólo se usan dentro de los tabs.
 */
@Composable
internal fun RevenueTab(closed: List<ClosedSessionEntry>) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        ChartCard(
            title = "Ingresos por hora",
            description = "Cobros cerrados durante el día corriente.",
        ) {
            BarChartByHour(closed = closed)
        }
        ChartCard(
            title = "Actividad reciente",
            description = "Últimos cobros procesados por la caja.",
        ) {
            ActivityList(closed = closed)
        }
    }
}

@Composable
internal fun UsageTab(closed: List<ClosedSessionEntry>) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        ChartCard(
            title = "Tráfico por hora",
            description = "Cantidad de salidas registradas por franja horaria.",
        ) {
            CountByHourChart(closed = closed)
        }
        ChartCard(
            title = "Horas pico",
            description = "Períodos con mayor demanda.",
        ) {
            PeakHoursGrid(closed = closed)
        }
    }
}

@Composable
internal fun VehiclesTab(closed: List<ClosedSessionEntry>) {
    ChartCard(
        title = "Distribución por tipo",
        description = "Porcentaje de cada tipo de vehículo en los cobros del día.",
    ) {
        VehicleTypeDistribution(closed = closed)
    }
}

@Composable
internal fun AnalysisTab() {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current
    val totalCap = app.zones.sumOf { it.capacity }
    val totalOcc = app.zones.sumOf { it.currentOccupancy }
    val occPct = if (totalCap == 0) 0 else 100 * totalOcc / totalCap
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.l), modifier = Modifier.fillMaxWidth()) {
        ChartCard(title = "KPIs principales", description = null, modifier = Modifier.weight(1f)) {
            KvRow("Tasa de ocupación", "$occPct%")
            KvRow("Sesiones cerradas hoy", "${app.closedToday.size}")
            KvRow("Sesiones activas", "${app.activeSessions.size}")
            KvRow("Mensualidades vigentes", "23")
        }
        ChartCard(title = "Tendencias", description = null, modifier = Modifier.weight(1f)) {
            KvBadgeRow("Crecimiento mensual", "+18.2%", StatusTone.Success)
            KvBadgeRow("Retención", "87%", StatusTone.Info)
            KvBadgeRow("Ingresos vs meta", "112%", StatusTone.Success)
            KvBadgeRow("Satisfacción", "94%", StatusTone.Success)
        }
    }
}

@Composable
private fun KvRow(label: String, value: String) {
    val spacing = LocalSpacing.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun KvBadgeRow(label: String, value: String, tone: StatusTone) {
    val spacing = LocalSpacing.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        StatusPill(label = value, tone = tone, showDot = false)
    }
}
