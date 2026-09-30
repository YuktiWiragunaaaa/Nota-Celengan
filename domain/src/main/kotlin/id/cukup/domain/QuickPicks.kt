package id.cukup.domain

/** Belanja yang sering diulang (kategori + nominal sama), untuk tombol sekali ketuk. */
data class QuickPick(val categoryId: Long, val amount: Long, val accountId: Long?, val merchant: String, val count: Int)

object QuickPicks {
    private const val DAY = 24L * 60 * 60 * 1000

    /** Kombinasi yang muncul minimal [minCount] kali dalam [days] hari terakhir, paling sering dulu. */
    fun top(txs: List<Transaction>, now: Long, limit: Int = 3, days: Int = 60, minCount: Int = 2): List<QuickPick> =
        txs.asSequence()
            .filter { it.type == TxType.EXPENSE && it.status == TxStatus.CONFIRMED && it.categoryId != null && it.occurredAt >= now - days * DAY }
            .groupBy { it.categoryId!! to it.amount }
            .filterValues { it.size >= minCount }
            .map { (key, list) ->
                val latest = list.maxBy { it.occurredAt }
                QuickPick(key.first, key.second, latest.accountId, latest.merchant, list.size)
            }
            .sortedWith(compareByDescending<QuickPick> { it.count }.thenByDescending { it.amount })
            .take(limit)
}
