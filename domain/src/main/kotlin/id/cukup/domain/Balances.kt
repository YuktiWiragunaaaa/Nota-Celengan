package id.cukup.domain

data class PocketBalance(
    val pocket: Pocket,
    /** Saldo sekarang (sepanjang waktu). */
    val balance: Long,
    /** Uang yang masuk ke pos ini selama siklus berjalan (alokasi + pindahan masuk). */
    val inThisCycle: Long,
    /** Pengeluaran + pindahan keluar pos ini selama siklus berjalan. */
    val outThisCycle: Long,
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
)

object Balances {

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
            }
        }

        val rows = pockets.sortedBy { it.sortOrder }.map {
            PocketBalance(it, balance[it.id] ?: 0, inCycle[it.id] ?: 0, outCycle[it.id] ?: 0)
        }
        val spendable = rows.filter { it.pocket.kind == PocketKind.SPEND }.sumOf { it.balance.coerceAtLeast(0) }
        return Summary(
            pockets = rows,
            total = rows.sumOf { it.balance },
            safeToSpendToday = spendable / daysLeft.coerceAtLeast(1),
            daysLeft = daysLeft,
            paylaterDebt = (paylater - repaid).coerceAtLeast(0),
            spentThisCycle = spent,
            incomeThisCycle = income,
        )
    }
}
