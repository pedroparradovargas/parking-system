package com.parking.app.ui.screens.mainmenu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parking.app.navigation.Screen
import com.parking.app.state.LocalAppState
import com.parking.app.state.UserRole
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.app.ui.theme.pair

/**
 * Pantalla principal "Menú Principal" — réplica del componente MainMenu.tsx
 * del mockup Figma.  Tres bloques:
 *   1. Welcome card con gradient azul + ícono grande + título por rol.
 *   2. 3 quick stats (plazas disponibles, vehículos activos, tiempo promedio).
 *   3. Grilla de opciones por rol — cada card es clickable y navega.
 *   4. Card "Acciones Rápidas" con 2-3 botones outlined.
 *
 * El contenido cambia según `AppState.userRole` para que el switcher del
 * header tenga efecto inmediato.
 */
@Composable
fun MainMenuScreen(onNavigate: (Screen) -> Unit) {
    val spacing = LocalSpacing.current
    val app = LocalAppState.current
    val role = app.userRole

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        WelcomeCard(role)
        QuickStats(app)
        Text(
            "Opciones Disponibles",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        OptionsGrid(role = role, onNavigate = onNavigate)
        QuickActionsCard(role = role, onNavigate = onNavigate)
    }
}

@Composable
private fun WelcomeCard(role: UserRole) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val (title, subtitle, icon) = when (role) {
        UserRole.Admin -> Triple("Panel de Administración", "Gestiona el sistema de parqueadero.", Icons.Filled.Settings)
        UserRole.Rented -> Triple("Bienvenido Cliente Alquilado", "Acceso rápido a tu plaza reservada.", Icons.Filled.Shield)
        UserRole.Eventual -> Triple("Bienvenido Cliente Eventual", "Encuentra y paga tu plaza de parqueo.", Icons.Filled.Bolt)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = semantic.blueContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, semantic.blueAccent.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(50.dp)).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = semantic.blueAccent, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(spacing.m))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = semantic.onBlueContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.xxs))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = semantic.onBlueContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun QuickStats(app: com.parking.app.state.AppState) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val capacityTotal = app.zones.sumOf { it.capacity }
    val occupancyTotal = app.zones.sumOf { it.currentOccupancy }
    val available = (capacityTotal - occupancyTotal).coerceAtLeast(0)
    val activeVehicles = app.activeSessions.size
    val avgHours = if (app.closedToday.isEmpty()) 0.0
        else app.closedToday.sumOf { it.breakdown.billedMinutes } / 60.0 / app.closedToday.size

    Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), modifier = Modifier.fillMaxWidth()) {
        StatCard("Plazas Disponibles", available.toString(), Icons.Filled.Map, AccentPalette.Green, Modifier.weight(1f))
        StatCard("Vehículos Activos", activeVehicles.toString(), Icons.Filled.DirectionsCar, AccentPalette.Blue, Modifier.weight(1f))
        StatCard("Tiempo Promedio", formatHours(avgHours), Icons.Filled.AccessTime, AccentPalette.Orange, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, palette: AccentPalette, modifier: Modifier = Modifier) {
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
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(spacing.m))
            Column {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = fg)
            }
        }
    }
}

private data class MenuOption(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val palette: AccentPalette,
    val target: Screen,
)

