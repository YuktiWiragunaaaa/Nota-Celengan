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
    fun `fixed amount pos keeps its own weekly window`() {
        val weekly = PlanPos(20, "Makan", "🍜", 0, PlanKind.SPEND, 0, amount = 300_000, period = PosPeriod.WEEK)
        val rest = PlanPos(21, "Lainnya", "☕", 50, PlanKind.SPEND, 1)
        val eat = food.copy(planId = 20)
        val other = coffee.copy(planId = 21)
        val txs = listOf(
            Transaction(1, TxType.EXPENSE, 200_000, accountId = 1, categoryId = 1, occurredAt = 10), // minggu lalu, periode ini
            Transaction(2, TxType.EXPENSE, 120_000, accountId = 1, categoryId = 1, occurredAt = 60), // minggu ini
            Transaction(3, TxType.EXPENSE, 100_000, accountId = 1, categoryId = 2, occurredAt = 61),
        )
        val s = Planner.status(
            listOf(weekly, rest), listOf(eat, other), listOf(wallet), txs, 1_000_000, from = 0, to = 100, daysLeft = 10,
            weekFrom = 50, dayFrom = 90, weekDaysLeft = 3, cycleDays = 28,
        )
        val rows = s.rows.associateBy { it.pos.id }
        assertEquals(300_000L, rows[20]!!.limit) // nominal tetap, bukan persen
        assertEquals(120_000L, rows[20]!!.used) // hanya minggu ini
        assertEquals(3, rows[20]!!.daysLeft)
        assertEquals(500_000L, rows[21]!!.limit) // persen dari uang yang dibagi, tidak terpengaruh pos tetap
        assertEquals(1_200_000L + 500_000L, s.spendLimit) // 300 rb x 4 minggu + 500 rb
        assertEquals(420_000L, s.spendUsed)
        assertEquals(400_000L / 10 + 180_000L / 3, s.perDay)
        // Tanpa uang yang dibagi pun rencana bernominal tetap tetap jalan.
        assertTrue(Planner.status(listOf(weekly), listOf(eat), listOf(wallet), txs, 0, 0, 100, 10, weekFrom = 50).active)
    }

    @Test
    fun `spending from a wallet grouped into a pos counts there when the category has no pos`() {
        val gopay = Account(5, "GoPay", "👛", AccountKind.EWALLET, planId = 11)
        val txs = listOf(
            Transaction(1, TxType.EXPENSE, 50_000, accountId = 5, categoryId = 3, occurredAt = 10), // hadiah: tanpa pos, ikut pos dompet
            Transaction(2, TxType.EXPENSE, 70_000, accountId = 5, categoryId = 1, occurredAt = 11), // makan: tetap ke pos kategorinya
            Transaction(3, TxType.EXPENSE, 20_000, accountId = 1, categoryId = 3, occurredAt = 12), // dompet tanpa pos: tidak dihitung
        )
        val rows = Planner.status(plan, listOf(food, coffee, gift), listOf(wallet, gopay), txs, 1_000_000, 0, 100, 10).rows.associateBy { it.pos.id }
        assertEquals(50_000L, rows[11]!!.used)
        assertEquals(70_000L, rows[10]!!.used)
    }

    @Test
    fun `moving money into a wallet grouped to a pos fills that pos this period`() {
        val krom = Account(6, "Krom", "🏦", AccountKind.BANK, planId = 12) // pos Tabungan (SAVE)
        val dana = Account(7, "DANA", "👛", AccountKind.EWALLET, planId = 11) // pos Keinginan (SPEND)
        val txs = listOf(
            Transaction(1, TxType.TRANSFER, 500_000, accountId = 1, toAccountId = 6, occurredAt = 10),
            Transaction(2, TxType.TRANSFER, 100_000, accountId = 6, toAccountId = 1, occurredAt = 11), // ditarik lagi
            Transaction(3, TxType.INCOME, 50_000, accountId = 6, occurredAt = 12), // diterima langsung di Krom
            Transaction(4, TxType.TRANSFER, 200_000, accountId = 1, toAccountId = 7, occurredAt = 13),
            Transaction(5, TxType.EXPENSE, 30_000, accountId = 6, categoryId = 3, occurredAt = 14), // belanja dari dompet tabungan: bukan menabung
        )
        val rows = Planner.status(plan, listOf(food, coffee, gift), listOf(wallet, krom, dana), txs, 1_000_000, 0, 100, 10).rows.associateBy { it.pos.id }
        assertEquals(450_000L, rows[12]!!.used) // 500 - 100 + 50
        assertEquals(450_000L, rows[12]!!.moved)
        assertEquals(0L, rows[11]!!.used) // memindah ke dompet pos belanja bukan belanja
        assertEquals(200_000L, rows[11]!!.moved)
    }

    @Test
    fun `a balance set by hand this period fills the wallet's pos`() {
        val krom = Account(6, "Krom", "🏦", AccountKind.BANK, planId = 12)
        val adjusts = listOf(BalanceAdjust(6, 20, 500_000), BalanceAdjust(6, 200, 900_000), BalanceAdjust(1, 20, 70_000))
        val rows = Planner.status(plan, listOf(food, coffee, gift), listOf(wallet, krom), emptyList(), 1_000_000, 0, 100, 10, adjusts = adjusts)
            .rows.associateBy { it.pos.id }
        assertEquals(500_000L, rows[12]!!.used) // yang di luar periode dan dompet tanpa pos tidak dihitung
        assertEquals(500_000L, rows[12]!!.moved)
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
