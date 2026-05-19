package com.parking.shared.domain.repository

import com.parking.shared.domain.model.Customer
import com.parking.shared.domain.model.ParkingSession
import com.parking.shared.domain.model.PlateNumber
import com.parking.shared.domain.model.Receipt
import com.parking.shared.domain.model.Tariff
import com.parking.shared.domain.model.VehicleType
import com.parking.shared.domain.model.Zone
import kotlinx.coroutines.flow.Flow

/*
 * Interfaces de repositorio (Clean Architecture).
 *
 * El dominio depende SOLO de estas interfaces; las implementaciones viven
 * en data/local (SQLDelight) y data/api (Ktor).  Esto permite testear el
 * dominio sin DB ni red.
 */

interface SessionRepository {
    suspend fun openSession(plate: PlateNumber, type: VehicleType, zoneId: String?): ParkingSession
    suspend fun closeSession(sessionId: String): ParkingSession
    suspend fun findActiveByPlate(plate: PlateNumber): ParkingSession?
    fun watchActive(): Flow<List<ParkingSession>>
}

interface TariffRepository {
    suspend fun currentTariffFor(type: VehicleType): Tariff?
    suspend fun upsertAll(tariffs: List<Tariff>)
    fun watchAll(): Flow<List<Tariff>>
}

interface ZoneRepository {
    suspend fun allZones(): List<Zone>
    suspend fun updateOccupancy(zoneId: String, delta: Int)
    fun watchAll(): Flow<List<Zone>>
}

interface CustomerRepository {
    suspend fun findByDocument(document: String): Customer?
    suspend fun upsertAll(customers: List<Customer>)
    fun watchMonthlyHolders(): Flow<List<Customer>>
}

interface ReceiptRepository {
    suspend fun create(receipt: Receipt): Receipt
    suspend fun markSynced(localId: String, serverId: String)
    suspend fun markFailed(localId: String, reason: String)
    suspend fun pendingForSync(): List<Receipt>
}
