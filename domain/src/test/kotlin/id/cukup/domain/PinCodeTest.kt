package id.cukup.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinCodeTest {
    @Test
    fun `correct pin verifies, wrong one does not`() {
        val stored = PinCode.make("2580")
        assertTrue(PinCode.verify("2580", stored))
        assertFalse(PinCode.verify("2581", stored))
        assertFalse(stored.contains("2580"))
    }

    @Test
    fun `same pin gets different salt`() {
        assertNotEquals(PinCode.make("1111"), PinCode.make("1111"))
    }

    @Test
    fun `invalid input rejected`() {
        assertFalse(PinCode.isValid("12a4"))
        assertFalse(PinCode.isValid("123"))
        assertFalse(PinCode.verify("1234", ""))
    }
}
