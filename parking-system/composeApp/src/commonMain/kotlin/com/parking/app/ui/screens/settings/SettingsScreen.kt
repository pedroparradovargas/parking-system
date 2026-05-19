package com.parking.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.SectionTitle
import com.parking.app.ui.theme.LocalSpacing

/** Ajustes locales — secciones discretas con switches y campos. */
@Composable
fun SettingsScreen() {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current
    var baseUrl by remember { mutableStateOf("https://api.parking.example.com") }
    var kioskMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        SectionTitle(
            title = "Configuración",
            subtitle = "Preferencias locales de esta caja. Se guardan al reiniciar.",
        )

        SettingsSection(title = "Servidor") {
            SettingRow(
                icon = Icons.Filled.Link,
                title = "URL del backend",
                subtitle = "Apunta al servidor Ktor de tu organización.",
                trailing = {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        singleLine = true,
                        modifier = Modifier.width(360.dp),
                    )
                }
            )
        }

        SettingsSection(title = "Operación") {
            SettingRow(
                icon = Icons.Filled.Fullscreen,
                title = "Modo kiosk",
                subtitle = "Inicia la caja en pantalla completa al abrir la app.",
                trailing = { Switch(checked = kioskMode, onCheckedChange = { kioskMode = it }) }
            )
            SettingRow(
                icon = Icons.Filled.WifiOff,
                title = "Simular offline",
                subtitle = "Útil para probar el flujo de sincronización en background.",
                trailing = { Switch(checked = !app.isOnline, onCheckedChange = { app.isOnline = !it }) }
            )
        }

        SettingsSection(title = "Apariencia") {
            SettingRow(
                icon = Icons.Filled.DarkMode,
                title = "Tema oscuro",
                subtitle = "Útil para cajas con baja iluminación nocturna.",
                trailing = { Switch(checked = app.darkTheme, onCheckedChange = { app.darkTheme = it }) }
            )
        }

        Text(
            "Versión 1.0.0 — Build local.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val spacing = LocalSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.padding(spacing.s)) { content() }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(spacing.m))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(spacing.m))
        trailing()
    }
}
