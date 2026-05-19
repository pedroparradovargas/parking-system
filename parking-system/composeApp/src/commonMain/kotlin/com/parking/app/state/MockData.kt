package com.parking.app.state

import com.parking.shared.domain.model.ParkingSession
import com.parking.shared.domain.model.PlateNumber
import com.parking.shared.domain.model.SessionStatus
import com.parking.shared.domain.model.Tariff
import com.parking.shared.domain.model.VehicleType
import com.parking.shared.domain.model.Zone
import com.parking.shared.domain.tariff.TariffCalculator
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.minutes

/**
 * Datos semilla para la demo de UI.  En producción salen del backend
 * sincronizado a SQLDelight (offline-first, Regla 9).
 */
internal object MockData {

    const val PARKING_ID = "00000000-0000-0000-0000-000000000001"
    const val PARKING_NAME = "Parqueadero Central"

    /** Tarifa demo CO$: 4 000 primera hora, 3 500 cada hora siguiente, recargo nocturno 20%, gracia 15min, IVA 19%. */
    fun demoTariff(parkingId: String): Tariff = Tariff(
        id = "tariff-demo",
        parkingId = parkingId,
        vehicleType = VehicleType.CAR,
        firstHourCents = 4_000_00L,
        subsequentHourCents = 3_500_00L,
        nightSurchargePercent = 20,
        nightFrom = LocalTime(22, 0),
        nightTo = LocalTime(6, 0),
        graceMinutes = 15,
        ivaPercent = 19,
        validFrom = Instant.fromEpochMilliseconds(0),
    )

    fun seedZones(parkingId: String): List<Zone> = listOf(
        Zone(id = "z-a", parkingId = parkingId, code = "A", capacity = 50, currentOccupancy = 38,
            allowedVehicleTypes = listOf(VehicleType.CAR, VehicleType.MOTORCYCLE)),
        Zone(id = "z-b", parkingId = parkingId, code = "B", capacity = 80, currentOccupancy = 72,
            allowedVehicleTypes = listOf(VehicleType.CAR)),
        Zone(id = "z-c", parkingId = parkingId, code = "C", capacity = 40, currentOccupancy = 12,
            allowedVehicleTypes = listOf(VehicleType.MOTORCYCLE, VehicleType.BICYCLE)),
        Zone(id = "z-d", parkingId = parkingId, code = "D", capacity = 100, currentOccupancy = 95,
            allowedVehicleTypes = listOf(VehicleType.CAR, VehicleType.TRUCK)),
        Zone(id = "z-e", parkingId = parkingId, code = "E", capacity = 30, currentOccupancy = 5,
            allowedVehicleTypes = listOf(VehicleType.BUS, VehicleType.TRUCK)),
        Zone(id = "z-f", parkingId = parkingId, code = "F", capacity = 60, currentOccupancy = 48,
            allowedVehicleTypes = listOf(VehicleType.CAR, VehicleType.MOTORCYCLE)),
    )

    /** 6 sesiones activas con tiempos variados (entre 5 min y 4h). */
    fun seedActiveSessions(): List<ParkingSession> {
        val now = Clock.System.now()
        val plates = listOf(
            Triple("ABC123", VehicleType.CAR, 5),
            Triple("XYZ987", VehicleType.CAR, 45),
            Triple("MOT012", VehicleType.MOTORCYCLE, 78),
            Triple("DEF456", VehicleType.CAR, 120),
            Triple("TRK001", VehicleType.TRUCK, 200),
            Triple("BIC777", VehicleType.BICYCLE, 14),
        )
        return plates.mapIndexed { idx, (plate, type, mins) ->
            ParkingSession(
                id = "active-$idx",
                parkingId = PARKING_ID,
                plate = PlateNumber(plate),
                vehicleType = type,
                zoneId = "z-${('a' + (idx % 4))}",
                entryAt = now - mins.minutes,
                status = SessionStatus.ACTIVE,
            )
        }
    }

    /** 12 sesiones cerradas hoy con cobros — para alimentar Dashboard y gráfica. */
    fun seedClosedToday(): List<ClosedSessionEntry> {
        val now = Clock.System.now()
        val data = listOf(
            // (minutos atrás del cierre, duración en minutos, placa, tipo)
            Triple(15, 95, "JKL741"),
            Triple(40, 130, "PQR222"),
            Triple(72, 60, "MOT555"),
            Triple(95, 200, "TRK010"),
            Triple(120, 45, "CAR901"),
            Triple(150, 75, "DEF111"),
            Triple(180, 35, "MOT300"),
            Triple(210, 150, "VAN404"),
            Triple(245, 220, "CAR707"),
            Triple(290, 50, "MOT812"),
            Triple(320, 110, "ABC555"),
            Triple(360, 85, "XYZ018"),
        )
        val tariff = demoTariff(PARKING_ID)
        // Se pasa explícitamente la TZ del navegador para evitar usar el default
        // `TimeZone.of("America/Bogota")` que puede fallar en Wasm-JS si Intl
        // aún no ha cargado la zona en el momento de la primera composición.
        val tz = runCatching { TimeZone.currentSystemDefault() }.getOrDefault(TimeZone.UTC)
        return data.mapIndexed { idx, (agoMin, durMin, plate) ->
            val exit = now - agoMin.minutes
            val entry = exit - durMin.minutes
            val type = if (plate.startsWith("MOT")) VehicleType.MOTORCYCLE
                else if (plate.startsWith("TRK") || plate.startsWith("VAN")) VehicleType.TRUCK
                else VehicleType.CAR
            val breakdown = TariffCalculator.calculate(tariff, entry, exit, timeZone = tz)
            ClosedSessionEntry(
                ParkingSession(
                    id = "closed-$idx",
                    parkingId = PARKING_ID,
                    plate = PlateNumber(plate),
                    vehicleType = type,
                    zoneId = null,
                    entryAt = entry,
                    exitAt = exit,
                    status = SessionStatus.CLOSED,
                ),
                breakdown,
            )
        }
    }
}
