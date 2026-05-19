package com.parking.app.ui.screens.availability

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.parking.app.ui.components.BackToMenuButton
import com.parking.app.ui.components.CardSectionTitle
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.components.ShadcnCard
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

/**
 * Consulta de Disponibilidad — Port de `ParkingAvailability.tsx`.
 * Header con "última actualización" + refresh, 4 KPIs, filtros, 3 cards de
 * sector y una grilla densa con cuadritos por estado (disponible / ocupado /
 * reservado / mantenimiento).  Refresca cada 30s automáticamente.
 */
@Composable
fun ParkingAvailabilityScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current

    var lastUpdate by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            lastUpdate = Clock.System.now().toEpochMilliseconds()
        }
    }

    val sectors = remember {
        listOf(
            SectorData("A", "Nivel 1", 25),
            SectorData("B", "Nivel 2", 30),
            SectorData("C", "Nivel 3", 35),
        ).map { sector ->
            val spaces = (1..sector.total).map { n ->
                val status = pickStatus(sector.code, n)
                Space(id = "${sector.code}-${n.toString().padStart(2, '0')}", status = status, occupiedHours = if (status == SpaceStatus.Occupied) Random.nextInt(1, 8) else null, plate = if (status == SpaceStatus.Occupied) randomPlate() else null)
            }
            sector.copy(spaces = spaces)
        }
    }

    val totalCap = sectors.sumOf { it.total }
    val totalAvailable = sectors.sumOf { it.spaces.count { sp -> sp.status == SpaceStatus.Available } }
    val totalOccupied = sectors.sumOf { it.spaces.count { sp -> sp.status == SpaceStatus.Occupied } }
    val occPct = if (totalCap == 0) 0 else (100 * totalOccupied / totalCap)

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            SectionHeader(
                title = "Consulta de Disponibilidad",
                description = "Plazas de parqueadero en tiempo real.",
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                        val lastTime = remember(lastUpdate) {
                            val ldt = kotlinx.datetime.Instant.fromEpochMilliseconds(lastUpdate)
                                .toLocalDateTime(runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC))
                            "${ldt.hour.toString().padStart(2, '0')}:${ldt.minute.toString().padStart(2, '0')}:${ldt.second.toString().padStart(2, '0')}"
                        }
                        Text("Última actualización: $lastTime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { lastUpdate = Clock.System.now().toEpochMilliseconds() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(spacing.s))
                            Text("Actualizar")
                        }
                    }
                },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), modifier = Modifier.fillMaxWidth()) {
            AccentKpi(label = "Total Plazas", value = totalCap.toString(), icon = Icons.Filled.Map, palette = AccentPalette.Blue, modifier = Modifier.weight(1f))
            AccentKpi(label = "Disponibles", value = totalAvailable.toString(), icon = Icons.Filled.DirectionsCar, palette = AccentPalette.Green, modifier = Modifier.weight(1f))
            AccentKpi(label = "Ocupadas", value = totalOccupied.toString(), icon = Icons.Filled.Schedule, palette = AccentPalette.Red, modifier = Modifier.weight(1f))
            AccentKpi(label = "Ocupación", value = "$occPct%", icon = Icons.Filled.Percent, palette = AccentPalette.Orange, modifier = Modifier.weight(1f))
        }

        FilterCard(query = query, onQueryChange = { query = it.uppercase() })

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.l), modifier = Modifier.fillMaxWidth()) {
            sectors.forEach { s ->
                SectorOverview(sector = s, modifier = Modifier.weight(1f))
            }
        }

        SpaceGridCard(sectors = sectors, filter = query)
    }
}

private data class SectorData(val code: String, val levelLabel: String, val total: Int, val spaces: List<Space> = emptyList())
private data class Space(val id: String, val status: SpaceStatus, val occupiedHours: Int? = null, val plate: String? = null)
private enum class SpaceStatus { Available, Occupied, Reserved, Maintenance }

