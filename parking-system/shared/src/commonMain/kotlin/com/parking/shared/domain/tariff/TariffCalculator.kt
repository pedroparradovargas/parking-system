package com.parking.shared.domain.tariff

import com.parking.shared.domain.model.ChargeBreakdown
import com.parking.shared.domain.model.Tariff
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/*
 * Motor de tarifas — núcleo crítico del sistema.
 *
 * REGLAS COMERCIALES:
 *   1. El tiempo se redondea AL ALZA por hora completa.
 *      Ej.: 1h05min → cobra 2 horas.
 *   2. Si el conductor sale dentro de `graceMinutes`, no se cobra.
 *      Ej.: gracia = 15 → sesiones <15 min son gratis.
 *   3. La primera hora completa cobra `firstHourCents`.
 *   4. Cada hora adicional cobra `subsequentHourCents`.
 *   5. Si la sesión coincide con horario nocturno, se aplica
 *      `nightSurchargePercent` sobre las horas que caen en ese rango.
 *   6. IVA (Colombia 19 %) se aplica al final, sobre el subtotal.
 *   7. Si el cliente tiene mensualidad activa, no se cobra parqueo.
 *
 * IMPORTANTE: Este código debe producir resultados IDÉNTICOS en
 * backend, escritorio, móvil, iOS y web.  Cualquier divergencia es bug.
 */
object TariffCalculator {

    /**
     * Calcula el cobro completo para una sesión.
     *
     * @param tariff      tarifa vigente al tipo de vehículo / parking.
     * @param entryAt     instante de entrada.
     * @param exitAt      instante de salida (cálculo final).
     * @param hasMonthly  si el conductor tiene mensualidad vigente.
     * @param timeZone    huso horario del parqueadero (default America/Bogota).
     */
    fun calculate(
        tariff: Tariff,
        entryAt: Instant,
        exitAt: Instant,
        hasMonthly: Boolean = false,
        timeZone: TimeZone = TimeZone.of("America/Bogota"),
    ): ChargeBreakdown {
        require(exitAt >= entryAt) { "La salida no puede ser anterior a la entrada." }

        val totalMinutes = ((exitAt - entryAt).inWholeMinutes).coerceAtLeast(0)

        // Caso 1: tiempo dentro del periodo de gracia → cobro $0.
        if (totalMinutes <= tariff.graceMinutes) {
            return ChargeBreakdown(
                billedMinutes = totalMinutes,
                billedHours = 0,
                baseCents = 0,
                nightSurchargeCents = 0,
                subtotalCents = 0,
                ivaCents = 0,
                totalCents = 0,
                appliedMonthly = false,
                withinGrace = true,
            )
        }

        // Caso 2: mensualidad activa → cobro $0.
        if (hasMonthly) {
            return ChargeBreakdown(
                billedMinutes = totalMinutes,
                billedHours = ceilHours(totalMinutes),
                baseCents = 0,
                nightSurchargeCents = 0,
                subtotalCents = 0,
                ivaCents = 0,
                totalCents = 0,
                appliedMonthly = true,
                withinGrace = false,
            )
        }

        // Caso 3: cobro normal.
        val hours = ceilHours(totalMinutes)
        val baseCents = if (hours <= 1) {
            tariff.firstHourCents
        } else {
            tariff.firstHourCents + (hours - 1) * tariff.subsequentHourCents
        }

        val nightHours = computeNightHours(entryAt, exitAt, tariff.nightFrom, tariff.nightTo, timeZone)
        val nightSurchargeCents = if (tariff.nightSurchargePercent > 0 && nightHours > 0) {
            // El recargo solo aplica al PRECIO de las horas nocturnas.
            val nightBase = nightHours * tariff.subsequentHourCents
            (nightBase * tariff.nightSurchargePercent + 50) / 100
        } else 0L

        val subtotalCents = baseCents + nightSurchargeCents
        val ivaCents = (subtotalCents * tariff.ivaPercent + 50) / 100
        val totalCents = subtotalCents + ivaCents

        return ChargeBreakdown(
            billedMinutes = totalMinutes,
            billedHours = hours,
            baseCents = baseCents,
            nightSurchargeCents = nightSurchargeCents,
            subtotalCents = subtotalCents,
            ivaCents = ivaCents,
            totalCents = totalCents,
            appliedMonthly = false,
            withinGrace = false,
        )
    }

    /** Redondeo al alza a hora completa. */
    private fun ceilHours(minutes: Long): Long =
        if (minutes <= 0) 0L else (minutes + 59) / 60

    /**
     * Calcula cuántas HORAS completas de la sesión caen dentro del rango
     * nocturno definido por [nightFrom, nightTo].  Si `nightTo` < `nightFrom`,
     * el rango cruza medianoche (ej.: 22:00 → 06:00).
     *
     * Implementación: barrido por minuto y luego división entera por 60.
     * El barrido es O(n) por minutos: para una sesión máxima de 24 horas
     * son 1440 iteraciones — despreciable.
     */
    private fun computeNightHours(
        entryAt: Instant,
        exitAt: Instant,
        nightFrom: LocalTime,
        nightTo: LocalTime,
        timeZone: TimeZone,
    ): Long {
        if (nightFrom == nightTo) return 0L
        val totalMinutes = (exitAt - entryAt).inWholeMinutes
        if (totalMinutes <= 0) return 0L

        var nightMinutes = 0L
        var cursor = entryAt
        val stepMinutes = 1L
        val maxIterations = (totalMinutes).coerceAtMost(60 * 24 * 7)

        var i = 0L
        while (i < maxIterations) {
            val ldt: LocalDateTime = cursor.toLocalDateTime(timeZone)
            if (isNight(ldt.time, nightFrom, nightTo)) {
                nightMinutes++
            }
            cursor = cursor.plus(kotlin.time.Duration.parse("PT${stepMinutes}M"))
            i++
        }
        return nightMinutes / 60
    }

    private fun isNight(t: LocalTime, from: LocalTime, to: LocalTime): Boolean {
        return if (from <= to) {
            t in from..to
        } else {
            // Cruza medianoche.
            t >= from || t < to
        }
    }
}
