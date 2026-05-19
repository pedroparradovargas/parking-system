package com.parking.app.ui.screens.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.ui.components.AlertBox
import com.parking.app.ui.components.AlertTone
import com.parking.app.ui.components.BackToMenuButton
import com.parking.app.ui.components.CardSectionTitle
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.components.ShadcnCard
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.AccentPalette
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing

/**
 * Centro de Ayuda y Soporte.  Port de `CustomerSupport.tsx`:
 *   - Tabs: Tickets · Reposición de Tarjetas · Preguntas Frecuentes · Información de Contacto.
 *   - Sin cableado al backend todavía — datos mock para diseño.
 */
@Composable
fun CustomerSupportScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    var tab by remember { mutableStateOf(SupportTab.Tickets) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            SectionHeader(
                title = "Centro de Ayuda y Soporte",
                description = "Gestión de tickets, reposición de tarjetas y preguntas frecuentes.",
            )
        }
        TabBar(active = tab, onChange = { tab = it })
        when (tab) {
            SupportTab.Tickets -> TicketsTab()
            SupportTab.Replacement -> ReplacementTab()
            SupportTab.Faq -> FaqTab()
            SupportTab.Contact -> ContactTab()
        }
    }
}

private enum class SupportTab(val label: String) {
    Tickets("Tickets de Soporte"), Replacement("Reposición de Tarjetas"), Faq("Preguntas Frecuentes"), Contact("Información de Contacto"),
}

@Composable
private fun TabBar(active: SupportTab, onChange: (SupportTab) -> Unit) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        SupportTab.entries.forEach { t ->
            val isActive = t == active
            val bg = if (isActive) MaterialTheme.colorScheme.surface else Color.Transparent
            val fg = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .clickable { onChange(t) }
                    .padding(vertical = spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(t.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium), color = fg)
            }
        }
    }
}

private data class Ticket(val id: String, val client: String, val issue: String, val priority: String, val status: String, val created: String)

private val sampleTickets = listOf(
    Ticket("TK-001", "María González", "Tarjeta no funciona", "Alta", "Abierto", "2026-05-15 09:30"),
    Ticket("TK-002", "Carlos Rodríguez", "Cobro incorrecto", "Media", "En Proceso", "2026-05-15 11:15"),
    Ticket("TK-003", "Ana Martínez", "Plaza ocupada incorrectamente", "Baja", "Resuelto", "2026-05-14 16:45"),
)

@Composable
private fun TicketsTab() {
    val spacing = LocalSpacing.current
    var clientId by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.l), modifier = Modifier.fillMaxWidth()) {
        ShadcnCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                CardSectionTitle(
                    title = "Crear Nuevo Ticket",
                    description = "Registre un nuevo caso de soporte al cliente.",
                    icon = Icons.Outlined.ChatBubbleOutline,
                    palette = AccentPalette.Blue,
                )
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it.uppercase() },
                    label = { Text("ID del Cliente") },
                    placeholder = { Text("CLI-1001") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción del Problema") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { clientId = ""; description = "" },
                    enabled = clientId.isNotBlank() && description.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) { Text("Crear Ticket") }
            }
        }
        ShadcnCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
                Text("Tickets Activos", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text("Lista de casos de soporte pendientes y recientes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar tickets...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                sampleTickets.filter { it.id.contains(query, true) || it.client.contains(query, true) || it.issue.contains(query, true) }.forEach { t ->
                    TicketRow(t)
                }
            }
        }
    }
}

