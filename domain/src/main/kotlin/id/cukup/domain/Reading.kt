package id.cukup.domain

import kotlin.math.ceil

/** Kesimpulan singkat dari grafik, untuk dibaca di bawahnya. */
object ChartReader {

    /** Maksimal dua kalimat tentang pengeluaran per kategori. */
    fun readSpending(parts: List<CategoryAmount>, totals: Totals, periodName: String): List<String> {
        val total = parts.sumOf { it.amount }
        if (total <= 0) {
            return if (totals.income > 0) listOf("Belum ada pengeluaran $periodName. Uang masuk ${Rupiah.short(totals.income)}.")
            else listOf("Belum ada catatan $periodName.")
        }
        val out = mutableListOf<String>()
        val top = parts.first()
        val name = top.category?.name ?: "Tanpa kategori"
        out += if (parts.size == 1) "Semua pengeluaran $periodName untuk $name."
        else "$name paling besar: ${top.amount * 100 / total}% dari ${Rupiah.short(total)}."
        when {
            totals.income > 0 && totals.expense > totals.income ->
                out += "Keluar lebih banyak ${Rupiah.short(totals.expense - totals.income)} dari yang masuk."
            totals.income > 0 ->
                out += "Masih tersisa ${Rupiah.short(totals.income - totals.expense)} dari uang masuk $periodName."
            parts.size >= 2 -> {
                val second = parts[1]
                out += "Disusul ${second.category?.name ?: "tanpa kategori"} (${Rupiah.short(second.amount)})."
            }
        }
        return out.take(2)
    }

    /** Satu kalimat tentang rencana. */
    fun readPlan(status: PlanStatus, periodName: String): String {
        if (!status.active) return "Belum ada rencana. Atur di tab Rencana kalau mau dibantu jaga belanja."
        val over = status.rows.filter { it.pos.kind == PlanKind.SPEND && it.left < 0 }
        return when {
            over.isNotEmpty() -> "${over.first().pos.name} sudah lewat ${Rupiah.short(-over.first().left)} dari rencana."
            status.warning == Warning.NEAR -> "Batas belanja $periodName hampir habis, tinggal ${Rupiah.short(status.spendLeft)}."
            else -> "Aman. Boleh belanja sekitar ${Rupiah.short(status.perDay)} per hari sampai akhir periode."
        }
    }
}

/** Kemajuan menuju target tabungan. */
data class GoalProgress(
    val saved: Long,
    val target: Long,
    /** Perkiraan jumlah periode lagi sampai tercapai; null bila tidak bisa diperkirakan. */
    val periodsLeft: Int?,
) {
    val ratio: Float get() = if (target <= 0) 0f else (saved.toFloat() / target).coerceIn(0f, 1f)
    val remaining: Long get() = (target - saved).coerceAtLeast(0)
    val reached: Boolean get() = target in 1..saved

    companion object {
        /** [perPeriod] = perkiraan yang bisa disisihkan tiap periode (dari rencana tabungan). */
        fun of(saved: Long, target: Long, perPeriod: Long): GoalProgress {
            val remaining = (target - saved).coerceAtLeast(0)
            val periods = when {
                remaining == 0L -> 0
                perPeriod <= 0 -> null
                else -> ceil(remaining.toDouble() / perPeriod).toInt()
            }
            return GoalProgress(saved, target, periods)
        }
    }
}
