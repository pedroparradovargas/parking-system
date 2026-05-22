package com.parking.app.ui.screens.cashier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.AccentKpi
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.app.ui.theme.pair
import com.parking.shared.domain.model.Money

/**
 * Caja — operación de cobro.  Layout estilo Figma:
 *   1. SectionHeader con título y descripción.
 *   2. Strip de 4 KPIs (sesiones activas / cobros hoy / ingreso / tiempo promedio).
 *   3. Grid 2x: Nueva entrada / Búsqueda y cobro (izquierda); Sesiones activas (derecha).
 *
 * Las cards de entrada/cobro/recibo viven en `CashierCards.kt` y la card
 * de sesiones activas en `ActiveSessionsCard.kt` para respetar la regla 3
 * (archivos < 300 líneas).
 */
@Composable
fun CashierScreen() {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current

    val activeCount = app.activeSessions.size
    val closedCount = app.closedToday.size
    val totalRevenueCents = remember(closedCount) { app.closedToday.sumOf { it.breakdown.totalCents } }
    val avgMinutes = remember(closedCount) {
        if (app.closedToday.isEmpty()) 0
        else app.closedToday.sumOf { it.breakdown.billedMinutes } / app.closedToday.size
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        SectionHeader(
            title = "Caja — Operación de Cobro",
            description = "Registra entradas, busca placas y cobra salidas. Funciona online y offline.",
        )

        // KPI strip
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), modifier = Modifier.fillMaxWidth()) {
            AccentKpi(label = "Sesiones activas", value = activeCount.toString(), icon = Icons.Filled.DirectionsCar, palette = AccentPalette.Blue, modifier = Modifier.weight(1f))
            AccentKpi(label = "Cobros hoy", value = closedCount.toString(), icon = Icons.Filled.Inbox, palette = AccentPalette.Green, modifier = Modifier.weight(1f))
            AccentKpi(label = "Ingreso del día", value = Money(totalRevenueCents).formatCop(), icon = Icons.Filled.AttachMoney, palette = AccentPalette.Purple, modifier = Modifier.weight(1f))
            AccentKpi(label = "Tiempo promedio", value = formatAvgMinutes(avgMinutes), icon = Icons.Filled.Timer, palette = AccentPalette.Orange, modifier = Modifier.weight(1f))
        }

        // Grid principal
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.l), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(0.55f), verticalArrangement = Arrangement.spacedBy(spacing.l)) {
                EntryCard()
                ChargeCard()
            }
            ActiveSessionsCard(modifier = Modifier.weight(0.45f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Primitives compartidas por todos los archivos del paquete `cashier`.
// ─────────────────────────────────────────────────────────────────────────

@Composable
internal fun ShadcnCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { content() }
}

@Composable
internal fun CardTitleWithIcon(title: String, description: String, palette: AccentPalette, icon: ImageVector) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val (bg, fg) = semantic.pair(palette)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(spacing.s))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatAvgMinutes(mins: Long): String = when {
    mins <= 0L -> "—"
    mins < 60 -> "$mins min"
    else -> "${mins / 60}h ${mins % 60}m"
}
