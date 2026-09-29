package id.cukup.domain

data class PocketBalance(
    val pocket: Pocket,
    /** Saldo sekarang (sepanjang waktu). */
    val balance: Long,
    /** Uang yang masuk ke pos ini selama siklus berjalan (alokasi + pindahan masuk). */
    val inThisCycle: Long,
    /** Pengeluaran + pindahan keluar kantong ini selama periode berjalan. */
    val outThisCycle: Long,
    /** Pengeluaran saja (tanpa pindahan) selama periode berjalan. */
    val spentThisCycle: Long = 0,
) {
    /** Porsi yang sudah terpakai dari uang yang tersedia di siklus ini (saldo awal siklus + masuk). */
    val usedRatio: Float
        get() {
            val available = balance + outThisCycle
            return when {
                available <= 0 -> if (outThisCycle > 0) 1f else 0f
                else -> (outThisCycle.toFloat() / available).coerceIn(0f, 1f)
            }
        }
}

data class Summary(
    val pockets: List<PocketBalance>,
    val total: Long,
    val safeToSpendToday: Long,
    val daysLeft: Int,
    val paylaterDebt: Long,
    val spentThisCycle: Long,
    val incomeThisCycle: Long,
    /** Jatah belanja periode ini (lihat [Balances.budgetOf]). */
    val budget: Long = 0,
    /** Yang sudah dibelanjakan dari kantong belanja periode ini. */
    val budgetUsed: Long = 0,
) {
    val budgetLeft: Long get() = budget - budgetUsed
    val budgetRatio: Float get() = if (budget <= 0) (if (budgetUsed > 0) 1.01f else 0f) else budgetUsed.toFloat() / budget
    val warning: Warning get() = Warning.of(budgetUsed, budget)
}

/** Tingkat peringatan jatah belanja. */
enum class Warning {
    CALM, NEAR, OVER;

    companion object {
        /** NEAR mulai 80%, OVER bila melebihi jatah. */
        fun of(used: Long, budget: Long): Warning = when {
            budget <= 0 -> if (used > 0) OVER else CALM
            used > budget -> OVER
            used * 100 >= budget * 80 -> NEAR
            else -> CALM
        }
    }
}

object Balances {

    /**
     * Jatah belanja = bagian kantong belanja dari uang yang masuk periode ini (mis. 50% gajian minggu ini).
     * Sisa periode lalu tidak menambah jatah. Bila periode ini belum ada uang masuk, jatah = saldo kantong belanja.
     */
    private fun budgetOf(spendRows: List<PocketBalance>): Long {
        val fresh = spendRows.sumOf { it.inThisCycle }
        return if (fresh > 0) fresh else spendRows.sumOf { (it.balance + it.spentThisCycle).coerceAtLeast(0) }
    }

    fun compute(
        pockets: List<Pocket>,
        transactions: List<Transaction>,
        allocations: List<Allocation>,
        cycleStartMillis: Long,
        daysLeft: Int,
    ): Summary {
        val confirmed = transactions.filter { it.status == TxStatus.CONFIRMED }
        val byId = confirmed.associateBy { it.id }
        val balance = mutableMapOf<Long, Long>()
        val inCycle = mutableMapOf<Long, Long>()
        val outCycle = mutableMapOf<Long, Long>()
        val spentCycle = mutableMapOf<Long, Long>()
        fun add(map: MutableMap<Long, Long>, id: Long?, v: Long) {
            if (id != null) map[id] = (map[id] ?: 0) + v
        }

        for (a in allocations) {
            val tx = byId[a.transactionId] ?: continue
            add(balance, a.pocketId, a.amount)
            if (tx.occurredAt >= cycleStartMillis) add(inCycle, a.pocketId, a.amount)
        }
        val debtPockets = pockets.filter { it.kind == PocketKind.DEBT }.map { it.id }.toSet()
        var paylater = 0L
        var repaid = 0L
        var spent = 0L
        var income = 0L
        for (t in confirmed) {
            val now = t.occurredAt >= cycleStartMillis
            when (t.type) {
                TxType.INCOME -> {
                    if (now) income += t.amount
                    if (t.toPocketId != null) {
                        add(balance, t.toPocketId, t.amount)
                        if (now) add(inCycle, t.toPocketId, t.amount)
                    }
                }
                TxType.EXPENSE -> {
                    if (t.isPaylater) {
                        paylater += t.amount
                    } else {
                        if (t.pocketId in debtPockets) repaid += t.amount
                        add(balance, t.pocketId, -t.amount)
                        if (now) {
                            add(outCycle, t.pocketId, t.amount)
                            spent += t.amount
                            add(spentCycle, t.pocketId, t.amount)
                        }
                    }
                }
                TxType.MOVE -> {
                    add(balance, t.pocketId, -t.amount)
                    add(balance, t.toPocketId, t.amount)
                    if (now) {
                        add(outCycle, t.pocketId, t.amount)
                        add(inCycle, t.toPocketId, t.amount)
                    }
                }
                TxType.ADJUST -> {
                    // pocketId = dikurangi, toPocketId = ditambah. Tidak memengaruhi jatah.
                    add(balance, t.pocketId, -t.amount)
                    add(balance, t.toPocketId, t.amount)
                }
            }
        }

        val rows = pockets.sortedBy { it.sortOrder }.map {
            PocketBalance(it, balance[it.id] ?: 0, inCycle[it.id] ?: 0, outCycle[it.id] ?: 0, spentCycle[it.id] ?: 0)
        }
        val spendable = rows.filter { it.pocket.kind == PocketKind.SPEND }.sumOf { it.balance.coerceAtLeast(0) }
        val spendRows = rows.filter { it.pocket.kind == PocketKind.SPEND }
        val used = spendRows.sumOf { it.spentThisCycle }
        return Summary(
            pockets = rows,
            total = rows.sumOf { it.balance },
            safeToSpendToday = spendable / daysLeft.coerceAtLeast(1),
            daysLeft = daysLeft,
            paylaterDebt = (paylater - repaid).coerceAtLeast(0),
            spentThisCycle = spent,
            incomeThisCycle = income,
            budget = budgetOf(spendRows),
            budgetUsed = used,
        )
    }
}
