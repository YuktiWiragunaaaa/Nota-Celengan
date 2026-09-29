package id.cukup.domain

import kotlin.math.ceil

/** Kesimpulan singkat dari data kantong, untuk dibaca di bawah grafik. */
object ChartReader {

    /** Maksimal dua kalimat pendek. */
    fun read(summary: Summary): List<String> {
        val rows = summary.pockets.filter { it.balance > 0 }
        val total = rows.sumOf { it.balance }
        if (total <= 0) return listOf("Belum ada uang di kantong. Catat uang masuk dulu.")
        val out = mutableListOf<String>()

        val biggest = rows.maxBy { it.balance }
        val share = (biggest.balance * 100 / total).toInt()
        out += "${biggest.pocket.name} paling besar, $share% dari semua uangmu."

        val spend = summary.pockets.filter { it.pocket.kind == PocketKind.SPEND && it.balance + it.spentThisCycle > 0 }
        val fastest = spend.maxByOrNull { it.usedRatio }
        when {
            fastest != null && fastest.usedRatio >= 0.8f ->
                out += "${fastest.pocket.name} hampir habis, tinggal ${Rupiah.short(fastest.balance.coerceAtLeast(0))}."
            fastest != null && fastest.usedRatio > 0f ->
                out += "${fastest.pocket.name} paling cepat terpakai (${(fastest.usedRatio * 100).toInt()}%)."
            else -> {
                val saved = summary.pockets.filter { it.pocket.kind == PocketKind.SAVE }.sumOf { it.balance.coerceAtLeast(0) }
                if (saved > 0) out += "${(saved * 100 / total).toInt()}% uangmu ada di tabungan."
            }
        }
        return out.take(2)
    }

    /** Pembagian pengeluaran per kantong untuk sekumpulan transaksi. */
    fun spendingByPocket(transactions: List<Transaction>, pockets: List<Pocket>): List<Pair<Pocket, Long>> {
        val sums = transactions
            .filter { it.status == TxStatus.CONFIRMED && it.type == TxType.EXPENSE && !it.isPaylater }
            .groupBy { it.pocketId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        return pockets.mapNotNull { p -> sums[p.id]?.takeIf { it > 0 }?.let { p to it } }.sortedByDescending { it.second }
    }

    /** Kalimat untuk grafik pengeluaran per kantong. */
    fun readSpending(parts: List<Pair<Pocket, Long>>): String {
        val total = parts.sumOf { it.second }
        if (total <= 0) return "Belum ada pengeluaran di periode ini."
        val (top, amount) = parts.first()
        val share = (amount * 100 / total).toInt()
        return if (parts.size == 1) {
            "Semua pengeluaran periode ini dari ${top.name}."
        } else {
            "${top.name} paling banyak makan uang: $share% dari ${Rupiah.short(total)}."
        }
    }
}

/** Kemajuan menuju target tabungan. */
data class GoalProgress(
    val saved: Long,
    val target: Long,
    /** Perkiraan jumlah periode (minggu/bulan) lagi sampai tercapai; null bila tidak bisa diperkirakan. */
    val periodsLeft: Int?,
) {
    val ratio: Float get() = if (target <= 0) 0f else (saved.toFloat() / target).coerceIn(0f, 1f)
    val remaining: Long get() = (target - saved).coerceAtLeast(0)
    val reached: Boolean get() = target in 1..saved

    companion object {
        /**
         * [perPeriod] = perkiraan uang yang masuk ke kantong ini tiap periode
         * (persen kantong × uang masuk periode ini atau periode lalu).
         */
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
