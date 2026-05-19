package com.parking.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing

/**
 * Banner persistente cuando no hay conexión (Regla 9: offline-first).
 * Color warning + ícono.  Animación suave para no distraer.
 */
@Composable
fun OfflineBanner(visible: Boolean) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(semantic.warningContainer)
                .padding(horizontal = spacing.l, vertical = spacing.s),
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = semantic.onWarningContainer, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(spacing.s))
            Text(
                "Sin conexión. Operando localmente — los recibos se sincronizarán cuando vuelva la red.",
                style = MaterialTheme.typography.bodySmall,
                color = semantic.onWarningContainer,
            )
        }
    }
}
