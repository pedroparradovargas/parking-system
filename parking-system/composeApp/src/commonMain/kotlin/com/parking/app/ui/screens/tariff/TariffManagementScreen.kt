package com.parking.app.ui.screens.tariff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parking.app.state.LocalAppState
import com.parking.app.ui.components.BackToMenuButton
import com.parking.app.ui.components.SectionHeader
import com.parking.app.ui.theme.LocalSpacing
import com.parking.shared.data.api.ParkingApiClient
import org.koin.compose.koinInject

/**
 * Gestión de Tarifas — Port de `TariffManagement.tsx`.
 * Tabs: Tarifas por Hora · Planes de Suscripción · Tarifas Especiales.
 *
 * Las tabs viven en `TariffTabs.kt` y los primitives de tabla en
 * `TariffTablePrimitives.kt` para respetar la regla 3 (archivos < 300 líneas).
 * Los datos hardcoded están al pie de este archivo y se reemplazarán por
 * un repositorio real cuando se complete la Fase B del plan del Admin.
 */
@Composable
fun TariffManagementScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    val appState = LocalAppState.current
    val api: ParkingApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val tariffVm = remember { AdminTariffViewModel(api = api, parkingId = appState.parkingId, scope = scope) }
    val planVm = remember { AdminTariffPlanViewModel(api = api, parkingId = appState.parkingId, scope = scope) }
    val specialVm = remember { AdminSpecialTariffViewModel(api = api, parkingId = appState.parkingId, scope = scope) }

    var tab by remember { mutableStateOf(TariffTab.Hourly) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            SectionHeader(
                title = "Gestión de Tarifas",
                description = "Configure precios y políticas del parqueadero.",
            )
        }
        TabBar(active = tab, onChange = { tab = it })
        when (tab) {
            TariffTab.Hourly -> HourlyTab(vm = tariffVm)
            TariffTab.Subscriptions -> SubscriptionsTab(vm = planVm)
            TariffTab.Special -> SpecialTab(vm = specialVm)
        }
        SummaryFooter()
    }

    // Diálogos crear/editar — uno por tipo de entidad.  Cada VM gestiona el
    // suyo (showCreate / editing).
    if (tariffVm.showCreate.value || tariffVm.editing.value != null) {
        TariffEditDialog(
            initial = tariffVm.editing.value,
            onCancel = { tariffVm.closeDialogs() },
            onSubmit = { tariffVm.submit(it) },
            submitting = tariffVm.loading.value,
        )
    }
    if (planVm.showCreate.value || planVm.editing.value != null) {
        TariffPlanEditDialog(
            initial = planVm.editing.value,
            onCancel = { planVm.closeDialogs() },
            onSubmit = { planVm.submit(it) },
            submitting = planVm.loading.value,
        )
    }
    if (specialVm.showCreate.value || specialVm.editing.value != null) {
        SpecialTariffEditDialog(
            initial = specialVm.editing.value,
            onCancel = { specialVm.closeDialogs() },
            onSubmit = { specialVm.submit(it) },
            submitting = specialVm.loading.value,
        )
    }
}

/** Versión sin handlers — se usa cuando se renderiza dentro del shell. */
@Composable
fun TariffManagementScreen() {
    TariffManagementScreen(onBack = { })
}

internal enum class TariffTab(val label: String) {
    Hourly("Tarifas por Hora"),
    Subscriptions("Planes de Suscripción"),
    Special("Tarifas Especiales"),
}

@Composable
private fun TabBar(active: TariffTab, onChange: (TariffTab) -> Unit) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        TariffTab.entries.forEach { t ->
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

// ────────────────────────────────────────────────────────────────────────────
// Datos hardcoded (mockups Figma).  Reemplazables por un Repositorio real en
// la Fase B del plan del módulo Admin.
// ────────────────────────────────────────────────────────────────────────────

internal data class HourlyTariff(val vehicleType: String, val rateCents: Long, val description: String)
internal data class SubscriptionPlan(val name: String, val days: Int, val priceCents: Long, val discountPct: Int, val active: Boolean)
internal data class SpecialRate(val name: String, val multiplier: Double, val description: String, val active: Boolean)

internal val hourlyRates = listOf(
    HourlyTariff("Automóvil", 2_500_00L, "Vehículo estándar de 4 ruedas"),
    HourlyTariff("Motocicleta", 1_500_00L, "Vehículo de 2 ruedas"),
    HourlyTariff("Camioneta", 3_500_00L, "Pick-up, SUV y similares"),
    HourlyTariff("Van/Furgón", 4_000_00L, "Vehículos comerciales grandes"),
)

internal val subscriptionPlans = listOf(
    SubscriptionPlan("Mensual", 30, 45_000_00L, 0, true),
    SubscriptionPlan("Trimestral", 90, 120_000_00L, 7, true),
    SubscriptionPlan("Semestral", 180, 230_000_00L, 10, true),
    SubscriptionPlan("Anual", 365, 420_000_00L, 17, true),
)

internal val specialRates = listOf(
    SpecialRate("Fin de Semana", 1.2, "Sábados y domingos", true),
    SpecialRate("Nocturno", 0.8, "10:00 PM — 6:00 AM", true),
    SpecialRate("Festivos", 1.5, "Días festivos nacionales", false),
)
