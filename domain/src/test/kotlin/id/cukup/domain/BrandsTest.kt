package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrandsTest {
    private val cash = Account(1, "Tunai", "💵", AccountKind.CASH)
    private val bank = Account(2, "Rekening bank", "🏦", AccountKind.BANK)
    private val ovo = Account(4, "OVO", "🟣", AccountKind.EWALLET)
    private val krom = Account(6, "Krom", "🐷", AccountKind.SAVINGS)
    private val bca = Account(7, "BCA Tahapan", "🏦", AccountKind.BANK)
    private val mybca = "com.bca.mybca.omni.android"

    @Test
    fun `brand name wins`() {
        assertEquals(7L, Brands.accountFor(mybca, listOf(cash, bank, bca), emptyMap()))
        assertEquals(6L, Brands.accountFor("com.krom.android", listOf(cash, bank, krom), emptyMap()))
    }

    @Test
    fun `single generic bank account is used for a bank app`() {
        assertEquals(2L, Brands.accountFor(mybca, listOf(cash, bank, ovo), emptyMap()))
    }

    @Test
    fun `learned link overrides guess`() {
        assertEquals(4L, Brands.accountFor(mybca, listOf(cash, bank, ovo), mapOf("bca" to 4L)))
    }

    @Test
    fun `unknown app gives null`() {
        assertNull(Brands.accountFor("com.whatsapp", listOf(cash, bank), emptyMap()))
    }

    @Test
    fun `mentioned own wallet`() {
        val all = listOf(cash, bank, ovo, krom)
        assertEquals(4L, Brands.mentionedAccount("Top up OVO Rp100.000 berhasil", all, except = 2))
        assertEquals(6L, Brands.mentionedAccount("Transfer ke KROM sebesar IDR 50,000.00", all, except = 2))
        assertNull(Brands.mentionedAccount("Pembayaran di Indomaret", all, except = 2))
        assertNull(Brands.mentionedAccount("Top up OVO", all, except = 4))
    }

    @Test
    fun `krom notifications are read`() {
        val r = NotificationParser.parse("com.krom.android", "Uang masuk", "Kamu menerima Rp250.000 dari BUDI")!!
        assertEquals(TxType.INCOME, r.type)
        assertEquals("Krom", r.appLabel)
    }
}
