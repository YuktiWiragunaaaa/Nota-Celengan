package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PayCycleTest {

    @Test
    fun `after payday cycle runs to next month`() {
        val c = PayCycle.of(LocalDate.of(2026, 9, 30), 25)
        assertEquals(LocalDate.of(2026, 9, 25), c.start)
        assertEquals(LocalDate.of(2026, 10, 25), c.nextPayday)
        assertEquals(25, c.daysLeft(LocalDate.of(2026, 9, 30)))
    }

    @Test
    fun `before payday cycle started last month`() {
        val c = PayCycle.of(LocalDate.of(2026, 9, 10), 25)
        assertEquals(LocalDate.of(2026, 8, 25), c.start)
        assertEquals(LocalDate.of(2026, 9, 25), c.nextPayday)
    }

    @Test
    fun `payday on the day starts new cycle`() {
        val c = PayCycle.of(LocalDate.of(2026, 9, 25), 25)
        assertEquals(LocalDate.of(2026, 9, 25), c.start)
        assertEquals(30, c.daysLeft(LocalDate.of(2026, 9, 25)))
    }

    @Test
    fun `day 31 clamps in short months`() {
        val c = PayCycle.of(LocalDate.of(2026, 2, 10), 31)
        assertEquals(LocalDate.of(2026, 1, 31), c.start)
        assertEquals(LocalDate.of(2026, 2, 28), c.nextPayday)
    }

    @Test
    fun `last day before payday leaves one day`() {
        val c = PayCycle.of(LocalDate.of(2026, 10, 24), 25)
        assertEquals(1, c.daysLeft(LocalDate.of(2026, 10, 24)))
    }
}
