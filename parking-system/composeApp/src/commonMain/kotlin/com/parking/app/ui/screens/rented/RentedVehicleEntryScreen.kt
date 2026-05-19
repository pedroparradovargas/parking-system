package com.parking.app.ui.screens.rented

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parking.app.navigation.Screen
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.AlertBox
import com.parking.app.ui.components.AlertTone
import com.parking.app.ui.components.BackToMenuButton
import com.parking.app.ui.components.CardSectionTitle
import com.parking.app.ui.components.LabelValueRow
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.components.ShadcnCard
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.components.StepBullet
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import kotlinx.coroutines.delay

/** Datos simulados de la lectura de tarjeta — en producción vienen del backend. */
private data class CardClientData(
    val name: String,
    val documentId: String,
    val plan: String,
    val parkingSpace: String,
    val validUntil: String,
)

/**
 * Ingreso de Vehículo — Cliente Alquilado.  Réplica del componente Figma
 * `RentedVehicleEntry.tsx`:
 *   - Header con botón "Volver al Menú" + título.
 *   - Grid 2 cols: lector de tarjeta (azul) | info del vehículo (verde).
 *   - Card Estado del Sistema (3 stats).
 *   - Card Instrucciones (3 pasos numerados).
 */
@Composable
fun RentedVehicleEntryScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    var cardNumber by remember { mutableStateOf("") }
    var licensePlate by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var processingCard by remember { mutableStateOf(false) }
    var processingEntry by remember { mutableStateOf(false) }
    var clientData by remember { mutableStateOf<CardClientData?>(null) }
    var accessGranted by remember { mutableStateOf(false) }

    LaunchedEffect(processingCard) {
        if (processingCard) {
            delay(1500)
            clientData = CardClientData(
                name = "Juan Carlos Pérez",
                documentId = "CC 001234567",
                plan = "Mensual Premium",
                parkingSpace = "A-15",
                validUntil = "15/03/2026",
            )
            cardNumber = "4521-****-****-1234"
            processingCard = false
            isProcessing = false
        }
    }
    LaunchedEffect(processingEntry) {
        if (processingEntry) {
            delay(1200)
            accessGranted = true
            processingEntry = false
            isProcessing = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            SectionHeader(
                title = "Ingreso de Vehículo — Cliente Alquilado",
                description = "Acceso rápido con tarjeta de cliente alquilado.",
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.l), modifier = Modifier.fillMaxWidth()) {
            // Lector de tarjeta
            ShadcnCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    CardSectionTitle(
                        title = "Lectura de Tarjeta",
                        description = "Acerque su tarjeta al lector o ingrese el número manualmente.",
                        icon = Icons.Filled.Shield,
                        palette = AccentPalette.Blue,
                    )
                    val data = clientData
                    if (data == null) {
                        // Slot del lector (estilo dashed-border del Figma).
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LocalSemanticColors.current.blueContainer.copy(alpha = 0.4f))
                                .border(2.dp, LocalSemanticColors.current.blueAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(spacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Filled.CreditCard, contentDescription = null, tint = LocalSemanticColors.current.blueAccent, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(spacing.s))
                            Text(
                                "Acerque su tarjeta al lector",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalSemanticColors.current.onBlueContainer,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(spacing.m))
                            Button(
                                onClick = { isProcessing = true; processingCard = true },
                                enabled = !isProcessing,
                            ) {
                                Text(if (processingCard) "Procesando..." else "Simular Lectura de Tarjeta")
                            }
                        }
                        // Divider "o ingrese manualmente"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                            Text("  o ingrese manualmente  ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        }
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text("Número de Tarjeta") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = { isProcessing = true; processingCard = true },
                            enabled = cardNumber.isNotBlank() && !isProcessing,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                        ) { Text("Validar Tarjeta") }
                    } else {
                        AlertBox(text = "Tarjeta validada correctamente.", icon = Icons.Filled.CheckCircle, tone = AlertTone.Success)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .padding(spacing.m),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs),
                        ) {
                            LabelValueRow("Cliente:", data.name)
                            LabelValueRow("Documento:", data.documentId)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Plan:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                StatusPill(label = data.plan, tone = StatusTone.Info, showDot = false)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Plaza Asignada:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                StatusPill(label = data.parkingSpace, tone = StatusTone.Neutral, showDot = false)
                            }
                            LabelValueRow("Válido hasta:", data.validUntil, accent = true)
                        }
                    }
                }
            }

            // Información del vehículo
            ShadcnCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    CardSectionTitle(
                        title = "Información del Vehículo",
                        description = "Confirme los datos de su vehículo.",
                        icon = Icons.Filled.DirectionsCar,
                        palette = AccentPalette.Green,
                    )
                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it.uppercase().filter(Char::isLetterOrDigit).take(10) },
                        label = { Text("Placa del Vehículo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val data = clientData
                    if (data != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(LocalSemanticColors.current.blueContainer)
                                .padding(spacing.m),
                        ) {
                            Text("Plaza Asignada", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = LocalSemanticColors.current.onBlueContainer)
                            Text(data.parkingSpace, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold), color = LocalSemanticColors.current.blueAccent)
                            Text("Sector A — Nivel 1", style = MaterialTheme.typography.bodySmall, color = LocalSemanticColors.current.onBlueContainer)
                        }
                    }
                    if (accessGranted) {
                        AlertBox(
                            title = "¡Acceso autorizado!",
                            text = "Puede dirigirse a su plaza asignada: ${data?.parkingSpace ?: "—"}",
                            icon = Icons.Filled.CheckCircle,
                            tone = AlertTone.Success,
                        )
                    } else {
                        Button(
                            onClick = { isProcessing = true; processingEntry = true },
                            enabled = data != null && licensePlate.length >= 5 && !isProcessing,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                        ) { Text(if (processingEntry) "Procesando ingreso..." else "Autorizar Ingreso") }
                    }
                }
            }
        }

        // Estado del sistema
        SystemStatusCard()
        // Instrucciones
        InstructionsCard()
    }
}

