package com.parking.app.ui.screens.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.BackToMenuButton
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.components.StatusPill
import com.parking.app.ui.components.StatusTone
import com.parking.app.ui.theme.LocalSemanticColors
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.ParkingApiClient
import com.parking.shared.data.api.dto.AdminUserDto
import org.koin.compose.koinInject

/**
 * Admin > Gestión de Usuarios.  Tabla con cards por usuario; acciones por
 * fila: Editar, Reset password, Activar/Desactivar 2FA, Deshabilitar.
 */
@Composable
fun AdminUsersScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    val appState = LocalAppState.current
    val api: ParkingApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val vm = remember { AdminUserViewModel(api = api, parkingId = appState.parkingId, scope = scope) }

    LaunchedEffect(vm) { vm.reload() }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            Box(modifier = Modifier.weight(1f)) {
                SectionHeader(
                    title = "Gestión de Usuarios",
                    description = "Crear, editar y configurar operadores y administradores.",
                )
            }
            OutlinedButton(onClick = { vm.reload() }) { Text("Recargar") }
            Spacer(Modifier.width(spacing.s))
            Button(onClick = { vm.openCreate() }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(spacing.xs))
                Text("Nuevo usuario")
            }
        }

        vm.error.value?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        val items = vm.items.value
        if (items.isEmpty() && vm.error.value == null && !vm.loading.value) {
            Text("No hay usuarios todavía. Crea el primero con \"Nuevo usuario\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items.forEach { u -> UserCard(u, vm) }
    }

    if (vm.showCreate.value || vm.editing.value != null) {
        UserEditDialog(
            initial = vm.editing.value,
            onCancel = { vm.closeDialogs() },
            onCreate = { req -> vm.create(req) },
            onUpdate = { id, req -> vm.update(id, req) },
            submitting = vm.loading.value,
        )
    }
    vm.tempPasswordToast.value?.let { temp ->
        AlertDialog(
            onDismissRequest = { vm.clearTempPassword() },
            title = { Text("Contraseña temporal generada") },
            text = {
                Column {
                    Text("Entrégala al operador.  Es de un solo uso — debe cambiarla en su próximo login.")
                    Spacer(Modifier.size(spacing.s))
                    Text(temp, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                }
            },
            confirmButton = { Button(onClick = { vm.clearTempPassword() }) { Text("Listo") } },
        )
    }
    vm.totpInfo.value?.let { info ->
        AlertDialog(
            onDismissRequest = { vm.clearTotpInfo() },
            title = { Text("2FA activado") },
            text = {
                Column {
                    Text("Comparte este secret o URI con el usuario para que lo escanee en Google Authenticator / Authy:")
                    Spacer(Modifier.size(spacing.s))
                    Text("Secret: ${info.secret}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.size(spacing.xs))
                    Text(info.otpAuthUri, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { Button(onClick = { vm.clearTotpInfo() }) { Text("Listo") } },
        )
    }
}

@Composable
private fun UserCard(u: AdminUserDto, vm: AdminUserViewModel) {
    val spacing = LocalSpacing.current
    val sc = LocalSemanticColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(spacing.l), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(sc.blueContainer),
                contentAlignment = Alignment.Center,
            ) { Text(u.username.take(2).uppercase(), color = sc.blueAccent, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(spacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(u.fullName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(Modifier.width(spacing.s))
                    StatusPill(
                        label = if (u.enabled) "Activo" else "Deshabilitado",
                        tone = if (u.enabled) StatusTone.Success else StatusTone.Neutral,
                        showDot = false,
                    )
                    if (u.requires2fa) {
                        Spacer(Modifier.width(spacing.xs))
                        StatusPill(label = "2FA", tone = StatusTone.Info, showDot = false)
                    }
                }
                Text("${u.username} · ${u.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Roles: ${u.roles.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                OutlinedButton(onClick = { vm.openEdit(u) }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                OutlinedButton(onClick = { vm.resetPassword(u) }) {
                    Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                if (u.requires2fa) {
                    OutlinedButton(onClick = { vm.disable2fa(u) }) {
                        Icon(Icons.Filled.Shield, contentDescription = "Desactivar 2FA", modifier = Modifier.size(14.dp))
                    }
                } else {
                    OutlinedButton(onClick = { vm.enable2fa(u) }) {
                        Icon(Icons.Filled.Shield, contentDescription = "Activar 2FA", modifier = Modifier.size(14.dp))
                    }
                }
                if (u.enabled) {
                    OutlinedButton(onClick = { vm.disable(u) }) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
