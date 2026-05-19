package com.parking.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.app.ui.theme.pair

/**
 * Tile KPI estilo Figma/shadcn: card blanco con borde sutil, ícono en cuadro
 * coloreado (Tailwind 100), label gris pequeño, valor 2xl bold en el color 600.
 *
 * Es el ladrillo que usa todo el dashboard, Reportes y la franja superior
 * de Caja/Disponibilidad.
 */
@Composable
fun AccentKpi(
    label: String,
    value: String,
    icon: ImageVector,
    palette: AccentPalette,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val (bg, fg) = LocalSemanticColors.current.pair(palette)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(spacing.m))
            Column {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = fg)
            }
        }
    }
}
