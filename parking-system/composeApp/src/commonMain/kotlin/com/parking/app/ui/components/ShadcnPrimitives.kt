package com.parking.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * Card blanco con borde sutil shadcn-style.  Sin sombra, solo border de 1dp.
 * Es el contenedor por defecto de todas las pantallas portadas.
 */
@Composable
fun ShadcnCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { content() }
}

/**
 * Encabezado de Card al estilo Figma: ícono coloreado + título semibold + descripción gris.
 * Es el patrón que se repite en TODAS las cards principales del mockup.
 */
@Composable
fun CardSectionTitle(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    palette: AccentPalette = AccentPalette.Blue,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val (bg, fg) = LocalSemanticColors.current.pair(palette)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(spacing.s))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Alert estilo shadcn: ícono + texto sobre fondo coloreado con borde.  Se usa para
 * confirmaciones (success), advertencias (warning), errores (danger) e info.
 */
@Composable
fun AlertBox(
    text: String,
    icon: ImageVector,
    tone: AlertTone,
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val (bg, fg) = when (tone) {
        AlertTone.Success -> semantic.greenContainer to semantic.onGreenContainer
        AlertTone.Warning -> semantic.yellowContainer to semantic.onYellowContainer
        AlertTone.Info -> semantic.blueContainer to semantic.onBlueContainer
        AlertTone.Danger -> semantic.redContainer to semantic.onRedContainer
    }
    val border = when (tone) {
        AlertTone.Success -> semantic.greenAccent.copy(alpha = 0.25f)
        AlertTone.Warning -> semantic.yellowAccent.copy(alpha = 0.25f)
        AlertTone.Info -> semantic.blueAccent.copy(alpha = 0.25f)
        AlertTone.Danger -> semantic.redAccent.copy(alpha = 0.25f)
    }
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(spacing.m),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(spacing.s))
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = fg)
            }
            Text(text, style = MaterialTheme.typography.bodySmall, color = fg)
        }
    }
}

enum class AlertTone { Success, Warning, Info, Danger }

/**
 * Botón "Volver al Menú" estilo Figma — usado en el header de cada pantalla
 * para que se pueda navegar también sin usar la tab nav (consistencia con
 * el mockup).  Como tenemos tabs siempre visibles, su rol es más decorativo
 * pero respeta el contrato visual del diseño.
 */
@Composable
fun BackToMenuButton(onClick: () -> Unit, label: String = "Volver al Menú") {
    OutlinedButton(onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(LocalSpacing.current.s))
        Text(label)
    }
}

/**
 * Numeración de pasos circular azul para listas de instrucciones del mockup.
 * (Las cards "Instrucciones" del Figma usan este patrón con números en círculo).
 */
@Composable
fun StepBullet(number: Int, palette: AccentPalette = AccentPalette.Blue) {
    val (bg, fg) = LocalSemanticColors.current.pair(palette)
    Box(
        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(50.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(number.toString(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = fg)
    }
}

/**
 * Fila etiqueta + valor que se repite en todas las cards de "Detalle" (RentedEntry,
 * Payment, Registration).  Mantiene tipografía consistente.
 */
@Composable
fun LabelValueRow(label: String, value: String, accent: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium),
            color = if (accent) LocalSemanticColors.current.greenAccent else MaterialTheme.colorScheme.onSurface,
        )
    }
}
