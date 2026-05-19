package com.parking.app.ui.screens.zones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.SectionTitle
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.components.icon
import com.parking.app.ui.components.label
import com.parking.app.ui.components.occupancyColor
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.domain.model.Zone

/**
 * Vista de zonas en grilla.  Cada zona se pinta con color según ocupación
 * (verde / ámbar / rojo) y muestra los tipos de vehículo permitidos.
 *
 * En producción se suscribe al WebSocket /ws/occupancy del backend.  Aquí
 * lee directo de [com.parking.app.state.AppState.zones] que se actualiza
 * cuando el cajero registra entradas/salidas.
 */
@Composable
fun ZonesScreen() {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current
    val totalCap = app.zones.sumOf { it.capacity }
    val totalOcc = app.zones.sumOf { it.currentOccupancy }
    val overallPct = if (totalCap == 0) 0 else (100 * totalOcc / totalCap)

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        SectionTitle(
            title = "Zonas",
            subtitle = "$totalOcc de $totalCap espacios ocupados — $overallPct% del parqueadero.",
            trailing = {
                StatusPill(
                    label = "${app.zones.size} zonas",
                    tone = StatusTone.Neutral,
                    showDot = false,
                )
            }
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(app.zones, key = { it.id }) { zone -> ZoneCard(zone) }
        }
    }
}

@Composable
private fun ZoneCard(zone: Zone) {
    val spacing = LocalSpacing.current
    val color = occupancyColor(zone.fractionSafe)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(zone.code, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = color)
                }
                Spacer(Modifier.width(spacing.s))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Zona ${zone.code}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text("${zone.currentOccupancy} / ${zone.capacity} ocupados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(label = "${(zone.fractionSafe * 100).toInt()}%", tone = toneFor(zone), showDot = false)
            }
            LinearProgressIndicator(
                progress = { zone.fractionSafe },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Garage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(spacing.xs))
                Text("Permite: ${zone.allowedVehicleTypes.joinToString { it.label() }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val Zone.fractionSafe: Float get() = if (capacity == 0) 0f else (currentOccupancy.toFloat() / capacity).coerceIn(0f, 1f)

private fun toneFor(zone: Zone): StatusTone = when {
    zone.fractionSafe >= 0.9f -> StatusTone.Danger
    zone.fractionSafe >= 0.65f -> StatusTone.Warning
    else -> StatusTone.Success
}
