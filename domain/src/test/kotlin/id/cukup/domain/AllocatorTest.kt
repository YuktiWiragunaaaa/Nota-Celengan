package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllocatorTest {

    private fun p(id: Long, percent: Int, order: Int = id.toInt()) =
        Pocket(id, "P$id", "•", percent, PocketKind.SPEND, sortOrder = order)

    @Test
    fun `split follows percentages`() {
        val out = Allocator.split(4_000_000, listOf(p(1, 50), p(2, 30), p(3, 20)))
        assertEquals(listOf(2_000_000L, 1_200_000L, 800_000L), out.map { it.second })
    }

    @Test
    fun `split never loses a rupiah`() {
        val pockets = listOf(p(1, 33), p(2, 33), p(3, 34))
        for (amount in listOf(1L, 2L, 100L, 999_999L, 1_234_567L, 7L)) {
            assertEquals(amount, Allocator.split(amount, pockets).sumOf { it.second })
        }
    }

    @Test
    fun `leftover goes to largest remainder`() {
        // 10 * 33% = 3.3, 10 * 33% = 3.3, 10 * 34% = 3.4 → 3,3,4
        val out = Allocator.split(10, listOf(p(1, 33), p(2, 33), p(3, 34)))
        assertEquals(listOf(3L, 3L, 4L), out.map { it.second })
    }

    @Test
    fun `zero percent pocket receives nothing`() {
        val out = Allocator.split(101, listOf(p(1, 0), p(2, 50), p(3, 50)))
        assertEquals(0L, out[0].second)
        assertEquals(101L, out.sumOf { it.second })
    }

    @Test
    fun `validity requires exactly 100`() {
        assertTrue(Allocator.isValid(listOf(p(1, 60), p(2, 40))))
        assertFalse(Allocator.isValid(listOf(p(1, 60), p(2, 30))))
        assertFalse(Allocator.isValid(emptyList()))
    }
}
