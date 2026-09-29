package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingTest {

    private val spend = Pocket(1, "Makan", "x", 50, PocketKind.SPEND, sortOrder = 0)
    private val save = Pocket(2, "Tabungan", "x", 50, PocketKind.SAVE, sortOrder = 1)
    private val pockets = listOf(spend, save)

    private fun income(id: Long, amount: Long, at: Long) =
        Transaction(id = id, type = TxType.INCOME, amount = amount, occurredAt = at) to
            Allocator.split(amount, pockets).map { (p, v) -> Allocation(id, p.id, v, p.percent) }

    @Test
    fun `last income rule uses previous period`() {
        val (last, a1) = income(1, 2_000_000, 100)
        val (now, a2) = income(2, 500_000, 1_000)
        val s = Balances.compute(
            pockets, listOf(last, now), a1 + a2, cycleStartMillis = 500, daysLeft = 5,
            rule = BudgetRule(BudgetRule.Mode.LAST_INCOME, 50), previousStartMillis = 0,
        )
        // 50% dari 2 jt minggu lalu, bukan dari 500 rb minggu ini.
        assertEquals(1_000_000L, s.budget)
        assertEquals(2_000_000L, s.basisIncome)
    }

    @Test
    fun `last income rule falls back to this period when last is empty`() {
        val (now, a) = income(1, 800_000, 1_000)
        val s = Balances.compute(pockets, listOf(now), a, 500, 5, BudgetRule(BudgetRule.Mode.LAST_INCOME, 50), 0)
        assertEquals(400_000L, s.budget)
    }

    @Test
    fun `reader names biggest pocket`() {
        val (inc, a) = income(1, 1_000_000, 1_000)
        val spent = Transaction(id = 2, type = TxType.EXPENSE, amount = 450_000, pocketId = 1, occurredAt = 1_100)
        val s = Balances.compute(pockets, listOf(inc, spent), a, 500, 5)
        val lines = ChartReader.read(s)
        assertTrue(lines[0].startsWith("Tabungan paling besar"))
        assertTrue(lines[1].contains("hampir habis"))
    }

    @Test
    fun `spending breakdown sorted`() {
        val txs = listOf(
            Transaction(id = 1, type = TxType.EXPENSE, amount = 10_000, pocketId = 2, occurredAt = 1),
            Transaction(id = 2, type = TxType.EXPENSE, amount = 30_000, pocketId = 1, occurredAt = 1),
        )
        val parts = ChartReader.spendingByPocket(txs, pockets)
        assertEquals(1L, parts.first().first.id)
        assertTrue(ChartReader.readSpending(parts).contains("75%"))
    }

    @Test
    fun `goal progress estimates periods`() {
        val g = GoalProgress.of(saved = 1_000_000, target = 3_000_000, perPeriod = 500_000)
        assertEquals(4, g.periodsLeft)
        assertEquals(2_000_000L, g.remaining)
        assertTrue(GoalProgress.of(3_000_000, 3_000_000, 0).reached)
        assertNull(GoalProgress.of(0, 1_000, 0).periodsLeft)
    }
}