private fun pickStatus(sector: String, n: Int): SpaceStatus {
    // Pesos por sector — A es ~70% ocupado, B ~75%, C más libre.
    val r = Random(("$sector-$n").hashCode()).nextDouble()
    return when (sector) {
        "A" -> when { r < 0.70 -> SpaceStatus.Occupied; r < 0.85 -> SpaceStatus.Available; r < 0.95 -> SpaceStatus.Reserved; else -> SpaceStatus.Maintenance }
        "B" -> when { r < 0.75 -> SpaceStatus.Occupied; r < 0.85 -> SpaceStatus.Available; r < 0.95 -> SpaceStatus.Reserved; else -> SpaceStatus.Maintenance }
        else -> when { r < 0.40 -> SpaceStatus.Occupied; r < 0.75 -> SpaceStatus.Available; r < 0.95 -> SpaceStatus.Reserved; else -> SpaceStatus.Maintenance }
    }
}

private fun randomPlate(): String {
    val letters = (1..3).map { ('A'..'Z').random() }.joinToString("")
    val digits = (100..999).random()
    return "$letters-$digits"
}

@Composable
private fun FilterCard(query: String, onQueryChange: (String) -> Unit) {
    val spacing = LocalSpacing.current
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            CardSectionTitle(
                title = "Filtros de Búsqueda",
                description = null,
                icon = Icons.Filled.Tune,
                palette = AccentPalette.Purple,
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Buscar plaza o placa") },
                placeholder = { Text("Ej: A-15 o ABC-123") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectorOverview(sector: SectorData, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    val available = sector.spaces.count { it.status == SpaceStatus.Available }
    val occupied = sector.spaces.count { it.status == SpaceStatus.Occupied }
    val reserved = sector.spaces.count { it.status == SpaceStatus.Reserved }
    val fraction = if (sector.total == 0) 0f else occupied.toFloat() / sector.total
    ShadcnCard(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            Text("Sector ${sector.code} — ${sector.levelLabel}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text("$available de ${sector.total} plazas disponibles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row { Text("Disponibles", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); StatusPill(label = available.toString(), tone = StatusTone.Success, showDot = false) }
            Row { Text("Ocupadas", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); StatusPill(label = occupied.toString(), tone = StatusTone.Danger, showDot = false) }
            Row { Text("Reservadas", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); StatusPill(label = reserved.toString(), tone = StatusTone.Warning, showDot = false) }
            Spacer(Modifier.height(spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ocupación", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = LocalSemanticColors.current.blueAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpaceGridCard(sectors: List<SectorData>, filter: String) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val all = sectors.flatMap { it.spaces }
    val filtered = if (filter.isBlank()) all else all.filter { it.id.contains(filter, true) || (it.plate?.contains(filter, true) == true) }
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            Text("Vista Detallada de Plazas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text("Estado en tiempo real de cada plaza de parqueadero.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Leyenda
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(spacing.m),
            ) {
                LegendDot(semantic.greenAccent, "Disponible")
                LegendDot(semantic.redAccent, "Ocupado")
                LegendDot(semantic.yellowAccent, "Reservado")
                LegendDot(MaterialTheme.colorScheme.onSurfaceVariant, "Mantenimiento")
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                filtered.forEach { sp -> SpaceCell(sp) }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val spacing = LocalSpacing.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(spacing.xs))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SpaceCell(space: Space) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val (bg, border, dot) = when (space.status) {
        SpaceStatus.Available -> Triple(semantic.greenContainer, semantic.greenAccent.copy(alpha = 0.5f), semantic.greenAccent)
        SpaceStatus.Occupied -> Triple(semantic.redContainer, semantic.redAccent.copy(alpha = 0.5f), semantic.redAccent)
        SpaceStatus.Reserved -> Triple(semantic.yellowContainer, semantic.yellowAccent.copy(alpha = 0.5f), semantic.yellowAccent)
        SpaceStatus.Maintenance -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(8.dp))
            .padding(spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.height(spacing.xs))
        Text(space.id, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
        space.plate?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        space.occupiedHours?.let {
            Text("${it}h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Versión sin handlers — se usa cuando se renderiza dentro del shell. */
@Composable
fun ParkingAvailabilityScreen() {
    ParkingAvailabilityScreen(onBack = { })
}
