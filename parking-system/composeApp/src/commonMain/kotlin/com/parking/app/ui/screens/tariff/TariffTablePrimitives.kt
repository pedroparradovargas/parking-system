package com.parking.app.ui.screens.tariff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing

/** Modelo de celda usado por TableRow.  Solo dos variantes: texto y badge. */
internal sealed interface TableCell {
    data class Text(
        val value: String,
        val weight: FontWeight,
        val muted: Boolean = false,
        val accent: AccentPalette? = null,
    ) : TableCell

    data class Badge(val value: String, val tone: StatusTone) : TableCell
}

@Composable
internal fun TableHeaderRow(headers: List<String>) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = spacing.m, vertical = spacing.s),
    ) {
        headers.forEach { h ->
            Text(
                h,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun TableRow(cells: List<TableCell>) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = spacing.m, vertical = spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEach { c ->
            Box(modifier = Modifier.weight(1f)) {
                when (c) {
                    is TableCell.Text -> {
                        val sc = LocalSemanticColors.current
                        val color = when {
                            c.muted -> MaterialTheme.colorScheme.onSurfaceVariant
                            c.accent != null -> when (c.accent) {
                                AccentPalette.Blue -> sc.blueAccent
                                AccentPalette.Green -> sc.greenAccent
                                AccentPalette.Purple -> sc.purpleAccent
                                AccentPalette.Orange -> sc.orangeAccent
                                AccentPalette.Red -> sc.redAccent
                                AccentPalette.Yellow -> sc.yellowAccent
                            }
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(c.value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = c.weight), color = color)
                    }
                    is TableCell.Badge -> StatusPill(label = c.value, tone = c.tone, showDot = false)
                }
            }
        }
    }
}
