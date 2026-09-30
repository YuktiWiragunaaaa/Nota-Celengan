package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AdvisorTest {

    private val zone = ZoneOffset.UTC
    private val food = Category(1, "Makan", "x", CategoryKind.EXPENSE, Tag.FOOD, planId = 10)
    private val plan = listOf(
        PlanPos(10, "Belanja", "x", 50, PlanKind.SPEND, 0),
        PlanPos(11, "Tabungan", "x", 50, PlanKind.SAVE, 1),
    )
    private val wallet = Account(1, "Dompet", "x", AccountKind.CASH)
    private val piggy = Account(2, "Celengan", "x", AccountKind.SAVINGS)

    // Gajian tiap Jumat. Hari ini Rabu 30 Sep 2026 → periode 25 Sep – 1 Okt.
    private val schedule = Schedule(Frequency.WEEKLY, weekday = 5)
    private val today = LocalDate.of(2026, 9, 30)
    private val cycle = PayCycle.of(today, schedule)
    private val previous = PayCycle.of(cycle.start.minusDays(1), schedule)

    private fun at(d: LocalDate) = d.atTime(12, 0).toInstant(zone).toEpochMilli()
    private var nextId = 100L
    private fun spent(amount: Long, d: LocalDate, merchant: String = "") =
        Transaction(id = nextId++, type = TxType.EXPENSE, amount = amount, accountId = 1, categoryId = 1, merchant = merchant, occurredAt = at(d))

    private fun run(txs: List<Transaction>, limit: Int = 3): List<Insight> {
        val income = Transaction(id = 1, type = TxType.INCOME, amount = 1_000_000, accountId = 1, occurredAt = at(cycle.start))
        val all = listOf(income) + txs
        val from = at(cycle.start) - 12 * 3600_000
        val status = Planner.status(plan, listOf(food), listOf(wallet, piggy), all, 1_000_000, from, Long.MAX_VALUE, cycle.daysLeft(today))
        return Advisor.insights(status, all, 0, cycle, previous, today, zone, limit)
    }

    @Test
    fun `warns when pace runs out before payday`() {
        // 450 rb dalam 6 hari (75 rb/hari); sisa 50 rb → habis hari ini, gajian masih 2 hari.
        val pace = run(listOf(spent(450_000, cycle.start.plusDays(1)))).first { it.kind == "pace" }
        assertEquals(Insight.Tone.WARN, pace.tone)
        assertTrue(pace.text.contains("gajian masih"))
    }

    @Test
    fun `praises saving compared to last period`() {
        val last = (0..4).map { spent(40_000, previous.start.plusDays(it.toLong())) }
        val now = listOf(spent(50_000, cycle.start.plusDays(1)))
        assertTrue(run(last + now).any { it.kind == "compare" && it.tone == Insight.Tone.GOOD })
    }

    @Test
    fun `spots frequent merchant and small spends`() {
        val kopi = (0..5).map { spent(25_000, cycle.start.plusDays((it % 5).toLong()), "Kopi Kenangan") }
        val list = run(kopi, limit = 10)
        assertTrue(list.any { it.kind == "merchant" && it.text.contains("Kopi Kenangan") })
        assertTrue(list.any { it.kind == "small" })
    }

    @Test
    fun `mentions money moved to savings`() {
        val move = Transaction(id = 50, type = TxType.TRANSFER, amount = 200_000, accountId = 1, toAccountId = 2, occurredAt = at(cycle.start.plusDays(1)))
        assertTrue(run(listOf(move), limit = 10).any { it.kind == "saved" && it.text.contains("Rp200.000") })
    }

    @Test
    fun `at most three insights`() {
        val many = (0..9).map { spent(29_000, cycle.start.plusDays((it % 5).toLong()), "Warung") }
        assertTrue(run(many).size <= 3)
    }
}
