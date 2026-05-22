package com.parking.app.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.parking.shared.data.api.dto.CashClosingReportDto
import com.parking.shared.data.api.dto.MonthlyCustomersReportDto
import com.parking.shared.data.api.dto.TopPlatesReportDto
import com.parking.shared.domain.model.Money
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

private class ReportsVm(val api: ParkingApiClient, val parkingId: String, val scope: CoroutineScope) {
    val from: MutableState<String> = mutableStateOf(defaultFromIso())
    val to: MutableState<String> = mutableStateOf(defaultToIso())
    val cash: MutableState<CashClosingReportDto?> = mutableStateOf(null)
    val plates: MutableState<TopPlatesReportDto?> = mutableStateOf(null)
    val monthly: MutableState<MonthlyCustomersReportDto?> = mutableStateOf(null)
    val loading: MutableState<Boolean> = mutableStateOf(false)
    val error: MutableState<String?> = mutableStateOf(null)
    val csvSnapshot: MutableState<String?> = mutableStateOf(null)

    fun reload() = scope.launch {
        loading.value = true; error.value = null
        runCatching {
            cash.value = api.adminCashClosing(parkingId, from.value, to.value)
            plates.value = api.adminTopPlates(parkingId, from.value, to.value, limit = 10)
            monthly.value = api.adminMonthlyCustomers(parkingId)
        }.onFailure { error.value = "No se pudieron cargar reportes: ${it.message ?: it::class.simpleName}" }
        loading.value = false
    }

    fun downloadCsv() = scope.launch {
        runCatching { api.adminCashClosingCsv(parkingId, from.value, to.value) }
            .onSuccess { csvSnapshot.value = it }
            .onFailure { error.value = "No se pudo exportar CSV: ${it.message ?: it::class.simpleName}" }
    }

    fun clearCsv() { csvSnapshot.value = null }

    private fun defaultFromIso(): String {
        val tz = runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC)
        return Clock.System.now().toLocalDateTime(tz).date.minus(daysAgo = 30).toString()
    }
    private fun defaultToIso(): String {
        val tz = runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC)
        return Clock.System.now().toLocalDateTime(tz).date.toString()
    }
    private fun kotlinx.datetime.LocalDate.minus(daysAgo: Int): kotlinx.datetime.LocalDate =
        kotlinx.datetime.LocalDate.fromEpochDays(this.toEpochDays() - daysAgo)
}

@Composable
fun AdminReportsScreen(onBack: () -> Unit) {
    val spacing = LocalSpacing.current
    val appState = LocalAppState.current
    val api: ParkingApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val vm = remember { ReportsVm(api, appState.parkingId, scope) }

    LaunchedEffect(vm) { vm.reload() }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.l)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.m)) {
            BackToMenuButton(onClick = onBack)
            Box(modifier = Modifier.weight(1f)) {
                SectionHeader(
                    title = "Reportes administrativos",
                    description = "Cierre de caja, top placas, estado de mensualidades.",
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = vm.from.value, onValueChange = { vm.from.value = it.take(10) },
                label = { Text("Desde (YYYY-MM-DD)") }, singleLine = true)
            OutlinedTextField(value = vm.to.value, onValueChange = { vm.to.value = it.take(10) },
                label = { Text("Hasta") }, singleLine = true)
            Button(onClick = { vm.reload() }) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(spacing.xs))
                Text("Actualizar")
            }
            OutlinedButton(onClick = { vm.downloadCsv() }) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(spacing.xs))
                Text("Exportar CSV")
            }
        }

        vm.error.value?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        vm.monthly.value?.let { MonthlyKpis(it) }
        vm.cash.value?.let { CashClosingTable(it) }
        vm.plates.value?.let { TopPlatesTable(it) }

        vm.csvSnapshot.value?.let { csv ->
            ReportCard("CSV generado (cópialo y guárdalo en disco)") {
                Text(csv, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
                Spacer(Modifier.width(spacing.s))
                OutlinedButton(onClick = { vm.clearCsv() }) { Text("Cerrar") }
            }
        }
    }
}

@Composable
private fun MonthlyKpis(r: MonthlyCustomersReportDto) {
    val spacing = LocalSpacing.current
    val sc = LocalSemanticColors.current
    ReportCard("Estado de mensualidades") {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.l), modifier = Modifier.fillMaxWidth()) {
            KpiCol("Activas", r.activeCount.toString(), sc.greenAccent, Modifier.weight(1f))
            KpiCol("Por vencer (7d)", r.expiringSoonCount.toString(), sc.orangeAccent, Modifier.weight(1f))
            KpiCol("Vencidas", r.expiredCount.toString(), sc.redAccent, Modifier.weight(1f))
            KpiCol("Ingresos 30d", Money(r.revenueLast30DaysCents).formatCop(), sc.blueAccent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CashClosingTable(r: CashClosingReportDto) {
    val spacing = LocalSpacing.current
    val sc = LocalSemanticColors.current
    ReportCard("Cierre de caja · ${r.fromIso.take(10)} → ${r.toIso.take(10)}") {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.l)) {
            KpiCol("Total", Money(r.grandTotalCents).formatCop(), sc.greenAccent, Modifier.weight(1f))
            KpiCol("IVA", Money(r.grandIvaCents).formatCop(), sc.blueAccent, Modifier.weight(1f))
        }
        Spacer(Modifier.width(spacing.s))
        if (r.rows.isEmpty()) {
            Text("Sin movimientos en el rango.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        r.rows.take(20).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(row.dayIso, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(row.operatorName, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium)
                Text("${row.sessionsCount} ses.", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money(row.totalCents).formatCop(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun TopPlatesTable(r: TopPlatesReportDto) {
    val spacing = LocalSpacing.current
    val sc = LocalSemanticColors.current
    ReportCard("Top placas") {
        if (r.rows.isEmpty()) {
            Text("Sin datos en el rango.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        r.rows.forEachIndexed { idx, row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Text("${idx + 1}.", modifier = Modifier.width(32.dp), color = sc.purpleAccent, fontWeight = FontWeight.SemiBold)
                Text(row.plate, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text("${row.sessionsCount} sesiones", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${row.totalMinutes / 60}h", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money(row.totalCents).formatCop(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = sc.greenAccent)
            }
        }
    }
}

@Composable
private fun KpiCol(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReportCard(title: String, content: @Composable () -> Unit) {
    val spacing = LocalSpacing.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(spacing.l), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CardMembership, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(spacing.s))
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.width(spacing.s))
                StatusPill(label = "Admin", tone = StatusTone.Info, showDot = false)
            }
            content()
        }
    }
}