private fun optionsFor(role: UserRole): List<MenuOption> = when (role) {
    UserRole.Admin -> listOf(
        MenuOption("Gestión de Clientes", "Mensualistas, vehículos asociados, vencimientos.", Icons.Filled.PersonOutline, AccentPalette.Blue, Screen.AdminCustomers),
        MenuOption("Gestión de Tarifas", "Configurar precios y políticas.", Icons.Filled.Settings, AccentPalette.Green, Screen.TariffManagement),
        MenuOption("Gestión de Zonas", "Crear, editar y configurar zonas.", Icons.Filled.Map, AccentPalette.Yellow, Screen.AdminZones),
        MenuOption("Gestión de Usuarios", "Operadores, roles y 2FA.", Icons.Filled.Shield, AccentPalette.Red, Screen.AdminUsers),
        MenuOption("Reportes y Estadísticas", "Análisis de uso del parqueadero.", Icons.Filled.BarChart, AccentPalette.Purple, Screen.Reports),
        MenuOption("Reportes administrativos", "Cierre de caja, top placas, mensualidades.", Icons.Filled.BarChart, AccentPalette.Green, Screen.AdminReports),
        MenuOption("Configuración del parqueadero", "Datos fiscales, horarios, integraciones.", Icons.Filled.Settings, AccentPalette.Orange, Screen.AdminConfig),
        MenuOption("Auditoría", "Histórico inmutable de acciones (hash chain).", Icons.Filled.Shield, AccentPalette.Purple, Screen.AdminAudit),
        MenuOption("Periféricos", "Estado y prueba de impresora, lector, cajón.", Icons.Filled.Settings, AccentPalette.Yellow, Screen.AdminDevices),
        MenuOption("Estado del Parqueadero", "Disponibilidad en tiempo real.", Icons.Filled.Map, AccentPalette.Orange, Screen.Availability),
        MenuOption("Soporte al Cliente", "Reposiciones y ayuda.", Icons.AutoMirrored.Filled.HelpOutline, AccentPalette.Red, Screen.Support),
    )
    UserRole.Rented -> listOf(
        MenuOption("Ingreso de Vehículo", "Acceso rápido con tarjeta de cliente.", Icons.Filled.Shield, AccentPalette.Blue, Screen.RentedEntry),
        MenuOption("Consultar Disponibilidad", "Ver plazas disponibles.", Icons.Filled.Map, AccentPalette.Green, Screen.Availability),
        MenuOption("Ayuda y Soporte", "Reposición de tarjeta y asistencia.", Icons.AutoMirrored.Filled.HelpOutline, AccentPalette.Orange, Screen.Support),
    )
    UserRole.Eventual -> listOf(
        MenuOption("Ingreso de Vehículo", "Registro para cliente eventual.", Icons.Filled.AccessTime, AccentPalette.Blue, Screen.EventualEntry),
        MenuOption("Pago de Tarifa", "Realizar pago con tarjeta.", Icons.Filled.CreditCard, AccentPalette.Green, Screen.Payment),
        MenuOption("Consultar Disponibilidad", "Ver plazas disponibles.", Icons.Filled.Map, AccentPalette.Purple, Screen.Availability),
        MenuOption("Ayuda y Soporte", "Asistencia al cliente.", Icons.AutoMirrored.Filled.HelpOutline, AccentPalette.Orange, Screen.Support),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionsGrid(role: UserRole, onNavigate: (Screen) -> Unit) {
    val spacing = LocalSpacing.current
    val options = optionsFor(role)
    // FlowRow en lugar de LazyVerticalGrid — el shell ya scrollea, y las
    // pantallas hijas no pueden tener Lazy* (Compose rompe con `maxHeight = ∞`).
    // Cada card ocupa ~33% del ancho con `Modifier.widthIn(min = 260.dp)`.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        options.forEach { option ->
            Box(modifier = Modifier.widthIn(min = 260.dp, max = 360.dp)) {
                OptionCard(option, onNavigate)
            }
        }
    }
}

@Composable
private fun OptionCard(option: MenuOption, onNavigate: (Screen) -> Unit) {
    val spacing = LocalSpacing.current
    val (bg, fg) = LocalSemanticColors.current.pair(option.palette)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onNavigate(option.target) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(option.icon, contentDescription = null, tint = fg, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(spacing.xs))
            Text(option.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(option.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(spacing.s))
            Button(onClick = { onNavigate(option.target) }, modifier = Modifier.fillMaxWidth()) { Text("Acceder", fontWeight = FontWeight.Medium) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsCard(role: UserRole, onNavigate: (Screen) -> Unit) {
    val spacing = LocalSpacing.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(spacing.l)) {
            Text("Acciones Rápidas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text("Accesos directos a funciones principales.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(spacing.m))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.s), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                OutlinedButton(onClick = { onNavigate(Screen.Availability) }) {
                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(spacing.s))
                    Text("Ver Disponibilidad")
                }
                if (role != UserRole.Admin) {
                    OutlinedButton(onClick = { onNavigate(Screen.Support) }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(spacing.s))
                        Text("Obtener Ayuda")
                    }
                }
                if (role == UserRole.Admin) {
                    OutlinedButton(onClick = { onNavigate(Screen.Reports) }) {
                        Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(spacing.s))
                        Text("Ver Reportes")
                    }
                }
            }
        }
    }
}

private fun formatHours(h: Double): String = if (h <= 0) "—" else {
    val rounded = (h * 10).toInt() / 10.0
    "${rounded}h"
}
