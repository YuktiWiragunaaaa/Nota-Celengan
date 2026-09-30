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
        assertEquals("Masih tersisa 600 rb dari uang masuk bulan ini.", lines[1])
    }

    @Test
    fun `warns when spending exceeds income`() {
        val lines = ChartReader.readSpending(listOf(CategoryAmount(food, 500_000)), Totals(400_000, 500_000), "bulan ini")
        assertTrue(lines[1].startsWith("Keluar lebih banyak"))
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
}
