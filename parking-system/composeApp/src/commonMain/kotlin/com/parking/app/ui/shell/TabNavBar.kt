package com.parking.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.navigation.Screen
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing

/**
 * Tab nav horizontal con las 9 secciones exactas del mockup Figma
 * `Sistema de Parqueadero Inteligente (Community)`.  Cada tab tiene su ícono
 * `lucide`-equivalente en Material Icons y un borde inferior azul cuando está activa.
 *
 * Es horizontalScroll para no truncar tabs en pantallas estrechas.
 */
@Composable
fun TabNavBar(current: Screen, onNavigate: (Screen) -> Unit) {
    val spacing = LocalSpacing.current
    val scroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)
                .padding(horizontal = spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabEntries.forEach { entry ->
                TabItem(
                    icon = entry.icon,
                    label = entry.label,
                    active = isSameSection(current, entry.target),
                    onClick = { onNavigate(entry.target) },
                )
            }
        }
    }
}

private data class TabEntry(val icon: ImageVector, val label: String, val target: Screen)

/** Las 9 secciones del Figma App.tsx, en el mismo orden. */
private val TabEntries: List<TabEntry> = listOf(
    TabEntry(Icons.Filled.DirectionsCar, "Menú Principal", Screen.MainMenu),
    TabEntry(Icons.Filled.Shield, "Ingreso Alquilado", Screen.RentedEntry),
    TabEntry(Icons.Filled.AccessTime, "Ingreso Eventual", Screen.EventualEntry),
    TabEntry(Icons.Filled.CreditCard, "Pago con Tarjeta", Screen.Payment),
    TabEntry(Icons.Filled.PersonOutline, "Registro Cliente", Screen.ClientRegistration),
    TabEntry(Icons.Filled.Settings, "Gestión Tarifas", Screen.TariffManagement),
    TabEntry(Icons.Filled.Map, "Disponibilidad", Screen.Availability),
    TabEntry(Icons.Filled.BarChart, "Reportes", Screen.Reports),
    TabEntry(Icons.AutoMirrored.Filled.HelpOutline, "Ayuda/Soporte", Screen.Support),
)

@Composable
private fun TabItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val fg: Color = if (active) semantic.blueAccent else MaterialTheme.colorScheme.onSurfaceVariant
    val underline: Color = if (active) semantic.blueAccent else Color.Transparent
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .height(56.dp)
            .padding(horizontal = spacing.s),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(spacing.s))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                color = fg,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(underline),
        )
    }
}

/** Marca como activa la pestaña que mejor representa la pantalla actual. */
private fun isSameSection(current: Screen, target: Screen): Boolean = when {
    target is Screen.MainMenu && current is Screen.MainMenu -> true
    target is Screen.RentedEntry && current is Screen.RentedEntry -> true
    target is Screen.EventualEntry && (current is Screen.EventualEntry || current is Screen.Cashier) -> true
    target is Screen.Payment && (current is Screen.Payment || current is Screen.Reservation || current is Screen.PaymentQr) -> true
    target is Screen.ClientRegistration && current is Screen.ClientRegistration -> true
    target is Screen.TariffManagement && current is Screen.TariffManagement -> true
    target is Screen.Availability && current is Screen.Availability -> true
    target is Screen.Reports && current is Screen.Reports -> true
    target is Screen.Support && current is Screen.Support -> true
    else -> false
}