@Composable
private fun SystemStatusCard() {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current
    val semantic = LocalSemanticColors.current
    val available = (app.zones.sumOf { it.capacity } - app.zones.sumOf { it.currentOccupancy }).coerceAtLeast(0)
    val active = app.activeSessions.size
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            CardSectionTitle(
                title = "Estado del Sistema",
                description = null,
                icon = Icons.Filled.Schedule,
                palette = AccentPalette.Orange,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), modifier = Modifier.fillMaxWidth()) {
                StatusValue(value = available.toString(), label = "Plazas Disponibles", color = semantic.greenAccent, modifier = Modifier.weight(1f))
                StatusValue(value = active.toString(), label = "Clientes Conectados", color = semantic.blueAccent, modifier = Modifier.weight(1f))
                StatusValue(value = "3 min", label = "Tiempo Promedio de Ingreso", color = semantic.orangeAccent, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusValue(value: String, label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InstructionsCard() {
    val spacing = LocalSpacing.current
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            Text("Instrucciones", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            listOf(
                "Acerque su tarjeta de cliente alquilado al lector o ingrese el número manualmente.",
                "Confirme la placa de su vehículo en el sistema.",
                "Una vez autorizado, diríjase directamente a su plaza asignada.",
            ).forEachIndexed { idx, text ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                    StepBullet(number = idx + 1)
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Versión sin handlers — se usa cuando la pantalla se renderiza dentro del shell con tab nav. */
@Composable
fun RentedVehicleEntryScreen() {
    val app = LocalAppState.current
    @Suppress("UNUSED_VARIABLE") val unused = app
    // Reutilizamos el shell de tabs, así que el "Volver" simplemente regresa a Main.
    RentedVehicleEntryScreen(onBack = { /* el tab nav del shell maneja la navegación */ })
}
