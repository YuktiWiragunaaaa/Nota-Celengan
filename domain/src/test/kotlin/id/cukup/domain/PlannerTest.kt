package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {

    private val needs = PlanPos(10, "Kebutuhan", "🏠", 50, PlanKind.SPEND, 0)
    private val wants = PlanPos(11, "Keinginan", "☕", 30, PlanKind.SPEND, 1)
    private val save = PlanPos(12, "Tabungan", "🌱", 20, PlanKind.SAVE, 2)
    private val plan = listOf(needs, wants, save)

    private val food = Category(1, "Makan", "🍜", CategoryKind.EXPENSE, Tag.FOOD, planId = 10)
    private val coffee = Category(2, "Kopi", "☕", CategoryKind.EXPENSE, Tag.FUN, planId = 11)
    private val gift = Category(3, "Hadiah", "🎁", CategoryKind.EXPENSE, Tag.OTHER, planId = null)
    private val wallet = Account(1, "Dompet", "💵", AccountKind.CASH)
    private val piggy = Account(2, "Celengan", "🐷", AccountKind.SAVINGS)

    @Test
    fun `split never loses a rupiah`() {
        val parts = Planner.split(1_000_001, plan).toMap()
        assertEquals(1_000_001L, parts.values.sum())
        assertTrue(parts[10]!! in 500_000L..500_001L)
    }

    @Test
    fun `percent under 100 leaves the rest unplanned`() {
        val parts = Planner.split(1_000_000, listOf(needs, wants)).toMap()
        assertEquals(500_000L, parts[10])
        assertEquals(300_000L, parts[11])
    }

    @Test
    fun `plan compares against records without changing them`() {
        val txs = listOf(
            Transaction(1, TxType.EXPENSE, 400_000, accountId = 1, categoryId = 1, occurredAt = 10),
            Transaction(2, TxType.EXPENSE, 350_000, accountId = 1, categoryId = 2, occurredAt = 11),
            Transaction(3, TxType.EXPENSE, 90_000, accountId = 1, categoryId = 3, occurredAt = 12),
            Transaction(4, TxType.TRANSFER, 150_000, accountId = 1, toAccountId = 2, occurredAt = 13),
            Transaction(5, TxType.EXPENSE, 1_000, accountId = 1, categoryId = 1, occurredAt = 999), // di luar periode
        )
        val s = Planner.status(plan, listOf(food, coffee, gift), listOf(wallet, piggy), txs, 1_000_000, 0, 100, daysLeft = 10)
        val rows = s.rows.associateBy { it.pos.id }
        assertEquals(500_000L, rows[10]!!.limit)
        assertEquals(400_000L, rows[10]!!.used)
        assertEquals(-50_000L, rows[11]!!.left) // keinginan lewat
        assertEquals(Warning.OVER, rows[11]!!.warning)
        assertEquals(150_000L, rows[12]!!.used) // pindah ke celengan = menabung
        assertEquals(800_000L, s.spendLimit)
        assertEquals(750_000L, s.spendUsed) // hadiah tidak masuk rencana
        assertEquals(5_000L, s.perDay)
        assertTrue(s.active)
    }

    @Test
    fun `basis modes`() {
        assertEquals(4_000_000L, Planner.basis(PlanBasis(PlanBasis.Mode.FIXED, 4_000_000), 1, 2))
        assertEquals(2L, Planner.basis(PlanBasis(PlanBasis.Mode.LAST_PERIOD), 1, 2))
        assertEquals(1L, Planner.basis(PlanBasis(PlanBasis.Mode.THIS_PERIOD), 1, 2))
        assertFalse(Planner.status(plan, emptyList(), emptyList(), emptyList(), 0, 0, 1, 1).active)
    }

    @Test
    fun `warning levels`() {
        assertEquals(Warning.CALM, Warning.of(70, 100))
        assertEquals(Warning.NEAR, Warning.of(80, 100))
        assertEquals(Warning.OVER, Warning.of(101, 100))
    }
}
