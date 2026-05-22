package com.parking.app.ui.screens.cashier

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.EmptyState
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.components.icon
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.domain.model.ParkingSession
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

/**
 * Card derecha del flujo de caja con la lista de sesiones activas y un tick
 * de refresco cada 30 s para que el contador de minutos transcurridos sea
 * fresco aunque no haya cambios de estado.
 */
@Composable
internal fun ActiveSessionsCard(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current

    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick = Clock.System.now().toEpochMilliseconds()
        }
    }

    ShadcnCard(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardTitleWithIcon(
                    title = "Sesiones activas",
                    description = "Vehículos actualmente dentro del parqueadero.",
                    palette = AccentPalette.Orange,
                    icon = Icons.Filled.Inbox,
                )
                Spacer(Modifier.weight(1f))
                StatusPill(label = "${app.activeSessions.size}", tone = StatusTone.Info, showDot = false)
            }
            Spacer(Modifier.height(spacing.s))
            if (app.activeSessions.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = "Sin sesiones activas",
                    description = "Cuando registres una entrada aparecerá aquí.",
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                    app.activeSessions.forEach { s ->
                        ActiveSessionRow(session = s, refreshKey = tick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSessionRow(session: ParkingSession, @Suppress("UNUSED_PARAMETER") refreshKey: Long) {
    val spacing = LocalSpacing.current
    val now = Clock.System.now()
    val mins = ((now - session.entryAt).inWholeMinutes).coerceAtLeast(0)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(spacing.s),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(session.vehicleType.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(spacing.s))
        Column(modifier = Modifier.weight(1f)) {
            Text(session.plate.normalized(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(spacing.xxs))
                Text(formatElapsed(mins), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(spacing.s))
                Text(
                    "Zona ${session.zoneId?.removePrefix("z-")?.uppercase() ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StatusPill(label = "Activa", tone = StatusTone.Success)
    }
}

private fun formatElapsed(mins: Long): String = when {
    mins < 1 -> "<1 min"
    mins < 60 -> "$mins min"
    else -> "${mins / 60}h ${mins % 60}m"
}
