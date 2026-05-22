package com.parking.shared.data.api

import com.parking.shared.data.api.dto.AdminCustomerDto
import com.parking.shared.data.api.dto.AdminUserDto
import com.parking.shared.data.api.dto.AssignMonthlyRequest
import com.parking.shared.data.api.dto.AuditEntryRowDto
import com.parking.shared.data.api.dto.AuditVerifyResponse
import com.parking.shared.data.api.dto.ParkingConfigDto
import com.parking.shared.data.api.dto.UpsertParkingConfigRequest
import com.parking.shared.data.api.dto.CashClosingReportDto
import com.parking.shared.data.api.dto.MonthlyCustomersReportDto
import com.parking.shared.data.api.dto.TopPlatesReportDto
import com.parking.shared.data.api.dto.CreateUserRequest
import com.parking.shared.data.api.dto.MonthlyPaymentDto
import com.parking.shared.data.api.dto.UpsertCustomerRequest
import com.parking.shared.data.api.dto.EnableTotpResponse
import com.parking.shared.data.api.dto.HolidayDto
import com.parking.shared.data.api.dto.LoginRequest
import com.parking.shared.data.api.dto.ResetPasswordResponse
import com.parking.shared.data.api.dto.UpdateUserRequest
import com.parking.shared.data.api.dto.LoginResponse
import com.parking.shared.data.api.dto.LprResultDto
import com.parking.shared.data.api.dto.OccupancyReportDto
import com.parking.shared.data.api.dto.RevenueReportDto
import com.parking.shared.data.api.dto.SessionDto
import com.parking.shared.data.api.dto.SpecialTariffDto
import com.parking.shared.data.api.dto.SyncPullResponse
import com.parking.shared.data.api.dto.SyncPushRequest
import com.parking.shared.data.api.dto.SyncPushResponse
import com.parking.shared.data.api.dto.TariffDto
import com.parking.shared.data.api.dto.TariffPlanDto
import com.parking.shared.data.api.dto.UpsertHolidayRequest
import com.parking.shared.data.api.dto.UpsertSpecialTariffRequest
import com.parking.shared.data.api.dto.UpsertTariffPlanRequest
import com.parking.shared.data.api.dto.UpsertTariffRequest
import com.parking.shared.data.api.dto.UpsertZoneRequest
import com.parking.shared.data.api.dto.ZoneDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

/**
 * Cliente HTTP del API REST del backend.
 *
 * No mantiene estado mutable: el HttpClient se inyecta y se reutiliza
 * a lo largo de toda la vida de la app cliente.
 */
class ParkingApiClient(private val client: HttpClient) {

    // ---- Auth ----
    suspend fun login(request: LoginRequest): LoginResponse =
        client.post("api/v1/auth/login") { setBody(request) }.body()

    suspend fun refresh(refreshToken: String): LoginResponse =
        client.post("api/v1/auth/refresh") { setBody(mapOf("refreshToken" to refreshToken)) }.body()

    // ---- Catálogos ----
    suspend fun tariffs(parkingId: String): List<TariffDto> =
        client.get("api/v1/parkings/$parkingId/tariffs").body()

    suspend fun zones(parkingId: String): List<ZoneDto> =
        client.get("api/v1/parkings/$parkingId/zones").body()

    // ---- Sesiones ----
    suspend fun openSession(body: SessionDto): SessionDto =
        client.post("api/v1/sessions") { setBody(body) }.body()

    suspend fun closeSession(sessionId: String, body: SessionDto): SessionDto =
        client.post("api/v1/sessions/$sessionId/close") { setBody(body) }.body()

    // ---- Sincronización ----
    suspend fun pushQueue(payload: SyncPushRequest): SyncPushResponse =
        client.post("api/v1/sync/push") { setBody(payload) }.body()

    suspend fun pullChanges(parkingId: String, sinceMillis: Long): SyncPullResponse =
        client.get("api/v1/sync/pull?parkingId=$parkingId&since=$sinceMillis").body()

    // ---- IA / LPR ----
    suspend fun recognizePlate(imageBase64: String): LprResultDto =
        client.post("api/v1/ai/lpr") { setBody(mapOf("imageBase64" to imageBase64)) }.body()

    // ---- Reportes ejecutivos ----
    suspend fun revenueReport(parkingId: String, fromIso: String, toIso: String): RevenueReportDto =
        client.get("api/v1/parkings/$parkingId/reports/revenue?from=$fromIso&to=$toIso").body()

