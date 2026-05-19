package com.parking.app.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PieChart
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.ClosedSessionEntry
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.AccentKpi
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.domain.model.Money
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Reportes y Estadísticas — sigue el layout Figma `ReportsStatistics`:
 *   1. SectionHeader con título "Reportes y Estadísticas".
 *   2. Strip 4 KPIs (ingresos / vehículos / promedio diario / tiempo promedio).
 *   3. Tabs (Ingresos / Uso / Vehículos / Análisis) con contenido distinto.
 *
 * En esta fase implementamos un TabBar manual (no usamos M3 TabRow para
 * controlar el estilo shadcn al detalle).  Las gráficas son Canvas-drawn.
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

private enum class ReportTab(val label: String) { Revenue("Ingresos"), Usage("Uso"), Vehicles("Vehículos"), Analysis("Análisis") }

@Composable
private fun TabBar(active: ReportTab, onChange: (ReportTab) -> Unit) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
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

@Composable
private fun RevenueTab(closed: List<ClosedSessionEntry>) {
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
private fun UsageTab(closed: List<ClosedSessionEntry>) {
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
private fun VehiclesTab(closed: List<ClosedSessionEntry>) {
    val spacing = LocalSpacing.current
    ChartCard(
        title = "Distribución por tipo",
        description = "Porcentaje de cada tipo de vehículo en los cobros del día.",
    ) {
        VehicleTypeDistribution(closed = closed)
    }
}

@Composable
private fun AnalysisTab() {
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
private fun ChartCard(title: String, description: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
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

@Composable
private fun BarChartByHour(closed: List<ClosedSessionEntry>) {
    val tz = remember { runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC) }
    val buckets = LongArray(24)
    closed.forEach { entry ->
        val exit = entry.session.exitAt ?: return@forEach
        val h = exit.toLocalDateTime(tz).hour
        buckets[h] = buckets[h] + entry.breakdown.totalCents
    }
    val maxValue = (buckets.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val barColor = LocalSemanticColors.current.blueAccent
    val barAccent = LocalSemanticColors.current.blueContainer
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val w = size.width
            val h = size.height
            val padBottom = 24f
            val padLeft = 32f
            val chartH = h - padBottom
            val chartW = w - padLeft
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val y = chartH - (chartH * i / gridSteps)
                drawLine(color = outlineColor, start = Offset(padLeft, y), end = Offset(w, y), strokeWidth = 1f)
            }
            val n = 24
            val slotW = chartW / n
            val barW = slotW * 0.6f
            for (i in 0 until n) {
                val value = buckets[i].toFloat()
                val barH = (value / maxValue.toFloat()) * chartH
                if (barH <= 0) continue
                val x = padLeft + i * slotW + (slotW - barW) / 2f
                val y = chartH - barH
                drawRect(color = barAccent, topLeft = Offset(x, y), size = Size(barW, barH))
                drawRect(color = barColor, topLeft = Offset(x, y), size = Size(barW, barH.coerceAtMost(4f)))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 32.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18, 23).forEach { h ->
                Text("${h.toString().padStart(2, '0')}h", style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
        }
    }
}

@Composable
private fun CountByHourChart(closed: List<ClosedSessionEntry>) {
    val tz = remember { runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC) }
    val buckets = IntArray(24)
    closed.forEach { entry ->
        val exit = entry.session.exitAt ?: return@forEach
        buckets[exit.toLocalDateTime(tz).hour]++
    }
    val maxValue = (buckets.maxOrNull() ?: 0).coerceAtLeast(1)
    val barColor = LocalSemanticColors.current.purpleAccent
    val barAccent = LocalSemanticColors.current.purpleContainer
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val w = size.width
            val h = size.height
            val padBottom = 24f
            val padLeft = 32f
            val chartH = h - padBottom
            val chartW = w - padLeft
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val y = chartH - (chartH * i / gridSteps)
                drawLine(color = outlineColor, start = Offset(padLeft, y), end = Offset(w, y), strokeWidth = 1f)
            }
            val n = 24
            val slotW = chartW / n
            val barW = slotW * 0.6f
            for (i in 0 until n) {
                val value = buckets[i].toFloat()
                val barH = (value / maxValue.toFloat()) * chartH
                if (barH <= 0) continue
                val x = padLeft + i * slotW + (slotW - barW) / 2f
                val y = chartH - barH
                drawRect(color = barAccent, topLeft = Offset(x, y), size = Size(barW, barH))
                drawRect(color = barColor, topLeft = Offset(x, y), size = Size(barW, barH.coerceAtMost(4f)))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 32.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, 6, 12, 18, 23).forEach { h ->
                Text("${h.toString().padStart(2, '0')}h", style = MaterialTheme.typography.labelSmall, color = labelColor)
            }
        }
    }
}

