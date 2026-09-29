package id.cukup.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Siklus keuangan dari tanggal gajian ke tanggal gajian berikutnya. */
data class PayCycle(val start: LocalDate, val nextPayday: LocalDate) {

    /** Sisa hari termasuk hari ini, minimal 1. */
    fun daysLeft(today: LocalDate): Int =
        ChronoUnit.DAYS.between(today, nextPayday).toInt().coerceAtLeast(1)

    val length: Int get() = ChronoUnit.DAYS.between(start, nextPayday).toInt()

    companion object {
        /** [payday] 1..31. Bila bulan lebih pendek (mis. 31 di Februari), memakai hari terakhir bulan itu. */
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

        private fun paydayIn(firstOfMonth: LocalDate, day: Int): LocalDate =
            firstOfMonth.withDayOfMonth(minOf(day, firstOfMonth.lengthOfMonth()))
    }
}