    suspend fun occupancyReport(parkingId: String): OccupancyReportDto =
        client.get("api/v1/parkings/$parkingId/reports/occupancy").body()

    // ---- Admin: Tarifas (versionadas) ----
    suspend fun adminListTariffs(parkingId: String, historic: Boolean = false): List<TariffDto> =
        client.get("api/v1/parkings/$parkingId/tariffs${if (historic) "?historic=true" else ""}").body()

    suspend fun adminCreateTariff(parkingId: String, body: UpsertTariffRequest): TariffDto =
        client.post("api/v1/parkings/$parkingId/tariffs") { setBody(body) }.body()

    suspend fun adminUpdateTariff(parkingId: String, tariffId: String, body: UpsertTariffRequest): TariffDto =
        client.put("api/v1/parkings/$parkingId/tariffs/$tariffId") { setBody(body) }.body()

    suspend fun adminCloseTariff(parkingId: String, tariffId: String) {
        client.delete("api/v1/parkings/$parkingId/tariffs/$tariffId")
    }

    // ---- Admin: Planes de mensualidad ----
    suspend fun adminListTariffPlans(parkingId: String): List<TariffPlanDto> =
        client.get("api/v1/parkings/$parkingId/tariff-plans").body()

    suspend fun adminCreateTariffPlan(parkingId: String, body: UpsertTariffPlanRequest): TariffPlanDto =
        client.post("api/v1/parkings/$parkingId/tariff-plans") { setBody(body) }.body()

    suspend fun adminUpdateTariffPlan(parkingId: String, planId: String, body: UpsertTariffPlanRequest): TariffPlanDto =
        client.put("api/v1/parkings/$parkingId/tariff-plans/$planId") { setBody(body) }.body()

    suspend fun adminDeleteTariffPlan(parkingId: String, planId: String) {
        client.delete("api/v1/parkings/$parkingId/tariff-plans/$planId")
    }

    // ---- Admin: Tarifas especiales ----
    suspend fun adminListSpecialTariffs(parkingId: String): List<SpecialTariffDto> =
        client.get("api/v1/parkings/$parkingId/special-tariffs").body()

    suspend fun adminCreateSpecialTariff(parkingId: String, body: UpsertSpecialTariffRequest): SpecialTariffDto =
        client.post("api/v1/parkings/$parkingId/special-tariffs") { setBody(body) }.body()

    suspend fun adminUpdateSpecialTariff(parkingId: String, specialId: String, body: UpsertSpecialTariffRequest): SpecialTariffDto =
        client.put("api/v1/parkings/$parkingId/special-tariffs/$specialId") { setBody(body) }.body()

    suspend fun adminDeleteSpecialTariff(parkingId: String, specialId: String) {
        client.delete("api/v1/parkings/$parkingId/special-tariffs/$specialId")
    }

    // ---- Admin: Zonas ----
    suspend fun adminListZones(parkingId: String): List<ZoneDto> =
        client.get("api/v1/parkings/$parkingId/zones").body()

    suspend fun adminCreateZone(parkingId: String, body: UpsertZoneRequest): ZoneDto =
        client.post("api/v1/parkings/$parkingId/zones") { setBody(body) }.body()

    suspend fun adminUpdateZone(parkingId: String, zoneId: String, body: UpsertZoneRequest): ZoneDto =
        client.put("api/v1/parkings/$parkingId/zones/$zoneId") { setBody(body) }.body()

    suspend fun adminDeleteZone(parkingId: String, zoneId: String) {
        client.delete("api/v1/parkings/$parkingId/zones/$zoneId")
    }

    // ---- Admin: Usuarios + 2FA ----
    suspend fun adminListUsers(parkingId: String): List<AdminUserDto> =
        client.get("api/v1/parkings/$parkingId/users").body()

    suspend fun adminCreateUser(parkingId: String, body: CreateUserRequest): AdminUserDto =
        client.post("api/v1/parkings/$parkingId/users") { setBody(body) }.body()

    suspend fun adminUpdateUser(parkingId: String, userId: String, body: UpdateUserRequest): AdminUserDto =
        client.put("api/v1/parkings/$parkingId/users/$userId") { setBody(body) }.body()

    suspend fun adminDisableUser(parkingId: String, userId: String) {
        client.delete("api/v1/parkings/$parkingId/users/$userId")
    }

