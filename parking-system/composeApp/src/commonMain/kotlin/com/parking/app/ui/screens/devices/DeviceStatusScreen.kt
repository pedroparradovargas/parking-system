package com.parking.app.ui.screens.devices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.components.BackToMenuButton
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.LocalSpacing

/**
 * Pantalla Admin > Periféricos.  Las acciones reales (imprimir, abrir cajón)
 * solo funcionan en la plataforma desktop (JVM) — en otras plataformas los
 * botones quedan deshabilitados con un tooltip indicativo.
 *
 * La integración real se delega a [DevicePort] (expect/actual): desktopMain
 * lo conecta a `ThermalPrinter` / `CashDrawer` / `BarcodeScanner`; las demás
 * plataformas devuelven `DeviceStatus.Unavailable`.
 */
@Composable
fun DeviceStatusScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    val port = remember { DevicePort() }
    var lastTestMsg by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            Box(modifier = Modifier.weight(1f)) {
                SectionHeader(
                    title = "Periféricos POS",
                    description = "Estado y test de impresora térmica, cajón monedero, lector de placas y datafono.",
                )
            }
        }

        lastTestMsg?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }

        DeviceCard(
            icon = Icons.Filled.Print,
            title = "Impresora térmica (ESC/POS)",
            description = "Imprime recibos y tiquetes vía USB o red.",
            status = port.printerStatus(),
            actionLabel = "Imprimir prueba",
            onAction = { lastTestMsg = port.printTest() },
        )

        DeviceCard(
            icon = Icons.Filled.Inbox,
            title = "Cajón monedero",
            description = "Se abre por comando ESC/POS de la impresora.",
            status = port.cashDrawerStatus(),
            actionLabel = "Abrir cajón",
            onAction = { lastTestMsg = port.openCashDrawer() },
        )

        DeviceCard(
            icon = Icons.Filled.QrCodeScanner,
            title = "Lector HID (placa/QR)",
            description = "Escáner que escribe la placa como si fuera teclado.",
            status = port.scannerStatus(),
            actionLabel = "Esperar lectura",
            onAction = { lastTestMsg = port.scanOnce() },
        )

        DeviceCard(
            icon = Icons.Filled.CreditCard,
            title = "Datafono",
            description = "Integración pendiente (v2).  Hoy se registran pagos manualmente.",
            status = DeviceStatus.Unavailable,
            actionLabel = "—",
            onAction = { },
        )
    }
}

@Composable
private fun DeviceCard(
    icon: ImageVector,
    title: String,
    description: String,
    status: DeviceStatus,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(spacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.width(spacing.s))
                    when (status) {
                        DeviceStatus.Ready -> StatusPill(label = "Listo", tone = StatusTone.Success, showDot = false)
                        DeviceStatus.Busy -> StatusPill(label = "Ocupado", tone = StatusTone.Warning, showDot = false)
                        DeviceStatus.Error -> StatusPill(label = "Error", tone = StatusTone.Danger, showDot = false)
                        DeviceStatus.Unavailable -> StatusPill(label = "No disponible", tone = StatusTone.Neutral, showDot = false)
                    }
                }
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(
                onClick = onAction,
                enabled = status == DeviceStatus.Ready,
            ) {
                Icon(Icons.Filled.Cable, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(spacing.xs))
                Text(actionLabel)
            }
        }
    }
}

/** Estado de un periférico. */
enum class DeviceStatus { Ready, Busy, Error, Unavailable }

/**
 * Adaptador `expect/actual` a las clases de periféricos (solo Desktop tiene
 * implementación real con `javax.print`).  En Web/Android/iOS retorna
 * `Unavailable` y los botones quedan deshabilitados.
 */
expect class DevicePort() {
    fun printerStatus(): DeviceStatus
    fun cashDrawerStatus(): DeviceStatus
    fun scannerStatus(): DeviceStatus
    fun printTest(): String
    fun openCashDrawer(): String
    fun scanOnce(): String
}
