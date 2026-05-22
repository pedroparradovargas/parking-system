package com.parking.backend

import com.parking.backend.routes.AdminTariffRepository
import com.parking.backend.routes.AdminTariffsT
import com.parking.shared.data.api.dto.UpsertTariffRequest
import com.parking.shared.domain.model.VehicleType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests del versionado de tarifas.  Aplica la política:
 *   - Crear nueva versión cierra la anterior con `valid_to = nueva.valid_from`.
 *   - `update` produce una nueva fila y NO sobrescribe la anterior.
 *   - `close` solo cierra (no crea).
 *   - El listado por defecto trae sólo vigentes; con `historic=true` trae todas.
 *   - Cada parking_id mantiene cadena propia (multi-tenant).
 */
class AdminTariffRepositoryTest {

    private val parkingA: String = UUID.randomUUID().toString()
    private val parkingB: String = UUID.randomUUID().toString()

    @BeforeTest
    fun setupDatabase() {
        Database.connect(
            url = "jdbc:h2:mem:admin_tariff_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )
        transaction { SchemaUtils.create(AdminTariffsT) }
    }

    @AfterTest
    fun teardownDatabase() {
        transaction { SchemaUtils.drop(AdminTariffsT) }
    }

    private fun sample(
        vehicleType: VehicleType = VehicleType.CAR,
        firstHourCents: Long = 250000,
        validFrom: Long? = null,
    ) = UpsertTariffRequest(
        vehicleType = vehicleType,
        firstHourCents = firstHourCents,
        subsequentHourCents = 200000,
        nightSurchargePercent = 20,
        nightFromIso = "22:00",
        nightToIso = "06:00",
        graceMinutes = 15,
        ivaPercent = 19,
        validFromMillis = validFrom,
        validToMillis = null,
    )

    @Test
    fun createGeneraTarifaVigenteSinValidTo() {
        val t = AdminTariffRepository.create(parkingA, sample())
        t.validFromMillis shouldBeGreaterThan 0L
        t.validToMillis.shouldBeNull()
        t.firstHourCents shouldBe 250000
        t.vehicleType shouldBe VehicleType.CAR
    }

    @Test
    fun updateCreaNuevaVersionYCierraLaAnterior() {
        val v1 = AdminTariffRepository.create(parkingA, sample(firstHourCents = 200000))
        Thread.sleep(5) // garantiza que la nueva versión tenga timestamp posterior
        val v2 = AdminTariffRepository.update(parkingA, v1.id, sample(firstHourCents = 300000))

        v2.id shouldNotBe v1.id
        v2.firstHourCents shouldBe 300000
        v2.validToMillis.shouldBeNull()

        // La versión 1 ahora debe estar cerrada con valid_to = v2.validFromMillis.
        val historic = AdminTariffRepository.list(parkingA, historic = true)
        historic shouldHaveSize 2
        val closedV1 = historic.first { it.id == v1.id }
        closedV1.validToMillis.shouldNotBeNull()
        closedV1.validToMillis shouldBe v2.validFromMillis

        // El listado por defecto trae sólo vigentes (v2).
        val current = AdminTariffRepository.list(parkingA, historic = false)
        current shouldHaveSize 1
        current.first().id shouldBe v2.id
    }

    @Test
    fun closeMarcaValidToSinCrearNuevaVersion() {
        val v1 = AdminTariffRepository.create(parkingA, sample())
        val rowsAffected = AdminTariffRepository.close(parkingA, v1.id)
        rowsAffected shouldBe 1

        val all = AdminTariffRepository.list(parkingA, historic = true)
        all shouldHaveSize 1
        all.first().validToMillis.shouldNotBeNull()

        val current = AdminTariffRepository.list(parkingA, historic = false)
        current shouldHaveSize 0   // ya no hay vigentes
    }

    @Test
    fun createDosVecesParaMismoVehicleTypeCierraAutomaticamenteLaAnterior() {
        val v1 = AdminTariffRepository.create(parkingA, sample(firstHourCents = 100000))
        Thread.sleep(5)
        val v2 = AdminTariffRepository.create(parkingA, sample(firstHourCents = 500000))

        val historic = AdminTariffRepository.list(parkingA, historic = true)
        historic shouldHaveSize 2

        // V1 debe quedar cerrada exactamente al validFrom de V2.
        val closed = historic.first { it.id == v1.id }
        closed.validToMillis shouldBe v2.validFromMillis

        // V2 vigente.
        val current = AdminTariffRepository.list(parkingA, historic = false)
        current shouldHaveSize 1
        current.first().id shouldBe v2.id
    }

    @Test
    fun tariffsDistintosVehicleTypeNoSeAfectanEntreSi() {
        val car = AdminTariffRepository.create(parkingA, sample(VehicleType.CAR))
        val moto = AdminTariffRepository.create(parkingA, sample(VehicleType.MOTORCYCLE, firstHourCents = 150000))
        Thread.sleep(5)  // garantiza que la nueva versión tenga validFrom > car.validFrom

        // Crear otra de CAR cierra la primera de CAR, pero no toca MOTORCYCLE.
        AdminTariffRepository.create(parkingA, sample(VehicleType.CAR, firstHourCents = 999999))

        val historic = AdminTariffRepository.list(parkingA, historic = true)
        historic shouldHaveSize 3

        val carEntries = historic.filter { it.vehicleType == VehicleType.CAR }
        carEntries shouldHaveSize 2
        carEntries.first { it.id == car.id }.validToMillis.shouldNotBeNull()  // cerrada

        val motoEntries = historic.filter { it.vehicleType == VehicleType.MOTORCYCLE }
        motoEntries shouldHaveSize 1
        motoEntries.first().id shouldBe moto.id
        motoEntries.first().validToMillis.shouldBeNull()                       // sigue vigente
    }

    @Test
    fun parkingsAisladosNoComparten() {
        AdminTariffRepository.create(parkingA, sample())
        AdminTariffRepository.create(parkingB, sample())

        AdminTariffRepository.list(parkingA, historic = true) shouldHaveSize 1
        AdminTariffRepository.list(parkingB, historic = true) shouldHaveSize 1
        // No se cruzan: cerrar la de A no toca B.
        val a1 = AdminTariffRepository.list(parkingA, historic = false).first()
        AdminTariffRepository.close(parkingA, a1.id)
        AdminTariffRepository.list(parkingA, historic = false) shouldHaveSize 0
        AdminTariffRepository.list(parkingB, historic = false) shouldHaveSize 1
    }

    @Test
    fun updateRechazaTarifaDeOtroParking() {
        val v1 = AdminTariffRepository.create(parkingA, sample())
        val err = runCatching { AdminTariffRepository.update(parkingB, v1.id, sample()) }
        err.isFailure shouldBe true
    }

    @Test
    fun createRechazaParametrosInvalidos() {
        val badPercent = runCatching {
            AdminTariffRepository.create(parkingA, sample().copy(nightSurchargePercent = 200))
        }
        badPercent.isFailure shouldBe true

        val negativePrice = runCatching {
            AdminTariffRepository.create(parkingA, sample().copy(firstHourCents = -1))
        }
        negativePrice.isFailure shouldBe true
    }
}