    suspend fun adminResetPassword(parkingId: String, userId: String): ResetPasswordResponse =
        client.post("api/v1/parkings/$parkingId/users/$userId/reset-password").body()

    suspend fun adminEnable2fa(parkingId: String, userId: String): EnableTotpResponse =
        client.post("api/v1/parkings/$parkingId/users/$userId/2fa/enable").body()

    suspend fun adminDisable2fa(parkingId: String, userId: String) {
        client.post("api/v1/parkings/$parkingId/users/$userId/2fa/disable")
    }

    // ---- Admin: Clientes (mensualistas) ----
    suspend fun adminListCustomers(parkingId: String): List<AdminCustomerDto> =
        client.get("api/v1/parkings/$parkingId/customers").body()

    suspend fun adminListExpiringCustomers(parkingId: String, days: Int = 7): List<AdminCustomerDto> =
        client.get("api/v1/parkings/$parkingId/customers/expiring-soon?days=$days").body()

    suspend fun adminCreateCustomer(parkingId: String, body: UpsertCustomerRequest): AdminCustomerDto =
        client.post("api/v1/parkings/$parkingId/customers") { setBody(body) }.body()

    suspend fun adminUpdateCustomer(parkingId: String, customerId: String, body: UpsertCustomerRequest): AdminCustomerDto =
        client.put("api/v1/parkings/$parkingId/customers/$customerId") { setBody(body) }.body()

    suspend fun adminAssignMonthly(parkingId: String, customerId: String, body: AssignMonthlyRequest): MonthlyPaymentDto =
        client.post("api/v1/parkings/$parkingId/customers/$customerId/monthly") { setBody(body) }.body()

    // ---- Admin: Reportes adicionales ----
    suspend fun adminCashClosing(parkingId: String, fromIso: String, toIso: String): CashClosingReportDto =
        client.get("api/v1/parkings/$parkingId/reports/cash-closing?from=$fromIso&to=$toIso").body()

    suspend fun adminCashClosingCsv(parkingId: String, fromIso: String, toIso: String): String =
        client.get("api/v1/parkings/$parkingId/reports/cash-closing.csv?from=$fromIso&to=$toIso").body()

    suspend fun adminTopPlates(parkingId: String, fromIso: String, toIso: String, limit: Int = 10): TopPlatesReportDto =
        client.get("api/v1/parkings/$parkingId/reports/top-plates?from=$fromIso&to=$toIso&limit=$limit").body()

    suspend fun adminMonthlyCustomers(parkingId: String): MonthlyCustomersReportDto =
        client.get("api/v1/parkings/$parkingId/reports/monthly-customers").body()

    // ---- Admin: Configuración del parqueadero ----
    suspend fun adminGetConfig(parkingId: String): ParkingConfigDto =
        client.get("api/v1/parkings/$parkingId/config").body()

    suspend fun adminUpdateConfig(parkingId: String, body: UpsertParkingConfigRequest): ParkingConfigDto =
        client.put("api/v1/parkings/$parkingId/config") { setBody(body) }.body()

    // ---- Admin: Audit log ----
    suspend fun adminQueryAudit(
        parkingId: String,
        entity: String? = null,
        action: String? = null,
        fromMs: Long? = null,
        toMs: Long? = null,
        limit: Int = 100,
        offset: Long = 0,
    ): List<AuditEntryRowDto> {
        val q = buildList {
            entity?.let { add("entity=$it") }
            action?.let { add("action=$it") }
            fromMs?.let { add("from=$it") }
            toMs?.let { add("to=$it") }
            add("limit=$limit")
            add("offset=$offset")
        }.joinToString("&")
        return client.get("api/v1/parkings/$parkingId/audit?$q").body()
    }

    suspend fun adminVerifyAudit(parkingId: String): AuditVerifyResponse =
        client.post("api/v1/parkings/$parkingId/audit/verify").body()

    // ---- Admin: Festivos ----
    suspend fun adminListHolidays(parkingId: String): List<HolidayDto> =
        client.get("api/v1/parkings/$parkingId/holidays").body()

    suspend fun adminCreateHoliday(parkingId: String, body: UpsertHolidayRequest): HolidayDto =
        client.post("api/v1/parkings/$parkingId/holidays") { setBody(body) }.body()

    suspend fun adminDeleteHoliday(parkingId: String, holidayId: String) {
        client.delete("api/v1/parkings/$parkingId/holidays/$holidayId")
    }
}
