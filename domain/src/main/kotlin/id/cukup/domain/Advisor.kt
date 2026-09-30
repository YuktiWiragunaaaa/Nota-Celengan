package id.cukup.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Satu catatan untuk pengguna. [tone] menentukan warna di layar. */
data class Insight(val text: String, val tone: Tone, val kind: String) {
    enum class Tone { GOOD, INFO, WARN }
}

/**
 * Penasehat sederhana dari data sendiri. Semua dihitung di HP, tanpa AI dan tanpa internet,
 * supaya angkanya bisa dipercaya. Maksimal [limit] catatan, yang paling penting lebih dulu.
 */
object Advisor {

    private val id = Locale.forLanguageTag("id-ID")

    fun insights(
        plan: PlanStatus,
        transactions: List<Transaction>,
        debt: Long,
        cycle: PayCycle,
        previousCycle: PayCycle,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        limit: Int = 3,
    ): List<Insight> {
        val out = mutableListOf<Pair<Int, Insight>>()
        fun date(t: Transaction) = Instant.ofEpochMilli(t.occurredAt).atZone(zone).toLocalDate()
        val expenses = transactions.filter { it.status == TxStatus.CONFIRMED && it.type == TxType.EXPENSE }
        val thisPeriod = expenses.filter { !date(it).isBefore(cycle.start) && date(it).isBefore(cycle.nextPayday) }
        val elapsed = (ChronoUnit.DAYS.between(cycle.start, today) + 1).toInt().coerceAtLeast(1)

        // 1. Laju belanja: kapan batas rencana habis kalau terus begini.
        if (plan.active && plan.spendUsed > 0 && plan.spendLeft > 0 && elapsed >= 2) {
            val perDay = plan.spendUsed / elapsed
            if (perDay > 0) {
                val daysUntilEmpty = plan.spendLeft / perDay
                if (daysUntilEmpty < plan.daysLeft) {
                    val day = today.plusDays(daysUntilEmpty)
                    val text = if (daysUntilEmpty == 0L) {
                        "Rata-rata belanjamu ${Rupiah.format(perDay)} sehari. Sisa batasnya cuma cukup untuk hari ini, padahal gajian masih ${plan.daysLeft} hari lagi."
                    } else {
                        "Kalau belanjanya terus segini, batas rencanamu habis hari ${dayName(day)}. Gajian masih ${plan.daysLeft} hari lagi."
                    }
                    out += 100 to Insight(
                        text,
                        Insight.Tone.WARN, "pace",
                    )
                }
            }
        }

        // 2. Dibanding periode lalu pada titik yang sama.
        val lastSamePoint = expenses.filter {
            val d = date(it)
            !d.isBefore(previousCycle.start) && d.isBefore(previousCycle.start.plusDays(elapsed.toLong())) && d.isBefore(cycle.start)
        }.sumOf { it.amount }
        val now = thisPeriod.sumOf { it.amount }
        if (lastSamePoint >= 10_000 && now > 0) {
            val diff = ((now - lastSamePoint) * 100 / lastSamePoint).toInt()
            if (diff <= -10) {
                out += 60 to Insight("Lebih hemat ${-diff}% dibanding periode lalu di hari yang sama. Mantap.", Insight.Tone.GOOD, "compare")
            } else if (diff >= 20) {
                out += 80 to Insight(
                    "Belanjamu ${diff}% lebih banyak dari periode lalu di hari yang sama (${Rupiah.short(now)} vs ${Rupiah.short(lastSamePoint)}).",
                    Insight.Tone.WARN, "compare",
                )
            }
        }

        // 3. Tempat yang paling sering.
        thisPeriod.filter { it.merchant.isNotBlank() }
            .groupBy { MerchantClassifier.key(it.merchant) }
            .maxByOrNull { (_, list) -> list.sumOf { it.amount } }
            ?.let { (_, list) ->
                if (list.size >= 3 && now > 0 && list.sumOf { it.amount } * 100 / now >= 20) {
                    out += 50 to Insight(
                        "Paling banyak keluar buat ${list.first().merchant}: ${Rupiah.format(list.sumOf { it.amount })} dalam ${list.size} kali.",
                        Insight.Tone.INFO, "merchant",
                    )
                }
            }

        // 4. Jajan kecil yang menumpuk.
        val small = thisPeriod.filter { it.amount < 30_000 }
        if (small.size >= 5 && now > 0 && small.sumOf { it.amount } * 100 / now >= 25) {
            out += 55 to Insight(
                "Belanja kecil di bawah 30 ribu sudah ${small.size} kali, totalnya ${Rupiah.format(small.sumOf { it.amount })}. Kecil-kecil, tapi lumayan.",
                Insight.Tone.INFO, "small",
            )
        }

        // 5. Belum ada uang masuk.
        val incomeNow = transactions.any {
            it.status == TxStatus.CONFIRMED && it.type == TxType.INCOME && !date(it).isBefore(cycle.start)
        }
        if (!incomeNow && elapsed >= 2) {
            out += 70 to Insight("Belum ada uang masuk periode ini. Kalau sudah gajian, jangan lupa dicatat ya.", Insight.Tone.INFO, "noincome")
        }

        // 6. Hutang paylater.
        if (debt > 0) {
            out += 65 to Insight("Masih ada hutang paylater ${Rupiah.format(debt)}.", Insight.Tone.WARN, "debt")
        }

        // 7. Tabungan sesuai rencana.
        plan.rows.filter { it.pos.kind == PlanKind.SAVE && it.used > 0 }.forEach { row ->
            out += 40 to Insight(
                if (row.used >= row.limit && row.limit > 0) "Target sisihan ${row.pos.name} periode ini sudah tercapai. Keren."
                else "Sudah menyisihkan ${Rupiah.format(row.used)} ke ${row.pos.name} periode ini.",
                Insight.Tone.GOOD, "saved",
            )
        }

        return out.sortedByDescending { it.first }.map { it.second }.distinctBy { it.kind }.take(limit)
    }

    private fun dayName(d: LocalDate): String =
        d.dayOfWeek.getDisplayName(TextStyle.FULL, id).replaceFirstChar { it.titlecase(id) }
}
