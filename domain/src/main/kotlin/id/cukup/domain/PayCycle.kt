package id.cukup.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Seberapa sering pengguna menerima uang. */
enum class Frequency { WEEKLY, BIWEEKLY, MONTHLY, DAYS }

/**
 * Jadwal gajian.
 * - WEEKLY / BIWEEKLY: [weekday] 1 = Senin … 7 = Minggu.
 * - BIWEEKLY: [anchor] tanggal gajian yang pernah terjadi (epoch day) untuk menentukan minggu ganjil/genap.
 * - MONTHLY: [monthDay] 1..31.
 * - DAYS: periode [days] hari berulang; [anchor] = hari pertama salah satu periode (epoch day).
 */
data class Schedule(
    val frequency: Frequency = Frequency.MONTHLY,
    val monthDay: Int = 25,
    val weekday: Int = 5,
    val anchor: Long = 0,
    val days: Int = 7,
) {
    /** Kata untuk periode: "minggu ini", "2 minggu ini", "bulan ini". */
    val periodName: String
        get() = when (frequency) {
            Frequency.WEEKLY -> "minggu ini"
            Frequency.BIWEEKLY -> "2 minggu ini"
            Frequency.MONTHLY -> "bulan ini"
            Frequency.DAYS -> "periode ini"
        }

    companion object {
        /** Blok tanggal [start]..[endInclusive] jadi jadwal paling sederhana (7 hari, 14 hari, sebulan, atau N hari). */
        fun fromRange(start: LocalDate, endInclusive: LocalDate): Schedule {
            val len = (ChronoUnit.DAYS.between(start, endInclusive).toInt() + 1).coerceIn(2, 62)
            val end = start.plusDays(len - 1L)
            val dow = start.dayOfWeek.value
            return when {
                len == 7 -> Schedule(Frequency.WEEKLY, weekday = dow, anchor = start.toEpochDay())
                len == 14 -> Schedule(Frequency.BIWEEKLY, weekday = dow, anchor = start.toEpochDay())
                end == start.plusMonths(1).minusDays(1) -> Schedule(Frequency.MONTHLY, monthDay = start.dayOfMonth)
                else -> Schedule(Frequency.DAYS, anchor = start.toEpochDay(), days = len)
            }
        }
    }
}

/** Satu periode keuangan: dari hari gajian sampai sehari sebelum gajian berikutnya. */
data class PayCycle(val start: LocalDate, val nextPayday: LocalDate) {

    /** Sisa hari termasuk hari ini, minimal 1. */
    fun daysLeft(today: LocalDate): Int =
        ChronoUnit.DAYS.between(today, nextPayday).toInt().coerceAtLeast(1)

    val length: Int get() = ChronoUnit.DAYS.between(start, nextPayday).toInt()

    companion object {
        fun of(today: LocalDate, schedule: Schedule): PayCycle = when (schedule.frequency) {
            Frequency.MONTHLY -> of(today, schedule.monthDay)
            Frequency.WEEKLY -> weekly(today, schedule.weekday)
            Frequency.BIWEEKLY -> biweekly(today, schedule.weekday, schedule.anchor)
            Frequency.DAYS -> custom(today, schedule.days, schedule.anchor)
        }

        /** Bulanan. [payday] 1..31; di bulan yang lebih pendek memakai hari terakhir bulan itu. */
        fun of(today: LocalDate, payday: Int): PayCycle {
            val day = payday.coerceIn(1, 31)
            val first = today.withDayOfMonth(1)
            val thisMonth = paydayIn(first, day)
            return if (!today.isBefore(thisMonth)) {
                PayCycle(thisMonth, paydayIn(first.plusMonths(1), day))
            } else {
                PayCycle(paydayIn(first.minusMonths(1), day), thisMonth)
            }
        }

        private fun weekly(today: LocalDate, weekday: Int): PayCycle {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(weekday.coerceIn(1, 7))))
            return PayCycle(start, start.plusWeeks(1))
        }

        private fun biweekly(today: LocalDate, weekday: Int, anchor: Long): PayCycle {
            val week = weekly(today, weekday).start
            val anchorDay = LocalDate.ofEpochDay(anchor).with(TemporalAdjusters.previousOrSame(DayOfWeek.of(weekday.coerceIn(1, 7))))
            val weeks = ChronoUnit.WEEKS.between(anchorDay, week)
            val start = if (Math.floorMod(weeks, 2L) == 0L) week else week.minusWeeks(1)
            return PayCycle(start, start.plusWeeks(2))
        }

        private fun custom(today: LocalDate, days: Int, anchor: Long): PayCycle {
            val n = days.coerceAtLeast(1).toLong()
            val start = LocalDate.ofEpochDay(anchor + Math.floorDiv(today.toEpochDay() - anchor, n) * n)
            return PayCycle(start, start.plusDays(n))
        }

        private fun paydayIn(firstOfMonth: LocalDate, day: Int): LocalDate =
            firstOfMonth.withDayOfMonth(minOf(day, firstOfMonth.lengthOfMonth()))
    }
}
