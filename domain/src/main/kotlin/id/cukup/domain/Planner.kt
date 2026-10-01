package id.cukup.domain

/** Tingkat peringatan rencana belanja. */
enum class Warning {
    CALM, NEAR, OVER;

    companion object {
        /** NEAR mulai 80%, OVER bila melebihi batas. */
        fun of(used: Long, limit: Long): Warning = when {
            limit <= 0 -> if (used > 0) OVER else CALM
            used > limit -> OVER
            used * 100 >= limit * 80 -> NEAR
            else -> CALM
        }
    }
}

/** Satu pos rencana dibanding catatan. */
data class PosStatus(
    val pos: PlanPos,
    /** Batas (SPEND) atau target sisihan (SAVE) periode ini. */
    val limit: Long,
    /** SPEND: yang sudah dibelanjakan dari kategori pos ini. SAVE: yang sudah disisihkan. Dihitung di jangka pos itu sendiri. */
    val used: Long,
    /** Sisa hari di jangka pos ini (termasuk hari ini). */
    val daysLeft: Int = 1,
    /** Batas dan pemakaian kalau dihitung satu periode gajian penuh; dipakai untuk ringkasan. */
    val limitCycle: Long = limit,
    val usedCycle: Long = used,
) {
    val left: Long get() = limit - used
    val ratio: Float get() = if (limit <= 0) (if (used > 0) 1f else 0f) else used.toFloat() / limit
    val warning: Warning get() = if (pos.kind == PlanKind.SPEND) Warning.of(used, limit) else Warning.CALM
}

/** Ringkasan rencana periode ini. */
data class PlanStatus(
    /** Uang masuk yang dibagi ke pos (dari [PlanBasis]). */
    val basis: Long,
    val rows: List<PosStatus>,
    val daysLeft: Int,
) {
    private val spend get() = rows.filter { it.pos.kind == PlanKind.SPEND }

    /** Total batas belanja semua pos SPEND. */
    val spendLimit: Long get() = spend.sumOf { it.limitCycle }
    val spendUsed: Long get() = spend.sumOf { it.usedCycle }
    val spendLeft: Long get() = spendLimit - spendUsed

    /** Sisa batas belanja dibagi sisa hari. Pos mingguan/harian dihitung dengan sisa harinya sendiri. */
    val perDay: Long
        get() {
            val (cycle, own) = spend.partition { it.pos.period == PosPeriod.CYCLE }
            return cycle.sumOf { it.left }.coerceAtLeast(0) / daysLeft.coerceAtLeast(1) +
                own.sumOf { it.left.coerceAtLeast(0) / it.daysLeft.coerceAtLeast(1) }
        }
    val warning: Warning get() = Warning.of(spendUsed, spendLimit)
    /** Aktif bila ada uang yang dibagi, atau ada pos bernominal tetap (tidak butuh uang yang dibagi). */
    val active: Boolean get() = rows.isNotEmpty() && (basis > 0 || rows.any { it.pos.fixed })
}

/**
 * Hitungan RENCANA. Membaca catatan, tidak pernah mengubahnya.
 */
object Planner {

    /** Uang masuk yang dipakai sebagai dasar rencana. */
    fun basis(rule: PlanBasis, incomeThisPeriod: Long, incomeLastPeriod: Long): Long = when (rule.mode) {
        PlanBasis.Mode.FIXED -> rule.fixedAmount
        PlanBasis.Mode.LAST_PERIOD -> incomeLastPeriod
        PlanBasis.Mode.THIS_PERIOD -> incomeThisPeriod
    }.coerceAtLeast(0)

