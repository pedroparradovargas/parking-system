package com.parking.app.ui.screens.cashier

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.parking.app.ui.components.PlateInput
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.components.VehicleTypeSelector
import com.parking.app.ui.components.label
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.domain.model.ChargeBreakdown
import com.parking.shared.domain.model.Money
import com.parking.shared.domain.model.ParkingSession
import com.parking.shared.domain.model.PlateNumber
import com.parking.shared.domain.model.VehicleType

/**
 * Cards de la pantalla de Caja:
 *  - [EntryCard]: registra entrada nueva.
 *  - [ChargeCard]: busca placa y dispara cobro.
 *
 * Las primitivas compartidas ([ShadcnCard], [CardTitleWithIcon]) viven en
 * `CashierScreen.kt` para que estén disponibles a todo el package.
 */
@Composable
internal fun EntryCard() {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val app = LocalAppState.current
    var plate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(VehicleType.CAR) }
    var lastConfirmation by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            CardTitleWithIcon(
                title = "Nueva entrada",
                description = "Registra un vehículo que entra al parqueadero.",
                palette = AccentPalette.Blue,
                icon = Icons.AutoMirrored.Filled.Login,
            )
            PlateInput(value = plate, onValueChange = { plate = it; validationError = null })
            Spacer(Modifier.height(spacing.xxs))
            Text("Tipo de vehículo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            VehicleTypeSelector(selected = type, onSelect = { type = it })
            validationError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
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
internal fun ChargeCard() {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<ParkingSession?>(null) }
    var preview by remember { mutableStateOf<ChargeBreakdown?>(null) }
    var notFound by remember { mutableStateOf(false) }
    var receipt by remember { mutableStateOf<ChargeBreakdown?>(null) }

    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            CardTitleWithIcon(
                title = "Búsqueda y cobro",
                description = "Busca la placa de un vehículo activo y registra su salida.",
                palette = AccentPalette.Green,
                icon = Icons.Filled.Search,
            )
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
            receipt?.let { r -> ReceiptPanel(r, onPrint = { receipt = null }) }
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
        if (breakdown.withinGrace) {
            Text(
                "Dentro de periodo de gracia — sin cobro.",
                style = MaterialTheme.typography.labelSmall,
                color = LocalSemanticColors.current.onGreenContainer,
            )
        }
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
        Text(
            "Total: ${Money(receipt.totalCents).formatCop()}",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = semantic.onGreenContainer,
        )
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
