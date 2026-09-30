package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPicksTest {
    private val now = 100L * 24 * 60 * 60 * 1000
    private fun tx(cat: Long, amount: Long, daysAgo: Int, type: TxType = TxType.EXPENSE) =
        Transaction(type = type, amount = amount, categoryId = cat, accountId = 1, merchant = "m$cat", occurredAt = now - daysAgo * 24L * 60 * 60 * 1000)

    @Test
    fun `repeated combos ranked by count`() {
        val txs = listOf(tx(1, 18_000, 1), tx(1, 18_000, 2), tx(1, 18_000, 3), tx(2, 15_000, 1), tx(2, 15_000, 5), tx(3, 99_000, 1))
        val picks = QuickPicks.top(txs, now)
        assertEquals(listOf(1L to 18_000L, 2L to 15_000L), picks.map { it.categoryId to it.amount })
        assertEquals(3, picks[0].count)
    }

    @Test
    fun `old and income ignored`() {
        val txs = listOf(tx(1, 10_000, 90), tx(1, 10_000, 91), tx(4, 5_000_000, 1, TxType.INCOME), tx(4, 5_000_000, 2, TxType.INCOME))
        assertTrue(QuickPicks.top(txs, now).isEmpty())
    }
}
