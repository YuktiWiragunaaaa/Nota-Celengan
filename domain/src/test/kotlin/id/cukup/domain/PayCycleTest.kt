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

    @Test
    fun `weekly cycle starts on chosen weekday`() {
        // 30 Sep 2026 = Rabu. Gajian tiap Jumat (5).
        val c = PayCycle.of(LocalDate.of(2026, 9, 30), Schedule(Frequency.WEEKLY, weekday = 5))
        assertEquals(LocalDate.of(2026, 9, 25), c.start)
        assertEquals(LocalDate.of(2026, 10, 2), c.nextPayday)
        assertEquals(2, c.daysLeft(LocalDate.of(2026, 9, 30)))
    }

    @Test
    fun `weekly on payday starts new week`() {
        val c = PayCycle.of(LocalDate.of(2026, 10, 2), Schedule(Frequency.WEEKLY, weekday = 5))
        assertEquals(LocalDate.of(2026, 10, 2), c.start)
        assertEquals(7, c.daysLeft(LocalDate.of(2026, 10, 2)))
    }

    @Test
    fun `biweekly follows anchor`() {
        val anchor = LocalDate.of(2026, 9, 18).toEpochDay() // Jumat
        val s = Schedule(Frequency.BIWEEKLY, weekday = 5, anchor = anchor)
        assertEquals(LocalDate.of(2026, 9, 18), PayCycle.of(LocalDate.of(2026, 9, 30), s).start)
        assertEquals(LocalDate.of(2026, 10, 2), PayCycle.of(LocalDate.of(2026, 10, 3), s).start)
        assertEquals(LocalDate.of(2026, 10, 16), PayCycle.of(LocalDate.of(2026, 10, 3), s).nextPayday)
    }
}
