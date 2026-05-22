package com.parking.backend

import com.parking.backend.routes.AdminZoneRepository
import com.parking.backend.routes.AdminZonesT
import com.parking.shared.data.api.dto.UpsertZoneRequest
import com.parking.shared.domain.model.VehicleType
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests CRUD del [AdminZoneRepository] con H2 in-memory.
 * Cubre invariantes:
 *  - El listado respeta `displayOrder` y luego `code`.
 *  - El código se valida no vacío y de longitud ≤ 16.
 *  - `under_maintenance` no puede exceder `capacity`.
 *  - `delete` rechaza zonas con `currentOccupancy > 0`.
 *  - Multi-tenant: zonas de un parking no se cruzan con otro.
 */
class AdminZoneRepositoryTest {

    private val parkingA: String = UUID.randomUUID().toString()
    private val parkingB: String = UUID.randomUUID().toString()

    @BeforeTest
    fun setupDatabase() {
        Database.connect(
            url = "jdbc:h2:mem:admin_zone_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )
        transaction { SchemaUtils.create(AdminZonesT) }
    }

    @AfterTest
    fun teardownDatabase() {
        transaction { SchemaUtils.drop(AdminZonesT) }
    }

    private fun sample(
        code: String = "A1",
        capacity: Int = 20,
        order: Int = 0,
        types: List<VehicleType> = listOf(VehicleType.CAR),
    ) = UpsertZoneRequest(
        code = code,
        capacity = capacity,
        allowedVehicleTypes = types,
        underMaintenance = 0,
        enabled = true,
        displayOrder = order,
        notes = null,
    )

    @Test
    fun createGuardaTodosLosCampos() {
        val z = AdminZoneRepository.create(
            parkingA,
            sample(code = "VIP", capacity = 50, order = 5, types = listOf(VehicleType.CAR, VehicleType.MOTORCYCLE))
                .copy(notes = "Zona VIP techada"),
        )
        z.code shouldBe "VIP"
        z.capacity shouldBe 50
        z.currentOccupancy shouldBe 0
        z.displayOrder shouldBe 5
        z.enabled shouldBe true
        z.notes shouldBe "Zona VIP techada"
        z.allowedVehicleTypes shouldHaveSize 2
        z.allowedVehicleTypes shouldContain VehicleType.MOTORCYCLE
    }

    @Test
    fun listOrdenaPorDisplayOrderYLuegoPorCode() {
        AdminZoneRepository.create(parkingA, sample(code = "B", order = 0))
        AdminZoneRepository.create(parkingA, sample(code = "A", order = 0))
        AdminZoneRepository.create(parkingA, sample(code = "Z", order = -1).copy(displayOrder = 0))
        AdminZoneRepository.create(parkingA, sample(code = "C", order = 99))

        val list = AdminZoneRepository.list(parkingA).map { it.code }
        // displayOrder 0 vienen primero ordenados por code; luego displayOrder 99.
        list shouldBe listOf("A", "B", "Z", "C")
    }

    @Test
    fun updateMutaCamposEnMismaFila() {
        val v1 = AdminZoneRepository.create(parkingA, sample(capacity = 10))
        val updated = AdminZoneRepository.update(
            parkingA,
            v1.id,
            sample(code = "A1", capacity = 25).copy(notes = "Ampliada"),
        )
        updated.id shouldBe v1.id   // misma fila — NO crea nueva
        updated.capacity shouldBe 25
        updated.notes shouldBe "Ampliada"
        AdminZoneRepository.list(parkingA) shouldHaveSize 1
    }

    @Test
    fun deleteRechazaZonaConOcupacion() {
        val v = AdminZoneRepository.create(parkingA, sample(capacity = 5))
        // Simular tráfico: subimos currentOccupancy directamente en DB.
        transaction {
            AdminZonesT.update({ AdminZonesT.id eq UUID.fromString(v.id) }) {
                it[AdminZonesT.currentOccupancy] = 2
            }
        }
        val err = runCatching { AdminZoneRepository.delete(parkingA, v.id) }
        err.isFailure shouldBe true
        AdminZoneRepository.list(parkingA) shouldHaveSize 1   // sigue ahí
    }

    @Test
    fun deleteEliminaZonaVacia() {
        val v = AdminZoneRepository.create(parkingA, sample())
        val n = AdminZoneRepository.delete(parkingA, v.id)
        n shouldBe 1
        AdminZoneRepository.list(parkingA) shouldHaveSize 0
    }

    @Test
    fun parkingsAisladosMultiTenant() {
        val a = AdminZoneRepository.create(parkingA, sample(code = "A1"))
        AdminZoneRepository.create(parkingB, sample(code = "A1"))   // mismo code, distinto parking
        AdminZoneRepository.list(parkingA) shouldHaveSize 1
        AdminZoneRepository.list(parkingB) shouldHaveSize 1

        // Borrar la del A no afecta a B.
        AdminZoneRepository.delete(parkingA, a.id)
        AdminZoneRepository.list(parkingA) shouldHaveSize 0
        AdminZoneRepository.list(parkingB) shouldHaveSize 1
    }

    @Test
    fun validacionesRechazanParametrosInvalidos() {
        val noCode = runCatching { AdminZoneRepository.create(parkingA, sample(code = "")) }
        noCode.isFailure shouldBe true

        val longCode = runCatching { AdminZoneRepository.create(parkingA, sample(code = "X".repeat(20))) }
        longCode.isFailure shouldBe true

        val negCap = runCatching { AdminZoneRepository.create(parkingA, sample(capacity = -1)) }
        negCap.isFailure shouldBe true

        val maintTooBig = runCatching {
            AdminZoneRepository.create(parkingA, sample(capacity = 5).copy(underMaintenance = 10))
        }
        maintTooBig.isFailure shouldBe true

        val noTypes = runCatching {
            AdminZoneRepository.create(parkingA, sample(types = emptyList()))
        }
        noTypes.isFailure shouldBe true
    }

    @Test
    fun updateRechazaZonaDeOtroParking() {
        val v = AdminZoneRepository.create(parkingA, sample())
        val err = runCatching { AdminZoneRepository.update(parkingB, v.id, sample(capacity = 100)) }
        err.isFailure shouldBe true
    }
}
