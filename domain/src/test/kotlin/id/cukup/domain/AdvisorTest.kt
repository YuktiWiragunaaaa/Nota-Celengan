package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AdvisorTest {

    private val zone = ZoneOffset.UTC
    private val spend = Pocket(1, "Belanja", "x", 50, PocketKind.SPEND, sortOrder = 0)
    private val save = Pocket(2, "Tabungan", "x", 50, PocketKind.SAVE, sortOrder = 1)
    private val pockets = listOf(spend, save)

    // Gajian tiap Jumat. Hari ini Rabu 30 Sep 2026 → periode 25 Sep – 1 Okt.
    private val schedule = Schedule(Frequency.WEEKLY, weekday = 5)
    private val today = LocalDate.of(2026, 9, 30)
    private val cycle = PayCycle.of(today, schedule)
    private val previous = PayCycle.of(cycle.start.minusDays(1), schedule)

    private fun at(d: LocalDate) = d.atTime(12, 0).toInstant(zone).toEpochMilli()
    private var nextId = 100L
    private fun spent(amount: Long, d: LocalDate, merchant: String = "") =
        Transaction(id = nextId++, type = TxType.EXPENSE, amount = amount, pocketId = 1, merchant = merchant, occurredAt = at(d))

    private fun run(txs: List<Transaction>): List<Insight> {
        val income = Transaction(id = 1, type = TxType.INCOME, amount = 1_000_000, occurredAt = at(cycle.start))
        val allocs = Allocator.split(1_000_000, pockets).map { (p, v) -> Allocation(1, p.id, v, p.percent) }
        val all = listOf(income) + txs
        val start = at(cycle.start) - 12 * 3600_000
        val summary = Balances.compute(pockets, all, allocs, start, cycle.daysLeft(today))
        return Advisor.insights(summary, all, pockets, cycle, previous, today, zone)
    }

    @Test
    fun `warns when pace runs out before payday`() {
        // 450 rb dalam 6 hari (75 rb/hari); sisa 50 rb → habis hari ini, gajian masih 2 hari.
        val list = run(listOf(spent(450_000, cycle.start.plusDays(1))))
        val pace = list.first { it.kind == "pace" }
        assertEquals(Insight.Tone.WARN, pace.tone)
        assertTrue(pace.text.contains("gajian masih"))
    }

    @Test
    fun `praises saving compared to last period`() {
        val last = (0..4).map { spent(40_000, previous.start.plusDays(it.toLong())) }
        val now = listOf(spent(50_000, cycle.start.plusDays(1)))
        val list = run(last + now)
        assertTrue(list.any { it.kind == "compare" && it.tone == Insight.Tone.GOOD })
    }

    @Test
    fun `spots frequent merchant and small spends`() {
        val kopi = (0..5).map { spent(25_000, cycle.start.plusDays((it % 5).toLong()), "Kopi Kenangan") }
        val list = Advisor.insights(
            Balances.compute(pockets, kopi, emptyList(), 0, 2), kopi, pockets, cycle, previous, today, zone, limit = 10,
        )
        assertTrue(list.any { it.kind == "merchant" && it.text.contains("Kopi Kenangan") })
        assertTrue(list.any { it.kind == "small" })
    }

    @Test
    fun `mentions savings growth`() {
        val list = run(emptyList())
        assertTrue(list.any { it.kind == "saved" && it.text.contains("Rp500.000") })
    }

    @Test
    fun `at most three insights`() {
        val many = (0..9).map { spent(29_000, cycle.start.plusDays((it % 5).toLong()), "Warung") }
        assertTrue(run(many).size <= 3)
    }
}
