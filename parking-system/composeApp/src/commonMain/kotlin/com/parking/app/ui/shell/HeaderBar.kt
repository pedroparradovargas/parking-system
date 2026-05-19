package com.parking.app.ui.shell

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.LocalAppState
import com.parking.app.state.UserRole
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing

/**
 * Cabecera "ParkSmart" estilo Figma: logo cuadrado azul + nombre + subtítulo
 * a la izquierda; switcher de rol (3 botones) + badge del rol activo a la derecha.
 *
 * Para web la cabecera ocupa todo el ancho; el contenido se centra después
 * en una columna con `widthIn(max = 1280.dp)`.
 */
@Composable
fun HeaderBar() {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current
    val semantic = LocalSemanticColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(semantic.blueContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = semantic.blueAccent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(spacing.m))
        Column {
            Text(
                "ParkSmart",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Sistema de Parqueadero Inteligente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        RoleSwitcher(current = app.userRole, onSelect = { app.userRole = it })
        Spacer(Modifier.width(spacing.m))
        StatusPill(label = app.userRole.label, tone = StatusTone.Info, showDot = false)
    }
}

@Composable
private fun RoleSwitcher(current: UserRole, onSelect: (UserRole) -> Unit) {
    val spacing = LocalSpacing.current
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
        UserRole.entries.forEach { role ->
            if (role == current) {
                Button(
                    onClick = { onSelect(role) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = spacing.m, vertical = spacing.s),
                ) {
                    Text(role.label, fontWeight = FontWeight.Medium)
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(role) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = spacing.m, vertical = spacing.s),
                ) {
                    Text(role.label, fontWeight = FontWeight.Normal)
                }
            }
        }
    }
}
