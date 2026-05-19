package com.parking.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/*
 * Tema shadcn/ui portado a Compose.
 *
 *   --background:   #F9FAFB    (gray-50, fondo de toda la app)
 *   --primary:      #030213    (casi negro — botones primarios)
 *   --foreground:   #0F172A    (texto principal)
 *   --muted:        #ECECF0    (badges neutrales)
 *   --border:       rgba(0,0,0,0.08)
 *
 * Los acentos por sección (blue/green/purple/...) viven en `SemanticColors`
 * y NO se exponen vía MaterialTheme.colorScheme.* — se consumen con
 * `LocalSemanticColors.current.bluePair`, etc.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF030213),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1D4ED8),

    secondary = Color(0xFFECECF0),
    onSecondary = Color(0xFF030213),
    secondaryContainer = Color(0xFFE9EBEF),
    onSecondaryContainer = Color(0xFF030213),

    tertiary = Color(0xFFEA580C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF9A3412),

    background = Color(0xFFF9FAFB),
    onBackground = Color(0xFF0F172A),

    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceTint = Color(0xFF030213),

    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),

    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5E7EB),
    onPrimary = Color(0xFF030213),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),

    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE5E7EB),

    surface = Color(0xFF171A1F),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1F232A),
    onSurfaceVariant = Color(0xFF9CA3AF),

    outline = Color(0xFF374151),
    outlineVariant = Color(0xFF1F2937),

    error = Color(0xFFF87171),
    onError = Color(0xFF7F1D1D),
)

@Composable
fun ParkingTheme(useDarkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        shapes = ParkingShapes,
    ) {
        CompositionLocalProvider(
            LocalSpacing provides Spacing(),
            LocalSemanticColors provides SemanticColors(),
            content = content,
        )
    }
}
