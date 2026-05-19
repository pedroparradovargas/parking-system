package com.parking.app.ui.screens.reservation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.generateLocalId
import com.parking.app.ui.components.PlateInput
import com.parking.app.ui.components.SectionTitle
import com.parking.app.ui.components.VehicleTypeSelector
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.domain.model.PlateNumber
import com.parking.shared.domain.model.VehicleType

/** Pantalla del cliente final: reservar un espacio.  Entrega un `reservationId`. */
@Composable
fun ReservationScreen(onConfirm: (reservationId: String) -> Unit) {
    val spacing = LocalSpacing.current
    var plate by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(VehicleType.CAR) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        SectionTitle(
            title = "Reservar espacio",
            subtitle = "El cliente recibe un QR de pago para asegurar su lugar.",
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.widthIn(max = 720.dp),
        ) {
            Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                Text("Datos de la reserva", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                PlateInput(value = plate, onValueChange = { plate = it; error = null })
                Text("Tipo de vehículo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                VehicleTypeSelector(selected = type, onSelect = { type = it })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(
                    onClick = {
                        val ok = runCatching { PlateNumber(plate) }.isSuccess
                        if (!ok) { error = "Placa inválida (5–10 caracteres alfanuméricos)."; return@Button }
                        onConfirm("RES-${plate.takeLast(4)}-${generateLocalId().takeLast(8)}")
                    },
                    enabled = plate.length >= 5,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null)
                    Spacer(Modifier.padding(start = spacing.s))
                    Text("Confirmar y generar QR de pago", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
