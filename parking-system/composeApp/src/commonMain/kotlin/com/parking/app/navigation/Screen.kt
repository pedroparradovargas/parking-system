package com.parking.app.navigation

import androidx.compose.runtime.Composable
import com.parking.app.ui.screens.availability.ParkingAvailabilityScreen
import com.parking.app.ui.screens.cashier.CashierScreen
import com.parking.app.ui.screens.dashboard.DashboardScreen
import com.parking.app.ui.screens.mainmenu.MainMenuScreen
import com.parking.app.ui.screens.payment.PaymentInterfaceScreen
import com.parking.app.ui.screens.registration.ClientRegistrationScreen
import com.parking.app.ui.screens.rented.RentedVehicleEntryScreen
import com.parking.app.ui.screens.reservation.PaymentQrScreen
import com.parking.app.ui.screens.reservation.ReservationScreen
import com.parking.app.ui.screens.support.CustomerSupportScreen
import com.parking.app.ui.screens.audit.AuditLogScreen
import com.parking.app.ui.screens.config.ParkingConfigScreen
import com.parking.app.ui.screens.customers.AdminCustomersScreen
import com.parking.app.ui.screens.reports.AdminReportsScreen
import com.parking.app.ui.screens.tariff.TariffManagementScreen
import com.parking.app.ui.screens.users.AdminUsersScreen
import com.parking.app.ui.screens.zones.AdminZonesScreen

/**
 * Modelo de navegación que sigue las 9 secciones del mockup Figma
 * "Sistema de Parqueadero Inteligente (Community)".  No usamos
 * `androidx.navigation` para no acoplar la UI compartida a Android.
 *
 * El AppShell muestra header + tab nav para todas las pantallas (no
 * hay pantalla de login en el Figma — el rol se cambia desde el header).
 */
sealed interface Screen {
    data object MainMenu : Screen
    data object RentedEntry : Screen
    data object EventualEntry : Screen
    data object Payment : Screen
    data object ClientRegistration : Screen
    data object TariffManagement : Screen
    data object Availability : Screen
    data object Reports : Screen
    data object Support : Screen
    data object AdminZones : Screen
    data object AdminUsers : Screen
    data object AdminCustomers : Screen
    data object AdminReports : Screen
    data object AdminConfig : Screen
    data object AdminAudit : Screen
    data object AdminDevices : Screen

    /** Flujo interno de Reservas → QR (no aparece en tab nav). */
    data object Cashier : Screen
    data object Reservation : Screen
    data class PaymentQr(val reservationId: String) : Screen
}

/**
 * Despacha la pantalla activa.  Todas las pantallas del mockup ya están portadas.
 */
@Composable
fun AppNav(current: Screen, navigateTo: (Screen) -> Unit) {
    val back: () -> Unit = { navigateTo(Screen.MainMenu) }
    when (val s = current) {
        is Screen.MainMenu -> MainMenuScreen(onNavigate = navigateTo)
        is Screen.RentedEntry -> RentedVehicleEntryScreen(onBack = back)
        is Screen.EventualEntry -> CashierScreen()
        is Screen.Payment -> PaymentInterfaceScreen(onBack = back)
        is Screen.ClientRegistration -> ClientRegistrationScreen(onBack = back)
        is Screen.TariffManagement -> TariffManagementScreen(onBack = back)
        is Screen.Availability -> ParkingAvailabilityScreen(onBack = back)
        is Screen.Reports -> DashboardScreen()
        is Screen.Support -> CustomerSupportScreen(onBack = back)
        is Screen.AdminZones -> AdminZonesScreen(onBack = back)
        is Screen.AdminUsers -> AdminUsersScreen(onBack = back)
        is Screen.AdminCustomers -> AdminCustomersScreen(onBack = back)
        is Screen.AdminReports -> AdminReportsScreen(onBack = back)
        is Screen.AdminConfig -> ParkingConfigScreen(onBack = back)
        is Screen.AdminAudit -> AuditLogScreen(onBack = back)
        is Screen.AdminDevices -> com.parking.app.ui.screens.devices.DeviceStatusScreen(onBack = back)

        is Screen.Cashier -> CashierScreen()
        is Screen.Reservation -> ReservationScreen(onConfirm = { id -> navigateTo(Screen.PaymentQr(id)) })
        is Screen.PaymentQr -> PaymentQrScreen(reservationId = s.reservationId, onDone = { navigateTo(Screen.MainMenu) })
    }
}
