package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingTest {

    private val food = Category(1, "Makan", "🍜", CategoryKind.EXPENSE, Tag.FOOD)
    private val fun_ = Category(2, "Jajan", "☕", CategoryKind.EXPENSE, Tag.FUN)

    @Test
    fun `names the biggest category and what is left`() {
        val lines = ChartReader.readSpending(
            listOf(CategoryAmount(food, 300_000), CategoryAmount(fun_, 100_000)),
            Totals(income = 1_000_000, expense = 400_000), "bulan ini",
        )
        assertEquals("Makan paling besar: 75% dari 400 rb.", lines[0])
        assertEquals("Sisa uang masuk: 600 rb.", lines[1])
    }

    @Test
    fun `warns when spending exceeds income`() {
        val lines = ChartReader.readSpending(listOf(CategoryAmount(food, 500_000)), Totals(400_000, 500_000), "bulan ini")
        assertTrue(lines[1].startsWith("Keluar melebihi masuk"))
    }

    @Test
    fun `empty period`() {
        assertEquals(listOf("Belum ada catatan minggu ini."), ChartReader.readSpending(emptyList(), Totals(0, 0), "minggu ini"))
    }

    @Test
    fun `goal progress estimate`() {
        val g = GoalProgress.of(saved = 400_000, target = 1_000_000, perPeriod = 250_000)
        assertEquals(3, g.periodsLeft)
        assertEquals(600_000L, g.remaining)
        assertNull(GoalProgress.of(0, 100, 0).periodsLeft)
        assertTrue(GoalProgress.of(100, 100, 0).reached)
    }

    @Test
    fun `income reading names the biggest source`() {
        val gaji = Category(13, "Gaji", "💼", CategoryKind.INCOME)
        val kiriman = Category(15, "Kiriman", "💌", CategoryKind.INCOME)
        val lines = ChartReader.readIncome(listOf(CategoryAmount(gaji, 4_000_000), CategoryAmount(kiriman, 1_000_000)), "bulan ini")
        org.junit.Assert.assertEquals("Gaji paling besar: 80% dari 5 jt.", lines[0])
        org.junit.Assert.assertEquals("Belum ada uang masuk bulan ini.", ChartReader.readIncome(emptyList(), "bulan ini")[0])
    }
}
