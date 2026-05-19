package com.parking.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing

/**
 * KPI tile estilo dashboard: ícono coloreado en cuadro, etiqueta, valor grande
 * y delta opcional (positivo / negativo / neutral).
 */
@Composable
fun KpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color = MaterialTheme.colorScheme.primary,
    accentBg: Color = MaterialTheme.colorScheme.primaryContainer,
    delta: KpiDelta? = null,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(accentBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.weight(1f))
                if (delta != null) DeltaPill(delta)
            }
            Spacer(Modifier.height(spacing.m))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(spacing.xxs))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Indicador de tendencia para una métrica.  [percent] es positivo o negativo. */
data class KpiDelta(val percent: Int, val hint: String = "vs ayer")

@Composable
private fun DeltaPill(delta: KpiDelta) {
    val semantic = LocalSemanticColors.current
    val spacing = LocalSpacing.current
    val (bg, fg, icon) = when {
        delta.percent > 0 -> Triple(semantic.successContainer, semantic.onSuccessContainer, Icons.Filled.ArrowUpward)
        delta.percent < 0 -> Triple(semantic.dangerContainer, semantic.onDangerContainer, Icons.Filled.ArrowDownward)
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Filled.ArrowUpward)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = spacing.s, vertical = spacing.xxs),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(spacing.xxs))
        Text(
            "${if (delta.percent >= 0) "+" else ""}${delta.percent}%",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
    }
}
