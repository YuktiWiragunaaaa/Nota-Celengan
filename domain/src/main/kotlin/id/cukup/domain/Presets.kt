package id.cukup.domain

data class CategorySeed(val name: String, val emoji: String, val kind: CategoryKind, val tag: Tag)
data class AccountSeed(val name: String, val emoji: String, val kind: AccountKind)
data class PlanSeed(val name: String, val emoji: String, val percent: Int, val kind: PlanKind, val tags: Set<Tag>)
data class PlanPreset(val id: String, val title: String, val subtitle: String, val pos: List<PlanSeed>)

object Presets {

    /** Kategori bawaan. Bisa diubah, ditambah, dihapus. */
    val categories = listOf(
        CategorySeed("Makan", "🍜", CategoryKind.EXPENSE, Tag.FOOD),
        CategorySeed("Jajan & kopi", "☕", CategoryKind.EXPENSE, Tag.FUN),
        CategorySeed("Transport", "🛵", CategoryKind.EXPENSE, Tag.TRANSPORT),
        CategorySeed("Tagihan & kos", "🏠", CategoryKind.EXPENSE, Tag.BILLS),
        CategorySeed("Belanja", "🛍️", CategoryKind.EXPENSE, Tag.SHOPPING),
        CategorySeed("Hiburan", "🎬", CategoryKind.EXPENSE, Tag.FUN),
        CategorySeed("Kesehatan", "💊", CategoryKind.EXPENSE, Tag.HEALTH),
        CategorySeed("Pendidikan", "📚", CategoryKind.EXPENSE, Tag.EDUCATION),
        CategorySeed("Keluarga", "👨‍👩‍👧", CategoryKind.EXPENSE, Tag.FAMILY),
        CategorySeed("Cicilan", "🧾", CategoryKind.EXPENSE, Tag.DEBT),
        CategorySeed("Investasi", "📈", CategoryKind.EXPENSE, Tag.SAVINGS),
        CategorySeed("Lainnya", "✨", CategoryKind.EXPENSE, Tag.OTHER),
        CategorySeed("Gaji", "💼", CategoryKind.INCOME, Tag.SALARY),
        CategorySeed("Freelance", "💻", CategoryKind.INCOME, Tag.SALARY),
        CategorySeed("Kiriman", "💌", CategoryKind.INCOME, Tag.FAMILY),
        CategorySeed("Lainnya", "💰", CategoryKind.INCOME, Tag.OTHER),
    )

    /** Pilihan cepat saat membuat dompet. */
    val accounts = listOf(
        AccountSeed("Tunai", "💵", AccountKind.CASH),
        AccountSeed("Rekening bank", "🏦", AccountKind.BANK),
        AccountSeed("GoPay", "🟢", AccountKind.EWALLET),
        AccountSeed("OVO", "🟣", AccountKind.EWALLET),
        AccountSeed("DANA", "🔵", AccountKind.EWALLET),
        AccountSeed("ShopeePay", "🟠", AccountKind.EWALLET),
        AccountSeed("Tabungan", "🐷", AccountKind.SAVINGS),
        AccountSeed("Paylater", "🧾", AccountKind.PAYLATER),
    )

    private val needs = setOf(Tag.FOOD, Tag.TRANSPORT, Tag.BILLS, Tag.HEALTH, Tag.EDUCATION, Tag.FAMILY, Tag.OTHER)
    private val wants = setOf(Tag.FUN, Tag.SHOPPING)

    /** Rencana siap pakai. Kategori diarahkan ke pos berdasarkan [Tag]. */
    val plans = listOf(
        PlanPreset(
            "balanced", "50 / 30 / 20", "Kebutuhan, keinginan, tabungan.",
            listOf(
                PlanSeed("Kebutuhan", "🏠", 50, PlanKind.SPEND, needs + Tag.DEBT),
                PlanSeed("Keinginan", "☕", 30, PlanKind.SPEND, wants),
                PlanSeed("Tabungan", "🌱", 20, PlanKind.SAVE, setOf(Tag.SAVINGS)),
            ),
        ),
        PlanPreset(
            "debt", "Lunasi hutang", "Ada cicilan atau paylater.",
            listOf(
                PlanSeed("Kebutuhan", "🏠", 40, PlanKind.SPEND, needs),
                PlanSeed("Cicilan", "🧾", 30, PlanKind.SPEND, setOf(Tag.DEBT)),
                PlanSeed("Tabungan", "🌱", 20, PlanKind.SAVE, setOf(Tag.SAVINGS)),
                PlanSeed("Keinginan", "☕", 10, PlanKind.SPEND, wants),
            ),
        ),
        PlanPreset(
            "half", "Setengah saja", "Belanja maksimal 50%, sisanya ditabung.",
            listOf(
                PlanSeed("Belanja", "🛍️", 50, PlanKind.SPEND, needs + wants + Tag.DEBT),
                PlanSeed("Tabungan", "🌱", 50, PlanKind.SAVE, setOf(Tag.SAVINGS)),
            ),
        ),
    )

    val emojis = listOf(
        "🍜", "🍔", "🍱", "☕", "🧋", "🛒", "🛵", "🚗", "⛽", "🚌", "🏠", "🔌", "📶", "📺", "🎮", "🛍️",
        "👕", "👟", "🧴", "💄", "🎧", "🎬", "🎤", "🏋️", "⚽", "💊", "🩺", "📚", "✏️", "👶", "👨‍👩‍👧", "🐾",
        "🎁", "💍", "💻", "📱", "🙏", "🕌", "⛪", "🌱", "🛟", "✈️", "🏝️", "📈", "💰", "🧾", "💳", "✨",
        "💵", "🏦", "🟢", "🟣", "🔵", "🟠", "🐷", "💼", "💌",
    )
}
