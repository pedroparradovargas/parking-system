package com.parking.app.ui.screens.payment

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
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
import com.parking.shared.domain.model.Money
import kotlinx.coroutines.delay

/**
 * Pago de Tarifa — Cliente Eventual.  Port de `PaymentInterface.tsx`:
 *   - Header con título.
 *   - Grid 2 cols: resumen del ticket | formulario de pago.
 *   - Si pago completado: comprobante + instrucciones de salida.
 *
 * Mock data del ticket — en producción llega del flujo `EventualVehicleEntry`.
 */
@Composable
fun PaymentInterfaceScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    var cardholderName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryMonth by remember { mutableStateOf("") }
    var expiryYear by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var paymentCompleted by remember { mutableStateOf(false) }

    val ticket = remember {
        TicketInfo(
            ticketNumber = "TK456789",
            licensePlate = "ABC-123",
            entryTime = "14:30",
            exitTime = "16:45",
            duration = "2h 15min",
            parkingSpace = "B-23",
            baseCents = 5_000_00L,
            additionalCents = 1_250_00L,
        )
    }

    LaunchedEffect(isProcessing) {
        if (isProcessing && !paymentCompleted) {
            delay(2500)
            paymentCompleted = true
            isProcessing = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack, label = "Volver")
            SectionHeader(
                title = "Pago de Tarifa — Cliente Eventual",
                description = "Procese su pago de parqueadero de forma segura.",
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.l), modifier = Modifier.fillMaxWidth()) {
            TicketSummaryCard(ticket, modifier = Modifier.weight(1f))
            if (!paymentCompleted) {
                PaymentFormCard(
                    ticket = ticket,
                    cardholderName = cardholderName,
                    onCardholderNameChange = { cardholderName = it.uppercase() },
                    cardNumber = cardNumber,
                    onCardNumberChange = { value ->
                        // Formatea: 1234 5678 9012 3456
                        val clean = value.filter(Char::isDigit).take(16)
                        cardNumber = clean.chunked(4).joinToString(" ")
                    },
                    expiryMonth = expiryMonth,
                    onExpiryMonthChange = { expiryMonth = it.filter(Char::isDigit).take(2) },
                    expiryYear = expiryYear,
                    onExpiryYearChange = { expiryYear = it.filter(Char::isDigit).take(4) },
                    cvv = cvv,
                    onCvvChange = { cvv = it.filter(Char::isDigit).take(4) },
                    isProcessing = isProcessing,
                    onSubmit = { isProcessing = true },
                    modifier = Modifier.weight(1f),
                )
            } else {
                PaymentSuccessCard(ticket = ticket, onFinish = onBack, modifier = Modifier.weight(1f))
            }
        }

        if (paymentCompleted) ExitInstructionsCard(ticket.parkingSpace)
    }
}

private data class TicketInfo(
    val ticketNumber: String,
    val licensePlate: String,
    val entryTime: String,
    val exitTime: String,
    val duration: String,
    val parkingSpace: String,
    val baseCents: Long,
    val additionalCents: Long,
) {
    val totalCents: Long get() = baseCents + additionalCents
}

@Composable
private fun TicketSummaryCard(ticket: TicketInfo, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    ShadcnCard(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            CardSectionTitle(
                title = "Resumen del Ticket",
                description = "Verifique la información de su estacionamiento.",
                icon = Icons.Filled.Receipt,
                palette = AccentPalette.Blue,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(spacing.m),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatusPill(label = "Ticket: ${ticket.ticketNumber}", tone = StatusTone.Neutral, showDot = false)
                Spacer(Modifier.height(spacing.s))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    LabelValueRow("Placa:", ticket.licensePlate)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Plaza:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        StatusPill(label = ticket.parkingSpace, tone = StatusTone.Info, showDot = false)
                    }
                    LabelValueRow("Entrada:", ticket.entryTime)
                    LabelValueRow("Salida:", ticket.exitTime)
                    LabelValueRow("Tiempo Total:", ticket.duration)
                }
            }
            // Desglose
            Text("Desglose del Pago", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            LabelValueRow("Tarifa base (2 horas):", Money(ticket.baseCents).formatCop())
            LabelValueRow("Tiempo adicional (15 min):", Money(ticket.additionalCents).formatCop())
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total a Pagar:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                Text(Money(ticket.totalCents).formatCop(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = LocalSemanticColors.current.greenAccent)
            }
        }
    }
}

