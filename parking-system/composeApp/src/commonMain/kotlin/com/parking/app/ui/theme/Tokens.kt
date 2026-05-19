package com.parking.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tokens de diseño del sistema — alineados con la paleta shadcn/ui usada por
 * el mockup Figma "Sistema de Parqueadero Inteligente (Community)".
 *
 * Los colores semánticos siguen el patrón Tailwind <color>-100 (fondo) /
 * <color>-600 (texto/ícono).  Cada sección del dashboard usa un acento
 * distinto (blue/green/purple/orange/red/yellow) para diferenciar visualmente.
 */

/** Escala de espaciado de 4dp.  Úsalo siempre en padding/spacing. */
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Formas shadcn — radius base 10dp (0.625rem en su CSS). */
val ParkingShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

/**
 * Colores semánticos extendidos.  Cubre las 6 paletas de acento usadas en
 * Figma (blue/green/purple/orange/red/yellow/amber) — cada una con su
 * variante "container" (Tailwind 100) y "on" (Tailwind 600/700/800).
 *
 * También expone tokens de fondo de página (`pageBackground`) y borde
 * tenue (`hairline`).
 */
data class SemanticColors(
    // Fondo de la app (gray-50)
    val pageBackground: Color = Color(0xFFF9FAFB),
    // Bordes finos (rgba(0,0,0,0.08))
    val hairline: Color = Color(0xFFE5E7EB),
    val rowHover: Color = Color(0xFFF3F4F6),

    // Blue (info / Tab activo / dashboard primario)
    val blueContainer: Color = Color(0xFFDBEAFE),
    val onBlueContainer: Color = Color(0xFF1D4ED8),
    val blueAccent: Color = Color(0xFF2563EB),

    // Green (success / available)
    val greenContainer: Color = Color(0xFFDCFCE7),
    val onGreenContainer: Color = Color(0xFF166534),
    val greenAccent: Color = Color(0xFF16A34A),

    // Purple (analytics / advanced)
    val purpleContainer: Color = Color(0xFFF3E8FF),
    val onPurpleContainer: Color = Color(0xFF6B21A8),
    val purpleAccent: Color = Color(0xFF9333EA),

    // Orange (warning / occupancy)
    val orangeContainer: Color = Color(0xFFFFEDD5),
    val onOrangeContainer: Color = Color(0xFF9A3412),
    val orangeAccent: Color = Color(0xFFEA580C),

    // Red (danger / occupied)
    val redContainer: Color = Color(0xFFFEE2E2),
    val onRedContainer: Color = Color(0xFF991B1B),
    val redAccent: Color = Color(0xFFDC2626),

    // Yellow (reserved)
    val yellowContainer: Color = Color(0xFFFEF3C7),
    val onYellowContainer: Color = Color(0xFF92400E),
    val yellowAccent: Color = Color(0xFFCA8A04),

    // Aliases semánticos comunes (para no romper código existente):
    val success: Color = Color(0xFF16A34A),
    val onSuccess: Color = Color.White,
    val successContainer: Color = Color(0xFFDCFCE7),
    val onSuccessContainer: Color = Color(0xFF166534),
    val warning: Color = Color(0xFFCA8A04),
    val onWarning: Color = Color.White,
    val warningContainer: Color = Color(0xFFFEF3C7),
    val onWarningContainer: Color = Color(0xFF92400E),
    val info: Color = Color(0xFF2563EB),
    val onInfo: Color = Color.White,
    val infoContainer: Color = Color(0xFFDBEAFE),
    val onInfoContainer: Color = Color(0xFF1D4ED8),
    val danger: Color = Color(0xFFDC2626),
    val onDanger: Color = Color.White,
    val dangerContainer: Color = Color(0xFFFEE2E2),
    val onDangerContainer: Color = Color(0xFF991B1B),
)

val LocalSemanticColors = staticCompositionLocalOf { SemanticColors() }

/** Convenience: par (container, accent) listo para usar en KpiCard. */
enum class AccentPalette { Blue, Green, Purple, Orange, Red, Yellow }

fun SemanticColors.pair(palette: AccentPalette): Pair<Color, Color> = when (palette) {
    AccentPalette.Blue -> blueContainer to blueAccent
    AccentPalette.Green -> greenContainer to greenAccent
    AccentPalette.Purple -> purpleContainer to purpleAccent
    AccentPalette.Orange -> orangeContainer to orangeAccent
    AccentPalette.Red -> redContainer to redAccent
    AccentPalette.Yellow -> yellowContainer to yellowAccent
}
