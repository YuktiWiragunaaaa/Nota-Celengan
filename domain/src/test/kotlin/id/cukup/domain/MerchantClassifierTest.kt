package id.cukup.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantClassifierTest {

    private val kos = listOf(
        Pocket(1, "Makan", "🍜", 35, PocketKind.SPEND, PocketTag.FOOD, 0),
        Pocket(2, "Kos & tagihan", "🔌", 30, PocketKind.SPEND, PocketTag.BILLS, 1),
        Pocket(3, "Transport", "🛵", 10, PocketKind.SPEND, PocketTag.TRANSPORT, 2),
        Pocket(4, "Nongkrong", "🎧", 10, PocketKind.SPEND, PocketTag.FUN, 3),
        Pocket(5, "Tabungan", "🌱", 15, PocketKind.SAVE, PocketTag.SAVINGS, 4),
    )
    private val simple = listOf(
        Pocket(1, "Kebutuhan", "🏠", 50, PocketKind.SPEND, PocketTag.NEEDS, 0),
        Pocket(2, "Keinginan", "☕", 30, PocketKind.SPEND, PocketTag.WANTS, 1),
        Pocket(3, "Tabungan", "🌱", 20, PocketKind.SAVE, PocketTag.SAVINGS, 2),
    )

    @Test
    fun `exact tag wins`() {
        assertEquals(4L, MerchantClassifier.suggest("Kopi Kenangan", kos, emptyMap())?.id)
        assertEquals(2L, MerchantClassifier.suggest("PLN Mobile", kos, emptyMap())?.id)
        assertEquals(1L, MerchantClassifier.suggest("Indomaret Cabang 21", kos, emptyMap())?.id)
    }

    @Test
    fun `fallback to needs and wants`() {
        assertEquals(1L, MerchantClassifier.suggest("PLN Mobile", simple, emptyMap())?.id)
        assertEquals(2L, MerchantClassifier.suggest("Starbucks", simple, emptyMap())?.id)
        assertEquals(2L, MerchantClassifier.suggest("Tokopedia", simple, emptyMap())?.id)
    }

    @Test
    fun `learned rule overrides keywords`() {
        val learned = mapOf(MerchantClassifier.key("Kopi Kenangan") to 1L)
        assertEquals(1L, MerchantClassifier.suggest("KOPI KENANGAN", simple, learned)?.id)
    }

    @Test
    fun `unknown merchant falls to first spend pocket`() {
        assertEquals(1L, MerchantClassifier.suggest("Toko Pak Budi", simple, emptyMap())?.id)
    }
}