@Composable
private fun TicketRow(t: Ticket) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.id, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(t.client, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(label = t.status, tone = statusTone(t.status))
            Spacer(Modifier.width(spacing.xs))
            StatusPill(label = t.priority, tone = priorityTone(t.priority), showDot = false)
        }
        Text(t.issue, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(spacing.xxs))
            Text(t.created, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun statusTone(s: String) = when (s) {
    "Abierto" -> StatusTone.Danger
    "En Proceso" -> StatusTone.Warning
    "Resuelto" -> StatusTone.Success
    else -> StatusTone.Neutral
}

private fun priorityTone(p: String) = when (p) {
    "Urgente", "Alta" -> StatusTone.Danger
    "Media" -> StatusTone.Warning
    "Baja" -> StatusTone.Success
    else -> StatusTone.Neutral
}

@Composable
private fun ReplacementTab() {
    val spacing = LocalSpacing.current
    var name by remember { mutableStateOf("") }
    var doc by remember { mutableStateOf("") }
    var newCard by remember { mutableStateOf<String?>(null) }
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            CardSectionTitle(
                title = "Reposición de Tarjetas",
                description = "Genere nuevas tarjetas para clientes que han perdido o dañado las suyas.",
                icon = Icons.Filled.CreditCard,
                palette = AccentPalette.Green,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del Cliente") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = doc, onValueChange = { doc = it }, label = { Text("Número de Documento") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            newCard?.let {
                AlertBox(
                    title = "Nueva tarjeta generada",
                    text = it,
                    icon = Icons.Filled.CheckCircle,
                    tone = AlertTone.Success,
                )
            }
            Button(
                onClick = { newCard = "PKS${kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString().takeLast(6)}" },
                enabled = name.isNotBlank() && doc.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) { Text("Generar Nueva Tarjeta") }
        }
    }
}

@Composable
private fun FaqTab() {
    val spacing = LocalSpacing.current
    val faqs = listOf(
        "¿Qué hago si pierdo mi tarjeta de cliente?" to "Puede solicitar una reposición de tarjeta en la sección \"Reposición de Tarjeta\" de este sistema. Necesitará proporcionar su documento de identidad y el motivo.",
        "¿Cómo puedo cambiar mi plan de suscripción?" to "Para cambiar su plan, contacte a nuestro equipo de soporte o visite la oficina administrativa. Los cambios se aplican desde el siguiente ciclo de facturación.",
        "¿Qué pasa si excedo el tiempo de mi ticket?" to "Se aplicarán tarifas adicionales por cada hora o fracción adicional. Puede realizar el pago en cualquier terminal de pago antes de la salida.",
        "¿Puedo reservar una plaza específica?" to "Las reservas están disponibles solo para clientes con plan mensual o superior. Los clientes eventuales reciben asignación automática de plaza disponible.",
        "¿Cómo reporto un problema con mi plaza asignada?" to "Puede reportar problemas creando un ticket de soporte o llamando a nuestra línea de atención al cliente 24/7.",
    )
    ShadcnCard {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            CardSectionTitle(
                title = "Preguntas Frecuentes",
                description = "Respuestas a las consultas más comunes de los clientes.",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                palette = AccentPalette.Purple,
            )
            faqs.forEach { (q, a) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .padding(spacing.m),
                ) {
                    Text(q, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.height(spacing.xs))
                    Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ContactTab() {
    val spacing = LocalSpacing.current
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), modifier = Modifier.fillMaxWidth()) {
        ContactCard(icon = Icons.Filled.Phone, palette = AccentPalette.Blue, title = "Teléfono", primary = "+57 (1) 234-5678", lines = listOf("Línea de atención 24/7", "Costo de llamada local"), modifier = Modifier.weight(1f))
        ContactCard(icon = Icons.Filled.Email, palette = AccentPalette.Green, title = "Email", primary = "soporte@parksmart.com", lines = listOf("Respuesta en 24 horas", "Lunes a domingo"), modifier = Modifier.weight(1f))
        ContactCard(icon = Icons.Filled.LocationOn, palette = AccentPalette.Purple, title = "Oficina", primary = "Calle 123 #45-67", lines = listOf("Bogotá, Colombia", "Lun-Vie: 8:00-18:00", "Sáb: 9:00-15:00"), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ContactCard(icon: ImageVector, palette: AccentPalette, title: String, primary: String, lines: List<String>, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    ShadcnCard(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            CardSectionTitle(title = title, description = null, icon = icon, palette = palette)
            Text(primary, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            lines.forEach { l -> Text(l, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** Versión sin handlers — se usa cuando se renderiza dentro del shell. */
@Composable
fun CustomerSupportScreen() {
    CustomerSupportScreen(onBack = { })
}
