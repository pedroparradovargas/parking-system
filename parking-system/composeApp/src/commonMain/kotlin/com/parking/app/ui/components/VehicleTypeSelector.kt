package com.parking.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.parking.shared.domain.model.VehicleType

/** Selector de tipo de vehículo con íconos.  Compatible con touch y mouse. */
@Composable
fun VehicleTypeSelector(
    selected: VehicleType,
    onSelect: (VehicleType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        VehicleType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text(type.label()) },
                leadingIcon = {
                    Icon(type.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
                },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

internal fun VehicleType.label(): String = when (this) {
    VehicleType.CAR -> "Auto"
    VehicleType.MOTORCYCLE -> "Moto"
    VehicleType.BICYCLE -> "Bici"
    VehicleType.TRUCK -> "Camión"
    VehicleType.BUS -> "Bus"
}

internal fun VehicleType.icon(): ImageVector = when (this) {
    VehicleType.CAR -> Icons.Filled.DirectionsCar
    VehicleType.MOTORCYCLE -> Icons.Filled.TwoWheeler
    VehicleType.BICYCLE -> Icons.AutoMirrored.Filled.DirectionsBike
    VehicleType.TRUCK -> Icons.Filled.LocalShipping
    VehicleType.BUS -> Icons.Filled.DirectionsBus
}
