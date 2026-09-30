package id.cukup.domain

/** Saldo satu dompet. */
data class AccountBalance(val account: Account, val balance: Long)

/** Total catatan dalam satu rentang waktu. */
data class Totals(val income: Long, val expense: Long) {
    val net: Long get() = income - expense
}

/** Pengeluaran (atau pemasukan) satu kategori dalam satu rentang waktu. */
data class CategoryAmount(val category: Category?, val amount: Long)

/**
 * Hitungan CATATAN: hanya dari transaksi yang sudah dikonfirmasi. Tidak ada rencana di sini.
 */
object Ledger {

    private fun List<Transaction>.confirmed() = filter { it.status == TxStatus.CONFIRMED }

    /** Saldo tiap dompet sekarang. */
    fun balances(accounts: List<Account>, transactions: List<Transaction>): List<AccountBalance> {
        val delta = mutableMapOf<Long, Long>()
        fun add(id: Long?, v: Long) {
            if (id != null) delta[id] = (delta[id] ?: 0) + v
        }
        for (t in transactions.confirmed()) {
            when (t.type) {
                TxType.EXPENSE -> add(t.accountId, -t.amount)
                TxType.INCOME -> add(t.accountId, t.amount)
                TxType.TRANSFER -> {
                    add(t.accountId, -t.amount)
                    add(t.toAccountId, t.amount)
                }
            }
        }
        return accounts.sortedBy { it.sortOrder }.map { AccountBalance(it, it.initialBalance + (delta[it.id] ?: 0)) }
    }

    /** Total uang yang kamu punya: jumlah saldo semua dompet selain paylater. */
    fun netWorth(balances: List<AccountBalance>): Long =
        balances.filter { it.account.kind != AccountKind.PAYLATER }.sumOf { it.balance }

    /** Hutang paylater yang belum lunas (angka positif). */
    fun debt(balances: List<AccountBalance>): Long =
        balances.filter { it.account.kind == AccountKind.PAYLATER }.sumOf { (-it.balance).coerceAtLeast(0) }

    /** Uang masuk & keluar dalam [from, to). Pindah antar dompet tidak dihitung. */
    fun totals(transactions: List<Transaction>, from: Long, to: Long): Totals {
        val inRange = transactions.confirmed().filter { it.occurredAt in from until to }
        return Totals(
            income = inRange.filter { it.type == TxType.INCOME }.sumOf { it.amount },
            expense = inRange.filter { it.type == TxType.EXPENSE }.sumOf { it.amount },
        )
    }

    /** Jumlah per kategori untuk [type] dalam [from, to), terbesar dulu. Tanpa kategori dikumpulkan jadi satu (category = null). */
    fun byCategory(
        transactions: List<Transaction>,
        categories: List<Category>,
        type: TxType,
        from: Long = Long.MIN_VALUE,
        to: Long = Long.MAX_VALUE,
    ): List<CategoryAmount> {
        val byId = categories.associateBy { it.id }
        return transactions.confirmed()
            .filter { it.type == type && it.occurredAt in from until to }
            .groupBy { it.categoryId?.let(byId::get) }
            .map { (cat, list) -> CategoryAmount(cat, list.sumOf { it.amount }) }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }
    }
}
