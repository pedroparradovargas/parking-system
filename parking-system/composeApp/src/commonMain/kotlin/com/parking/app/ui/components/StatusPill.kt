package com.parking.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing

/** Tono semántico de la píldora de estado. */
enum class StatusTone { Success, Warning, Info, Danger, Neutral }

/**
 * Píldora reutilizable para etiquetar estados (Activo / Cerrado / Sin conexión / etc.).
 * Tamaño consistente con el resto del sistema.
 */
@Composable
fun StatusPill(
    label: String,
    tone: StatusTone = StatusTone.Info,
    showDot: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalSemanticColors.current
    val spacing = LocalSpacing.current
    val (bg, fg, dot) = when (tone) {
        StatusTone.Success -> Triple(semantic.successContainer, semantic.onSuccessContainer, semantic.success)
        StatusTone.Warning -> Triple(semantic.warningContainer, semantic.onWarningContainer, semantic.warning)
        StatusTone.Info -> Triple(semantic.infoContainer, semantic.onInfoContainer, semantic.info)
        StatusTone.Danger -> Triple(semantic.dangerContainer, semantic.onDangerContainer, semantic.danger)
        StatusTone.Neutral -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = spacing.s, vertical = spacing.xxs),
    ) {
        if (showDot) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(spacing.xs))
        }
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = fg)
    }
}

/** Devuelve un color sólido para la zona según ocupación: verde / ámbar / rojo. */
@Composable
fun occupancyColor(fraction: Float): Color {
    val semantic = LocalSemanticColors.current
    return when {
        fraction >= 0.9f -> semantic.danger
        fraction >= 0.65f -> semantic.warning
        else -> semantic.success
    }
}
