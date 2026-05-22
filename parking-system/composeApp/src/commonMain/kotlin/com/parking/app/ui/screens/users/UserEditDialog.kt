package com.parking.app.ui.screens.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.dto.AdminUserDto
import com.parking.shared.data.api.dto.CreateUserRequest
import com.parking.shared.data.api.dto.UpdateUserRequest

/** Diálogo crear / editar usuario.  En edición no se pide password. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserEditDialog(
    initial: AdminUserDto?,
    onCancel: () -> Unit,
    onCreate: (CreateUserRequest) -> Unit,
    onUpdate: (id: String, UpdateUserRequest) -> Unit,
    submitting: Boolean = false,
) {
    val spacing = LocalSpacing.current
    val creating = initial == null
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var fullName by remember { mutableStateOf(initial?.fullName ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    val rolesState = remember {
        mutableStateOf(initial?.roles?.toSet() ?: setOf("CASHIER"))
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val availableRoles = listOf("SUPERADMIN", "ADMIN", "CASHIER", "VIEWER")

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (creating) "Nuevo usuario" else "Editar usuario ${initial?.username}",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s), modifier = Modifier.fillMaxWidth()) {
                if (creating) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' }.take(80) },
                        label = { Text("Username") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = fullName, onValueChange = { fullName = it.take(200) },
                    label = { Text("Nombre completo") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it.take(200) },
                    label = { Text("Email") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (creating) {
                    OutlinedTextField(
                        value = password, onValueChange = { password = it.take(100) },
                        label = { Text("Contraseña inicial (min 8)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text("Roles", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    availableRoles.forEach { r ->
                        val sel = r in rolesState.value
                        FilterChip(
                            selected = sel,
                            onClick = {
                                rolesState.value = if (sel) rolesState.value - r else rolesState.value + r
                            },
                            label = { Text(r) },
                        )
                    }
                }
                if (!creating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                        Text("Usuario activo", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    validationError = when {
                        creating && (username.isBlank() || username.length < 3) -> "Username inválido (min 3)."
                        fullName.isBlank() -> "Nombre completo obligatorio."
                        !email.contains("@") -> "Email inválido."
                        creating && password.length < 8 -> "Contraseña muy corta (min 8)."
                        rolesState.value.isEmpty() -> "Asigna al menos un rol."
                        else -> null
                    }
                    if (validationError == null) {
                        if (creating) {
                            onCreate(
                                CreateUserRequest(
                                    username = username,
                                    fullName = fullName,
                                    email = email,
                                    password = password,
                                    roles = rolesState.value.toList(),
                                )
                            )
                        } else {
                            onUpdate(
                                initial!!.id,
                                UpdateUserRequest(
                                    fullName = fullName,
                                    email = email,
                                    roles = rolesState.value.toList(),
                                    enabled = enabled,
                                ),
                            )
                        }
                    }
                },
                enabled = !submitting,
            ) { Text(if (submitting) "Guardando…" else "Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel, enabled = !submitting) { Text("Cancelar") }
        },
    )
}
