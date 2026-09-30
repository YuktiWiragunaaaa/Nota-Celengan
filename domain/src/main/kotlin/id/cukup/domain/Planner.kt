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
    /** SPEND: yang sudah dibelanjakan dari kategori pos ini. SAVE: yang sudah disisihkan. */
    val used: Long,
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
    val spendLimit: Long get() = spend.sumOf { it.limit }
    val spendUsed: Long get() = spend.sumOf { it.used }
    val spendLeft: Long get() = spendLimit - spendUsed

    /** Sisa batas belanja dibagi sisa hari. */
    val perDay: Long get() = spendLeft.coerceAtLeast(0) / daysLeft.coerceAtLeast(1)
    val warning: Warning get() = Warning.of(spendUsed, spendLimit)
    val active: Boolean get() = rows.isNotEmpty() && basis > 0
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
    ): PlanStatus {
        val limits = split(basis, plan).toMap()
        val planOf = categories.associate { it.id to it.planId }
        val savings = accounts.filter { it.kind == AccountKind.SAVINGS }.map { it.id }.toSet()
        val used = mutableMapOf<Long, Long>()
        val firstSave = plan.sortedBy { it.sortOrder }.firstOrNull { it.kind == PlanKind.SAVE }?.id
        for (t in transactions) {
            if (t.status != TxStatus.CONFIRMED || t.occurredAt !in from until to) continue
            val posId = when {
                t.type == TxType.EXPENSE -> t.categoryId?.let { planOf[it] }
                t.type == TxType.TRANSFER && t.toAccountId in savings && t.accountId !in savings -> firstSave
                else -> null
            } ?: continue
            used[posId] = (used[posId] ?: 0) + t.amount
        }
        val rows = plan.sortedBy { it.sortOrder }.map { PosStatus(it, limits[it.id] ?: 0, used[it.id] ?: 0) }
        return PlanStatus(basis, rows, daysLeft)
    }

    /**
     * Membagi [amount] ke pos sesuai persentase dengan metode largest remainder,
     * sehingga jumlahnya selalu tepat sama dengan bagian yang dibagi (persen total bisa < 100).
     */
    fun split(amount: Long, plan: List<PlanPos>): List<Pair<Long, Long>> {
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
