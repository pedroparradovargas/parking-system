package com.parking.app.ui.screens.cashier

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.AccentKpi
import com.parking.app.ui.components.EmptyState
import com.parking.app.ui.components.PlateInput
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.components.VehicleTypeSelector
import com.parking.app.ui.components.icon
import com.parking.app.ui.components.label
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.app.ui.theme.pair
import com.parking.shared.domain.model.ChargeBreakdown
import com.parking.shared.domain.model.Money
import com.parking.shared.domain.model.ParkingSession
import com.parking.shared.domain.model.PlateNumber
import com.parking.shared.domain.model.VehicleType
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

/**
 * Caja — operación de cobro.  Layout estilo Figma:
 *   1. SectionHeader con título y descripción.
 *   2. Strip de 4 KPIs (sesiones activas / placas hoy / ingreso / tiempo promedio).
 *   3. Grid 2x: Nueva entrada / Búsqueda y cobro (izquierda); Sesiones activas (derecha).
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

@Composable
private fun EntryCard() {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val app = LocalAppState.current
    var plate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(VehicleType.CAR) }
    var lastConfirmation by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            CardTitleWithIcon(title = "Nueva entrada", description = "Registra un vehículo que entra al parqueadero.", palette = AccentPalette.Blue, icon = Icons.AutoMirrored.Filled.Login)
            PlateInput(value = plate, onValueChange = { plate = it; validationError = null })
            Spacer(Modifier.height(spacing.xxs))
            Text("Tipo de vehículo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VehicleTypeSelector(selected = type, onSelect = { type = it })
            validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(spacing.xxs))
            Button(
                onClick = {
                    val pn = runCatching { PlateNumber(plate) }.getOrNull()
                    if (pn == null) {
                        validationError = "Placa inválida. Mínimo 5, máximo 10 caracteres alfanuméricos."
                    } else if (app.findActiveByPlate(plate) != null) {
                        validationError = "La placa $plate ya tiene una sesión activa."
                    } else {
                        val session = app.registerEntry(pn, type)
                        lastConfirmation = "${session.plate.normalized()} registrada como ${type.label().lowercase()}."
                        plate = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(spacing.s))
                Text("Registrar entrada", fontWeight = FontWeight.Medium)
            }
            AnimatedVisibility(visible = lastConfirmation != null) {
                lastConfirmation?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(semantic.greenContainer)
                            .padding(horizontal = spacing.s, vertical = spacing.xs),
                    ) {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = semantic.onGreenContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChargeCard() {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val app = LocalAppState.current
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<ParkingSession?>(null) }
    var preview by remember { mutableStateOf<ChargeBreakdown?>(null) }
    var notFound by remember { mutableStateOf(false) }
    var receipt by remember { mutableStateOf<ChargeBreakdown?>(null) }

    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            CardTitleWithIcon(title = "Búsqueda y cobro", description = "Busca la placa de un vehículo activo y registra su salida.", palette = AccentPalette.Green, icon = Icons.Filled.Search)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.uppercase().filter(Char::isLetterOrDigit).take(10); notFound = false },
                    label = { Text("Placa a cobrar") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val session = app.findActiveByPlate(query)
                        found = session
                        preview = session?.let { app.previewCharge(it) }
                        notFound = session == null && query.length >= 5
                    },
                    modifier = Modifier.height(56.dp),
                ) { Text("Buscar") }
            }
            if (notFound) {
                Text("No hay sesión activa para $query.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            val session = found
            val br = preview
            if (session != null && br != null && receipt == null) {
                ChargeBreakdownPanel(session = session, breakdown = br)
                Button(
                    onClick = {
                        receipt = app.closeSession(session)
                        found = null
                        preview = null
                        query = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(spacing.s))
                    Text("Registrar salida y cobrar", fontWeight = FontWeight.Medium)
                }
            }
            receipt?.let { r ->
                ReceiptPanel(r, onPrint = { receipt = null })
            }
        }
    }
}

@Composable
private fun ChargeBreakdownPanel(session: ParkingSession, breakdown: ChargeBreakdown) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(session.plate.normalized(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            StatusPill(label = session.vehicleType.label(), tone = StatusTone.Info)
        }
        BreakdownRow("Tiempo", "${breakdown.billedMinutes} min  (${breakdown.billedHours} h)")
        BreakdownRow("Base", Money(breakdown.baseCents).formatCop())
        if (breakdown.nightSurchargeCents > 0) BreakdownRow("Recargo nocturno", Money(breakdown.nightSurchargeCents).formatCop())
        BreakdownRow("Subtotal", Money(breakdown.subtotalCents).formatCop())
        BreakdownRow("IVA", Money(breakdown.ivaCents).formatCop())
        Spacer(Modifier.height(spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Total", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
            Text(Money(breakdown.totalCents).formatCop(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
        if (breakdown.withinGrace) Text("Dentro de periodo de gracia — sin cobro.", style = MaterialTheme.typography.labelSmall, color = LocalSemanticColors.current.onGreenContainer)
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ReceiptPanel(receipt: ChargeBreakdown, onPrint: () -> Unit) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(semantic.greenContainer)
            .padding(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text("Cobro exitoso", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = semantic.onGreenContainer)
        Text("Total: ${Money(receipt.totalCents).formatCop()}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = semantic.onGreenContainer)
        OutlinedButton(
            onClick = onPrint,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = semantic.onGreenContainer),
        ) {
            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(spacing.s))
            Text("Imprimir recibo")
        }
    }
}

@Composable
private fun ActiveSessionsCard(modifier: Modifier = Modifier) {
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
                CardTitleWithIcon(title = "Sesiones activas", description = "Vehículos actualmente dentro del parqueadero.", palette = AccentPalette.Orange, icon = Icons.Filled.Inbox)
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
                Text("Zona ${session.zoneId?.removePrefix("z-")?.uppercase() ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        StatusPill(label = "Activa", tone = StatusTone.Success)
    }
}

@Composable
private fun ShadcnCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { content() }
}

@Composable
private fun CardTitleWithIcon(title: String, description: String, palette: AccentPalette, icon: androidx.compose.ui.graphics.vector.ImageVector) {
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

private fun formatElapsed(mins: Long): String = when {
    mins < 1 -> "<1 min"
    mins < 60 -> "$mins min"
    else -> "${mins / 60}h ${mins % 60}m"
}

private fun formatAvgMinutes(mins: Long): String = when {
    mins <= 0L -> "—"
    mins < 60 -> "$mins min"
    else -> "${mins / 60}h ${mins % 60}m"
}
