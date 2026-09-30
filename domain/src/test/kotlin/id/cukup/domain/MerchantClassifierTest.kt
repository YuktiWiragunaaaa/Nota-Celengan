package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantClassifierTest {

    private val cats = listOf(
        Category(1, "Makan", "🍜", CategoryKind.EXPENSE, Tag.FOOD, 0),
        Category(2, "Tagihan", "🔌", CategoryKind.EXPENSE, Tag.BILLS, 1),
        Category(3, "Transport", "🛵", CategoryKind.EXPENSE, Tag.TRANSPORT, 2),
        Category(4, "Jajan", "☕", CategoryKind.EXPENSE, Tag.FUN, 3),
        Category(5, "Cicilan", "🧾", CategoryKind.EXPENSE, Tag.DEBT, 4),
        Category(6, "Lainnya", "✨", CategoryKind.EXPENSE, Tag.OTHER, 5),
        Category(7, "Gaji", "💼", CategoryKind.INCOME, Tag.SALARY, 6),
    )

    @Test
    fun `keyword picks the category`() {
        assertEquals(4L, MerchantClassifier.suggest("Kopi Kenangan", cats, emptyMap())?.id)
        assertEquals(2L, MerchantClassifier.suggest("PLN Mobile", cats, emptyMap())?.id)
        assertEquals(1L, MerchantClassifier.suggest("Indomaret Cabang 21", cats, emptyMap())?.id)
    }

    @Test
    fun `learned rule overrides keywords`() {
        val learned = mapOf(MerchantClassifier.key("Kopi Kenangan") to 1L)
        assertEquals(1L, MerchantClassifier.suggest("KOPI KENANGAN", cats, learned)?.id)
    }

    @Test
    fun `unknown merchant goes to other`() {
        assertEquals(6L, MerchantClassifier.suggest("Toko Pak Budi", cats, emptyMap())?.id)
    }

    @Test
    fun `debt payment and income kinds`() {
        assertEquals(5L, MerchantClassifier.suggest("Bayar tagihan", cats, emptyMap(), isDebtPayment = true)?.id)
        assertEquals(7L, MerchantClassifier.suggest("PT Maju", cats, emptyMap(), CategoryKind.INCOME)?.id)
    }
}
