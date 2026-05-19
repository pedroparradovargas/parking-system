package com.parking.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization

/**
 * Input para placa con autocapitalización y normalización mientras se tipea.
 * Filtra caracteres no alfanuméricos y limita a 10 caracteres.
 */
@Composable
fun PlateInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Placa",
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val clean = raw.uppercase().filter { it.isLetterOrDigit() }.take(10)
            onValueChange(clean)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        textStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = MaterialTheme.typography.titleMedium.letterSpacing,
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
        ),
        leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
        modifier = modifier.fillMaxWidth(),
    )
}
