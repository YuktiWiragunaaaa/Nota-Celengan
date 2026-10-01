package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReporterTest {

    private val food = Category(1, "Makan", "🍜", CategoryKind.EXPENSE, Tag.FOOD)
    private val coffee = Category(2, "Jajan", "☕", CategoryKind.EXPENSE, Tag.FUN)
    private val pay = Category(3, "Gaji", "💼", CategoryKind.INCOME, Tag.OTHER)

    @Test
    fun `notice names the top category and its share of income`() {
        val txs = listOf(
            Transaction(1, TxType.INCOME, 2_000_000, accountId = 1, categoryId = 3, occurredAt = 100),
            Transaction(2, TxType.EXPENSE, 500_000, accountId = 1, categoryId = 1, occurredAt = 110),
            Transaction(3, TxType.EXPENSE, 100_000, accountId = 1, categoryId = 2, occurredAt = 120),
            Transaction(4, TxType.EXPENSE, 300_000, accountId = 1, categoryId = 1, occurredAt = 50), // periode sebelumnya
        )
        val r = Reporter.of(txs, listOf(food, coffee, pay), from = 100, to = 200, prevFrom = 0, days = 30)
        assertEquals("Makan paling banyak: 500 rb, 25% dari uang masuk 2 jt.", r.notice)
        assertEquals(600_000L, r.totals.expense)
        assertTrue(r.findings.any { it.kind == "rise" && it.text.contains("Makan naik 200 rb") })
        assertTrue(r.advice.any { it.contains("Makan memakan lebih dari separuh") })
        assertTrue(r.advice.any { it.contains("sisa 1,4 jt") })
    }

    @Test
    fun `overspending is flagged and an empty period stays quiet`() {
        val txs = listOf(
            Transaction(1, TxType.INCOME, 100_000, accountId = 1, categoryId = 3, occurredAt = 100),
            Transaction(2, TxType.EXPENSE, 150_000, accountId = 1, categoryId = 1, occurredAt = 110),
        )
        val r = Reporter.of(txs, listOf(food, pay), 100, 200, 0, 7)
        assertTrue(r.findings.first().tone == Insight.Tone.WARN)
        assertTrue(r.advice.first().contains("tekor 50 rb"))
        assertTrue(Reporter.of(emptyList(), listOf(food), 100, 200, 0, 7).empty)
    }
}