@Composable
private fun PeakHoursGrid(closed: List<ClosedSessionEntry>) {
    val spacing = LocalSpacing.current
    val tz = remember { runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC) }
    val buckets = IntArray(24)
    closed.forEach { e -> e.session.exitAt?.let { buckets[it.toLocalDateTime(tz).hour]++ } }
    val total = buckets.sum().coerceAtLeast(1)
    val periods = listOf(
        Triple("Mañana", 6..11, AccentPalette.Blue),
        Triple("Tarde", 12..17, AccentPalette.Orange),
        Triple("Noche", 18..23, AccentPalette.Purple),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), modifier = Modifier.fillMaxWidth()) {
        periods.forEach { (label, range, palette) ->
            val count = range.sumOf { buckets[it] }
            val pct = 100 * count / total
            val (bg, fg) = LocalSemanticColors.current.let { sc ->
                when (palette) {
                    AccentPalette.Blue -> sc.blueContainer to sc.blueAccent
                    AccentPalette.Orange -> sc.orangeContainer to sc.orangeAccent
                    AccentPalette.Purple -> sc.purpleContainer to sc.purpleAccent
                    else -> sc.blueContainer to sc.blueAccent
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .padding(spacing.m),
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = fg)
                Text("${range.first}h – ${range.last}h", style = MaterialTheme.typography.bodySmall, color = fg)
                Spacer(Modifier.height(spacing.s))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$pct%", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = fg)
                    Spacer(Modifier.weight(1f))
                    StatusPill(label = "$count cobros", tone = StatusTone.Neutral, showDot = false)
                }
            }
        }
    }
}

@Composable
private fun VehicleTypeDistribution(closed: List<ClosedSessionEntry>) {
    val spacing = LocalSpacing.current
    val total = closed.size.coerceAtLeast(1)
    val byType = closed.groupingBy { it.session.vehicleType }.eachCount()
    val all = com.parking.shared.domain.model.VehicleType.entries
    Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
        all.forEach { t ->
            val count = byType[t] ?: 0
            val pct = 100 * count / total
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colorForType(t)))
                Spacer(Modifier.width(spacing.s))
                Text(typeLabel(t), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("$count", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Spacer(Modifier.width(spacing.s))
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(modifier = Modifier.fillMaxWidth(pct / 100f).height(8.dp).background(colorForType(t)))
                }
                Spacer(Modifier.width(spacing.s))
                Text("$pct%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun colorForType(t: com.parking.shared.domain.model.VehicleType): Color {
    val s = LocalSemanticColors.current
    return when (t) {
        com.parking.shared.domain.model.VehicleType.CAR -> s.blueAccent
        com.parking.shared.domain.model.VehicleType.MOTORCYCLE -> s.greenAccent
        com.parking.shared.domain.model.VehicleType.BICYCLE -> s.purpleAccent
        com.parking.shared.domain.model.VehicleType.TRUCK -> s.orangeAccent
        com.parking.shared.domain.model.VehicleType.BUS -> s.redAccent
    }
}

private fun typeLabel(t: com.parking.shared.domain.model.VehicleType): String = when (t) {
    com.parking.shared.domain.model.VehicleType.CAR -> "Automóvil"
    com.parking.shared.domain.model.VehicleType.MOTORCYCLE -> "Motocicleta"
    com.parking.shared.domain.model.VehicleType.BICYCLE -> "Bicicleta"
    com.parking.shared.domain.model.VehicleType.TRUCK -> "Camioneta"
    com.parking.shared.domain.model.VehicleType.BUS -> "Bus"
}

@Composable
private fun ActivityList(closed: List<ClosedSessionEntry>) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
        closed.take(15).forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.session.plate.normalized(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text("${entry.breakdown.billedHours} h facturadas · ${typeLabel(entry.session.vehicleType)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    Money(entry.breakdown.totalCents).formatCop(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = LocalSemanticColors.current.greenAccent,
                )
            }
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

private fun formatAvgMinutes(mins: Long): String = when {
    mins <= 0L -> "—"
    mins < 60 -> "$mins min"
    else -> "${mins / 60}h ${mins % 60}m"
}