@Composable
private fun PaymentFormCard(
    ticket: TicketInfo,
    cardholderName: String,
    onCardholderNameChange: (String) -> Unit,
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    expiryMonth: String,
    onExpiryMonthChange: (String) -> Unit,
    expiryYear: String,
    onExpiryYearChange: (String) -> Unit,
    cvv: String,
    onCvvChange: (String) -> Unit,
    isProcessing: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val canSubmit = cardholderName.isNotBlank() && cardNumber.filter { it.isDigit() }.length >= 13 &&
        expiryMonth.length == 2 && expiryYear.length == 4 && cvv.length >= 3 && !isProcessing

    ShadcnCard(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            CardSectionTitle(
                title = "Información de Pago",
                description = "Ingrese los datos de su tarjeta de crédito o débito.",
                icon = Icons.Filled.CreditCard,
                palette = AccentPalette.Green,
            )
            OutlinedTextField(
                value = cardholderName,
                onValueChange = onCardholderNameChange,
                label = { Text("Nombre del Titular *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = cardNumber,
                onValueChange = onCardNumberChange,
                label = { Text("Número de Tarjeta *") },
                placeholder = { Text("1234 5678 9012 3456") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                OutlinedTextField(
                    value = expiryMonth,
                    onValueChange = onExpiryMonthChange,
                    label = { Text("Mes *") },
                    placeholder = { Text("MM") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = expiryYear,
                    onValueChange = onExpiryYearChange,
                    label = { Text("Año *") },
                    placeholder = { Text("YYYY") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = cvv,
                    onValueChange = onCvvChange,
                    label = { Text("CVV *") },
                    placeholder = { Text("123") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f),
                )
            }
            AlertBox(
                title = "Pago Seguro",
                text = "Su información está protegida con encriptación SSL de 256 bits. No almacenamos datos de tarjetas de crédito.",
                icon = Icons.Filled.Lock,
                tone = AlertTone.Info,
            )
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(
                    if (isProcessing) "Procesando pago..." else "Pagar ${Money(ticket.totalCents).formatCop()}",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PaymentSuccessCard(ticket: TicketInfo, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val now = remember { kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString().takeLast(8) }
    ShadcnCard(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(50.dp)).background(semantic.greenContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = semantic.greenAccent, modifier = Modifier.size(36.dp))
            }
            AlertBox(
                title = "¡Pago procesado exitosamente!",
                text = "Su vehículo está autorizado para salir.",
                icon = Icons.Filled.CheckCircle,
                tone = AlertTone.Success,
            )
            // Comprobante
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(10.dp))
                    .padding(spacing.m),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text("Comprobante de Pago", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                LabelValueRow("Transacción:", "TXN$now")
                LabelValueRow("Monto:", Money(ticket.totalCents).formatCop(), accent = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { /* print */ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(spacing.s))
                    Text("Imprimir Comprobante")
                }
                Button(onClick = onFinish, modifier = Modifier.weight(1f)) { Text("Finalizar") }
            }
        }
    }
}

@Composable
private fun ExitInstructionsCard(parkingSpace: String) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            CardSectionTitle(
                title = "Instrucciones de Salida",
                description = null,
                icon = Icons.Filled.Schedule,
                palette = AccentPalette.Orange,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(semantic.orangeContainer)
                    .padding(spacing.m),
                verticalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = semantic.onOrangeContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(spacing.s))
                    Text("Tiempo de Gracia: 15 minutos", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = semantic.onOrangeContainer)
                }
                Text(
                    "Tiene 15 minutos desde el momento del pago para salir del parqueadero sin cargos adicionales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.onOrangeContainer,
                )
                listOf(
                    "Diríjase a su vehículo en la plaza $parkingSpace.",
                    "Presente su ticket y comprobante de pago en la salida.",
                    "El sistema validará automáticamente su pago y abrirá la barrera.",
                ).forEachIndexed { idx, text ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                        StepBullet(number = idx + 1, palette = AccentPalette.Orange)
                        Text(text, style = MaterialTheme.typography.bodySmall, color = semantic.onOrangeContainer, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Versión sin handlers — usada cuando se renderiza dentro del shell. */
@Composable
fun PaymentInterfaceScreen() {
    PaymentInterfaceScreen(onBack = { /* navegación por tab nav */ })
}
