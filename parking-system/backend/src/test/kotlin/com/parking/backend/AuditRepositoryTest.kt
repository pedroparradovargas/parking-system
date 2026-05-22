package com.parking.backend

import com.parking.backend.routes.AuditLogT
import com.parking.backend.routes.AuditRepository
import io.kotest.assertions.fail
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
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
 * Tests del [AuditRepository] usando H2 in-memory (modo PostgreSQL).
 * Comprueban:
 *  - El hash chain se construye correctamente entre inserciones.
 *  - `verifyChain` distingue cadena íntegra vs corrupta.
 *  - El primer registro de un parking usa "0".repeat(64) como prev_hash.
 *  - La cadena es **por parking** (multi-tenant) — dos parkings no se mezclan.
 *
 * NOTA: H2 no soporta `UUID` con default `uuid_generate_v4()` ni `JSONB` ni
 * particiones; la tabla se crea aquí desde la definición Exposed (que ya usa
 * tipos portables) en lugar de la migración Flyway.
 */
class AuditRepositoryTest {

    private val parkingA: String = UUID.randomUUID().toString()
    private val parkingB: String = UUID.randomUUID().toString()
    private val actor: String = UUID.randomUUID().toString()

    @BeforeTest
    fun setupDatabase() {
        Database.connect(
            url = "jdbc:h2:mem:audit_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )
        transaction { SchemaUtils.create(AuditLogT) }
    }

    @AfterTest
    fun teardownDatabase() {
        transaction { SchemaUtils.drop(AuditLogT) }
    }

    @Test
    fun firstEntryUsesGenesisPrevHash() {
        val hash = AuditRepository.append(
            parkingId = parkingA,
            action = "session.opened",
            entity = "session",
            entityId = "s1",
            actorUserId = actor,
            payloadJson = """{"plate":"ABC123"}""",
        )
        hash shouldHaveLength 64
        hash shouldMatch Regex("^[0-9a-f]{64}$")

        // El registro inserto debe tener prev_hash = "0" * 64.
        val rows = AuditRepository.query(parkingA)
        rows shouldHaveSize 1
        rows.first().prevHash shouldBe "0".repeat(64)
        rows.first().currentHash shouldBe hash
    }

    @Test
    fun chainEncadenaMultiplesEntradasYVerifyChainEsLimpia() {
        val h1 = AuditRepository.append(parkingA, "session.opened", "session", "s1", actor, """{"i":1}""")
        val h2 = AuditRepository.append(parkingA, "session.closed", "session", "s1", actor, """{"i":2}""")
        val h3 = AuditRepository.append(parkingA, "tariff.created", "tariff", "t1", actor, """{"i":3}""")

        // Cada hash es distinto.
        setOf(h1, h2, h3).size shouldBe 3

        // verifyChain devuelve lista vacía → cadena íntegra.
        val corrupted = AuditRepository.verifyChain(parkingA)
        corrupted shouldHaveSize 0

        // Las entradas son recuperables y vienen ordenadas DESC por timestamp.
        val rows = AuditRepository.query(parkingA, limit = 10)
        rows shouldHaveSize 3
        rows[0].currentHash shouldBe h3   // más reciente primero
        rows[1].currentHash shouldBe h2
        rows[2].currentHash shouldBe h1

        // El prev_hash de cada entrada es el currentHash de la anterior.
        rows[0].prevHash shouldBe h2
        rows[1].prevHash shouldBe h1
        rows[2].prevHash shouldBe "0".repeat(64)
    }

    @Test
    fun cadenaEsPorParkingNoGlobal() {
        AuditRepository.append(parkingA, "x.y", "x", "1", actor, """{}""")
        AuditRepository.append(parkingB, "x.y", "x", "2", actor, """{}""")

        // Cada parking arrancó su cadena con prev_hash genesis.
        val a = AuditRepository.query(parkingA)
        val b = AuditRepository.query(parkingB)
        a shouldHaveSize 1
        b shouldHaveSize 1
        a.first().prevHash shouldBe "0".repeat(64)
        b.first().prevHash shouldBe "0".repeat(64)
        // Y los current_hash son distintos (porque payload/entity_id difieren).
        a.first().currentHash shouldNotBe b.first().currentHash

        // verifyChain de cada parking funciona en aislamiento.
        AuditRepository.verifyChain(parkingA) shouldHaveSize 0
        AuditRepository.verifyChain(parkingB) shouldHaveSize 0
    }

    @Test
    fun verifyChainDetectaCorrupcion() {
        AuditRepository.append(parkingA, "a", "x", "1", actor, """{"v":1}""")
        AuditRepository.append(parkingA, "b", "x", "2", actor, """{"v":2}""")
        val h3 = AuditRepository.append(parkingA, "c", "x", "3", actor, """{"v":3}""")

        // Cadena íntegra antes de manipular.
        AuditRepository.verifyChain(parkingA) shouldHaveSize 0

        // Simulamos que un atacante cambió el payload de la entrada del medio
        // sin recalcular hashes — debería volverse detectable.
        val tampered = transaction {
            AuditLogT.update({ AuditLogT.entityId eq "2" }) {
                it[AuditLogT.payload] = """{"v":99}"""
            }
        }
        if (tampered <= 0) fail("La fila a manipular no existe")

        val corrupted = AuditRepository.verifyChain(parkingA)
        // Al cambiar el payload de la entrada #2, su current_hash deja de
        // coincidir con el SHA-256 esperado, y la entrada #3 también queda
        // implicada porque su prev_hash apunta al hash original (no al recalculado).
        corrupted.isNotEmpty() shouldBe true
        // El último hash registrado por append sigue intacto en su columna —
        // verificamos que verifyChain encuentre la corrupción aguas arriba.
        corrupted.forEach { id -> id shouldMatch Regex("^[0-9a-f-]{36}$") }
        // h3 no se reporta como corrupto porque su current_hash en DB fue
        // calculado con el prev_hash genuino — pero la cadena igual está rota
        // porque su prev_hash apuntaba al hash CORRECTO de #2, y #2 ya no lo tiene.
        h3 shouldHaveLength 64
    }
}

