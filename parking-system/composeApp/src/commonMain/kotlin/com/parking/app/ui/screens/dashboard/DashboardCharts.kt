package com.parking.app.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.ClosedSessionEntry
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.domain.model.Money
import com.parking.shared.domain.model.VehicleType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Componentes Canvas para Dashboard:
 *  - BarChartByHour: ingresos por hora (24 buckets)
 *  - CountByHourChart: cantidad de cobros por hora
 *  - PeakHoursGrid: 3 cards (mañana/tarde/noche) con % del total
 *  - VehicleTypeDistribution: barras horizontales por tipo
 *  - ActivityList: últimos 15 cobros
 *  - colorForType / typeLabel: helpers de paleta y etiqueta por tipo
 *
 * Todo está marcado `internal` para que DashboardScreen y DashboardTabs lo
 * consuman sin romper el encapsulamiento del paquete.
 */
@Composable
internal fun BarChartByHour(closed: List<ClosedSessionEntry>) {
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
internal fun CountByHourChart(closed: List<ClosedSessionEntry>) {
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
internal fun PeakHoursGrid(closed: List<ClosedSessionEntry>) {
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
internal fun VehicleTypeDistribution(closed: List<ClosedSessionEntry>) {
    val spacing = LocalSpacing.current
    val total = closed.size.coerceAtLeast(1)
    val byType = closed.groupingBy { it.session.vehicleType }.eachCount()
    val all = VehicleType.entries
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
internal fun ActivityList(closed: List<ClosedSessionEntry>) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
        closed.take(15).forEach { entry ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.session.plate.normalized(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text(
                        "${entry.breakdown.billedHours} h facturadas · ${typeLabel(entry.session.vehicleType)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
internal fun colorForType(t: VehicleType): Color {
    val s = LocalSemanticColors.current
    return when (t) {
        VehicleType.CAR -> s.blueAccent
        VehicleType.MOTORCYCLE -> s.greenAccent
        VehicleType.BICYCLE -> s.purpleAccent
        VehicleType.TRUCK -> s.orangeAccent
        VehicleType.BUS -> s.redAccent
    }
}

internal fun typeLabel(t: VehicleType): String = when (t) {
    VehicleType.CAR -> "Automóvil"
    VehicleType.MOTORCYCLE -> "Motocicleta"
    VehicleType.BICYCLE -> "Bicicleta"
    VehicleType.TRUCK -> "Camioneta"
    VehicleType.BUS -> "Bus"
}
