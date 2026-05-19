package com.parking.app.ui.screens.placeholder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing

/** Pantalla genérica "en construcción" para secciones del Figma todavía no portadas. */
@Composable
fun PlaceholderScreen(title: String, description: String) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        SectionHeader(title = title, description = description)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(semantic.orangeContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Construction, contentDescription = null, tint = semantic.orangeAccent, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(spacing.m))
                Text(
                    "Pantalla en construcción",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.height(spacing.xs))
                Text(
                    "Esta sección se portará completa en la siguiente fase de maduración.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
