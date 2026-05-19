package com.parking.shared.data.api

import com.parking.shared.data.api.dto.LoginRequest
import com.parking.shared.data.api.dto.LoginResponse
import com.parking.shared.data.api.dto.LprResultDto
import com.parking.shared.data.api.dto.OccupancyReportDto
import com.parking.shared.data.api.dto.RevenueReportDto
import com.parking.shared.data.api.dto.SessionDto
import com.parking.shared.data.api.dto.SyncPullResponse
import com.parking.shared.data.api.dto.SyncPushRequest
import com.parking.shared.data.api.dto.SyncPushResponse
import com.parking.shared.data.api.dto.TariffDto
import com.parking.shared.data.api.dto.ZoneDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
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
}