    /**
     * [transactions] boleh berisi semua catatan; yang dihitung hanya [from, to).
     * SPEND: pengeluaran dari kategori yang diarahkan ke pos itu.
     * SAVE: uang yang dipindah ke dompet tabungan + pengeluaran kategori pos itu (mis. "Investasi").
     */
    fun status(
        plan: List<PlanPos>,
        categories: List<Category>,
        accounts: List<Account>,
        transactions: List<Transaction>,
        basis: Long,
        from: Long,
        to: Long,
        daysLeft: Int,
        /** Awal minggu ini (Senin) dan awal hari ini, untuk pos berjangka mingguan/harian. */
        weekFrom: Long = from,
        dayFrom: Long = from,
        weekDaysLeft: Int = daysLeft,
        cycleDays: Int = 30,
    ): PlanStatus {
        val limits = split(basis, plan).toMap()
        val planOf = categories.associate { it.id to it.planId }
        val savings = accounts.filter { it.kind == AccountKind.SAVINGS }.map { it.id }.toSet()
        val periodOf = plan.associate { it.id to it.period }
        val used = mutableMapOf<Long, Long>()
        val usedCycle = mutableMapOf<Long, Long>()
        val firstSave = plan.sortedBy { it.sortOrder }.firstOrNull { it.kind == PlanKind.SAVE }?.id
        for (t in transactions) {
            if (t.status != TxStatus.CONFIRMED || t.occurredAt !in minOf(from, weekFrom) until to) continue
            val posId = when {
                t.type == TxType.EXPENSE -> t.categoryId?.let { planOf[it] }
                t.type == TxType.TRANSFER && t.toAccountId in savings && t.accountId !in savings -> firstSave
                else -> null
            } ?: continue
            // Minggu berjalan bisa mulai sebelum hari gajian; yang sebelum gajian tidak masuk hitungan periode.
            if (t.occurredAt >= from) usedCycle[posId] = (usedCycle[posId] ?: 0) + t.amount
            val own = when (periodOf[posId]) {
                PosPeriod.WEEK -> weekFrom
                PosPeriod.DAY -> dayFrom
                else -> from
            }
            if (t.occurredAt >= own) used[posId] = (used[posId] ?: 0) + t.amount
        }
        val rows = plan.sortedBy { it.sortOrder }.map {
            val limit = limits[it.id] ?: 0
            when (it.period) {
                PosPeriod.CYCLE -> PosStatus(it, limit, used[it.id] ?: 0, daysLeft)
                PosPeriod.WEEK -> PosStatus(it, limit, used[it.id] ?: 0, weekDaysLeft, limit * cycleDays / 7, usedCycle[it.id] ?: 0)
                PosPeriod.DAY -> PosStatus(it, limit, used[it.id] ?: 0, 1, limit * cycleDays, usedCycle[it.id] ?: 0)
            }
        }
        return PlanStatus(basis, rows, daysLeft)
    }

    /**
     * Membagi [amount] ke pos sesuai persentase dengan metode largest remainder,
     * sehingga jumlahnya selalu tepat sama dengan bagian yang dibagi (persen total bisa < 100).
     * Pos bernominal tetap tidak ikut dibagi: batasnya ya nominal itu, per jangkanya sendiri.
     */
    fun split(amount: Long, plan: List<PlanPos>): List<Pair<Long, Long>> {
        if (plan.any { it.fixed }) {
            val shares = split(amount, plan.filter { !it.fixed }).toMap()
            return plan.map { it.id to if (it.fixed) it.amount else shares[it.id] ?: 0L }
        }
        if (plan.isEmpty() || amount <= 0) return plan.map { it.id to 0L }
        val totalPercent = plan.sumOf { it.percent.coerceAtLeast(0) }
        if (totalPercent <= 0) return plan.map { it.id to 0L }
        // Persen total di atas 100 dibagi rata ke 100; di bawah 100 sisanya tidak dibagi.
        val denominator = maxOf(100, totalPercent)
        val share = amount * minOf(100, totalPercent) / 100
        val parts = plan.map { p ->
            val num = amount * p.percent.coerceAtLeast(0)
            Triple(p, num / denominator, num % denominator)
        }
        var leftover = share - parts.sumOf { it.second }
        val extra = mutableMapOf<Long, Long>()
        val order = parts.filter { it.first.percent > 0 }
            .sortedWith(compareByDescending<Triple<PlanPos, Long, Long>> { it.third }.thenBy { it.first.sortOrder })
            .map { it.first.id }
        var i = 0
        while (leftover > 0 && order.isNotEmpty()) {
            val id = order[i % order.size]
            extra[id] = (extra[id] ?: 0) + 1
            leftover--
            i++
        }
        return parts.map { (p, base, _) -> p.id to base + (extra[p.id] ?: 0) }
    }
}
