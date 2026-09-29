package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contoh teks di bawah adalah tiruan pola umum notifikasi, bukan salinan resmi. */
class NotificationParserTest {

    @Test
    fun `gopay payment at merchant`() {
        val r = NotificationParser.parse("com.gojek.app", "Pembayaran berhasil", "Kamu bayar Rp25.000 di Kopi Kenangan pakai GoPay.")!!
        assertEquals(TxType.EXPENSE, r.type)
        assertEquals(25_000L, r.amount)
        assertEquals("Kopi Kenangan", r.merchant)
        assertEquals("GoPay", r.appLabel)
    }

    @Test
    fun `bank incoming transfer`() {
        val r = NotificationParser.parse("id.co.bri.brimo", "Dana Masuk", "Kamu menerima Rp 4.000.000,00 dari PT MAJU JAYA.")!!
        assertEquals(TxType.INCOME, r.type)
        assertEquals(4_000_000L, r.amount)
        assertEquals("PT MAJU JAYA", r.merchant)
    }

    @Test
    fun `international number format`() {
        val r = NotificationParser.parse("com.jago.digitalBanking", "Transaksi", "Pembayaran QRIS IDR 125,500.00 di Warung Bu Sri berhasil")!!
        assertEquals(125_500L, r.amount)
        assertEquals("Warung Bu Sri", r.merchant)
    }

    @Test
    fun `transfer out to person`() {
        val r = NotificationParser.parse("id.dana", "Transfer berhasil", "Kirim uang Rp150.000 ke Andi Pratama berhasil.")!!
        assertEquals(TxType.EXPENSE, r.type)
        assertEquals("Andi Pratama", r.merchant)
    }

    @Test
    fun `paylater from shopee`() {
        val r = NotificationParser.parse("com.shopee.id", "SPayLater", "Pembayaran Rp349.000 dengan SPayLater berhasil.")!!
        assertTrue(r.isPaylater)
        assertEquals(349_000L, r.amount)
    }

    @Test
    fun `kredivo is always paylater`() {
        val r = NotificationParser.parse("com.finaccel.android", "Transaksi berhasil", "Pembelian Rp1.200.000 di Tokopedia berhasil.")!!
        assertTrue(r.isPaylater)
    }

    @Test
    fun `otp and promo are ignored`() {
        assertNull(NotificationParser.parse("ovo.id", "OVO", "Kode OTP kamu 123456. Jangan berikan ke siapa pun."))
        assertNull(NotificationParser.parse("id.dana", "Promo!", "Dapatkan cashback hingga Rp50.000 hari ini, yuk bayar pakai DANA!"))
    }

    @Test
    fun `failed transaction ignored`() {
        assertNull(NotificationParser.parse("id.dana", "Transaksi gagal", "Pembayaran Rp20.000 gagal."))
    }

    @Test
    fun `unknown app ignored`() {
        assertNull(NotificationParser.parse("com.whatsapp", "Budi", "Bayar Rp20.000 ya"))
    }

    @Test
    fun `no amount ignored`() {
        assertNull(NotificationParser.parse("ovo.id", "OVO", "Pembayaran berhasil"))
    }

    @Test
    fun `rupiah parse variants`() {
        assertEquals(25_000L, Rupiah.parse("25.000"))
        assertEquals(25_000L, Rupiah.parse("25.000,00"))
        assertEquals(25_000L, Rupiah.parse("25,000.00"))
        assertEquals(1_250_000L, Rupiah.parse("1.250.000,50"))
        assertEquals(5_000L, Rupiah.parse("5000"))
        assertEquals("Rp1.250.000", Rupiah.format(1_250_000))
        assertEquals("1,2 jt", Rupiah.short(1_250_000))
        assertEquals("45 rb", Rupiah.short(45_000))
    }
}
