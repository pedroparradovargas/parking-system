package com.parking.app.ui.screens.reservation

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.components.SectionTitle
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import io.github.alexzhirkevich.qrose.options.QrShapes
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.circle
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

/**
 * Vista del QR de pago.  Se renderiza con la librería [qrose] — funciona
 * idéntico en desktop, móvil y web (wasmJs).
 *
 * El contenido del QR es un placeholder con el reservationId.  En producción
 * sería un deeplink a la pasarela de pago (PSE, Nequi, etc.).
 */
@Composable
fun PaymentQrScreen(reservationId: String, onDone: () -> Unit) {
    val spacing = LocalSpacing.current
    val payload = "parking://pay?ref=$reservationId"
    val painter = rememberQrCodePainter(
        data = payload,
        shapes = QrShapes(
            ball = QrBallShape.circle(),
            frame = QrFrameShape.circle(),
            darkPixel = QrPixelShape.circle(),
        ),
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        SectionTitle(
            title = "Pago por QR",
            subtitle = "Escanea con la app del banco o la pasarela de pago.",
        )
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing.l)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(0.55f),
            ) {
                Column(
                    modifier = Modifier.padding(spacing.xl).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(spacing.m),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(painter = painter, contentDescription = "Código QR de pago", modifier = Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(spacing.l))
                    StatusPill(label = "Esperando confirmación", tone = StatusTone.Warning)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(0.45f),
            ) {
                Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                    Text("Detalle", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    KeyValueRow("Reserva", reservationId)
                    KeyValueRow("Vigencia", "15 minutos")
                    KeyValueRow("Monto retenido", "$ 5.000")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enlace", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(payload, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { /* clipboard solo en desktop/android via expect */ }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(spacing.s))
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.padding(start = spacing.s))
                        Text("He pagado, volver", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Cancelar reserva") }
                }
            }
        }
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    Row {
        Text(key, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
    }
}
