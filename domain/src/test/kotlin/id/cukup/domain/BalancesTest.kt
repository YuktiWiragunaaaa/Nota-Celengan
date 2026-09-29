package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BalancesTest {

    private val needs = Pocket(1, "Kebutuhan", "🏠", 50, PocketKind.SPEND, PocketTag.NEEDS, 0)
    private val wants = Pocket(2, "Keinginan", "☕", 30, PocketKind.SPEND, PocketTag.WANTS, 1)
    private val save = Pocket(3, "Tabungan", "🌱", 20, PocketKind.SAVE, PocketTag.SAVINGS, 2)
    private val debt = Pocket(4, "Cicilan", "🧾", 0, PocketKind.DEBT, PocketTag.DEBT, 3)
    private val pockets = listOf(needs, wants, save, debt)

    private fun income(id: Long, amount: Long, at: Long = 100): Pair<Transaction, List<Allocation>> {
        val tx = Transaction(id = id, type = TxType.INCOME, amount = amount, occurredAt = at)
        val allocs = Allocator.split(amount, pockets).map { (p, v) -> Allocation(id, p.id, v, p.percent) }
        return tx to allocs
    }

    @Test
    fun `income split and expenses reduce pockets`() {
        val (inc, allocs) = income(1, 4_000_000)
        val txs = listOf(
            inc,
            Transaction(id = 2, type = TxType.EXPENSE, amount = 150_000, pocketId = 1, occurredAt = 200),
            Transaction(id = 3, type = TxType.EXPENSE, amount = 50_000, pocketId = 2, occurredAt = 200),
        )
        val s = Balances.compute(pockets, txs, allocs, cycleStartMillis = 0, daysLeft = 10)
        assertEquals(1_850_000L, s.pockets[0].balance)
        assertEquals(1_150_000L, s.pockets[1].balance)
        assertEquals(800_000L, s.pockets[2].balance)
        assertEquals(3_800_000L, s.total)
        // aman dipakai = (1.850.000 + 1.150.000) / 10 hari
        assertEquals(300_000L, s.safeToSpendToday)
        assertEquals(200_000L, s.spentThisCycle)
    }

    @Test
    fun `pending and dismissed transactions are ignored`() {
        val (inc, allocs) = income(1, 1_000_000)
        val txs = listOf(
            inc,
            Transaction(id = 2, type = TxType.EXPENSE, amount = 99_000, pocketId = 1, occurredAt = 200, status = TxStatus.PENDING),
            Transaction(id = 3, type = TxType.EXPENSE, amount = 99_000, pocketId = 1, occurredAt = 200, status = TxStatus.DISMISSED),
        )
        val s = Balances.compute(pockets, txs, allocs, 0, 1)
        assertEquals(500_000L, s.pockets[0].balance)
    }

    @Test
    fun `move shifts money between pockets without changing total`() {
        val (inc, allocs) = income(1, 1_000_000)
        val txs = listOf(inc, Transaction(id = 2, type = TxType.MOVE, amount = 100_000, pocketId = 3, toPocketId = 1, occurredAt = 300))
        val s = Balances.compute(pockets, txs, allocs, 0, 1)
        assertEquals(600_000L, s.pockets[0].balance)
        assertEquals(100_000L, s.pockets[2].balance)
        assertEquals(1_000_000L, s.total)
    }

    @Test
    fun `income straight to one pocket`() {
        val txs = listOf(Transaction(id = 1, type = TxType.INCOME, amount = 250_000, toPocketId = 3, occurredAt = 10))
        val s = Balances.compute(pockets, txs, emptyList(), 0, 1)
        assertEquals(250_000L, s.pockets[2].balance)
        assertEquals(0L, s.safeToSpendToday)
    }

    @Test
    fun `paylater is debt until repaid from debt pocket`() {
        val (inc, allocs) = income(1, 1_000_000)
        val txs = listOf(
            inc,
            Transaction(id = 2, type = TxType.EXPENSE, amount = 300_000, pocketId = 2, occurredAt = 50, isPaylater = true),
            Transaction(id = 3, type = TxType.MOVE, amount = 100_000, pocketId = 1, toPocketId = 4, occurredAt = 60),
            Transaction(id = 4, type = TxType.EXPENSE, amount = 100_000, pocketId = 4, occurredAt = 70),
        )
        val s = Balances.compute(pockets, txs, allocs, 0, 1)
        assertEquals(300_000L, s.pockets[1].balance) // paylater tidak mengurangi pos sekarang
        assertEquals(200_000L, s.paylaterDebt)
        assertEquals(0L, s.pockets[3].balance)
    }

    @Test
    fun `cycle only counts recent activity`() {
        val (old, a1) = income(1, 1_000_000, at = 10)
        val (new, a2) = income(2, 2_000_000, at = 1_000)
        val s = Balances.compute(pockets, listOf(old, new), a1 + a2, cycleStartMillis = 500, daysLeft = 1)
        assertEquals(2_000_000L, s.incomeThisCycle)
        assertEquals(1_000_000L, s.pockets[0].inThisCycle)
        assertEquals(1_500_000L, s.pockets[0].balance)
    }
}
