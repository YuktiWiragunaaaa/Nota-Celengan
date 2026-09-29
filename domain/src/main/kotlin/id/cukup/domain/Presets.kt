package id.cukup.domain

data class PresetPocket(val name: String, val emoji: String, val percent: Int, val kind: PocketKind, val tag: PocketTag)
data class Preset(val id: String, val title: String, val subtitle: String, val pockets: List<PresetPocket>)

object Presets {
    val all = listOf(
        Preset(
            "half", "50 : 50", "Setengah dipakai, setengah ditabung.",
            listOf(
                PresetPocket("Belanja", "🛍️", 50, PocketKind.SPEND, PocketTag.NEEDS),
                PresetPocket("Tabungan", "🌱", 50, PocketKind.SAVE, PocketTag.SAVINGS),
            ),
        ),
        Preset(
            "balanced", "50 : 30 : 20", "Kebutuhan, keinginan, tabungan.",
            listOf(
                PresetPocket("Kebutuhan", "🏠", 50, PocketKind.SPEND, PocketTag.NEEDS),
                PresetPocket("Keinginan", "☕", 30, PocketKind.SPEND, PocketTag.WANTS),
                PresetPocket("Tabungan", "🌱", 20, PocketKind.SAVE, PocketTag.SAVINGS),
            ),
        ),
        Preset(
            "debt", "Lunasi hutang", "Ada cicilan atau paylater.",
            listOf(
                PresetPocket("Kebutuhan", "🏠", 40, PocketKind.SPEND, PocketTag.NEEDS),
                PresetPocket("Bayar hutang", "🧾", 30, PocketKind.DEBT, PocketTag.DEBT),
                PresetPocket("Tabungan", "🌱", 20, PocketKind.SAVE, PocketTag.SAVINGS),
                PresetPocket("Keinginan", "☕", 10, PocketKind.SPEND, PocketTag.WANTS),
            ),
        ),
        Preset(
            "custom", "Atur sendiri", "Buat kantong sendiri.",
            listOf(
                PresetPocket("Belanja", "🛍️", 100, PocketKind.SPEND, PocketTag.NEEDS),
            ),
        ),
    )
}

/** Kategori siap pakai untuk kantong baru. Nama dan ikon tetap bisa diubah. */
object Templates {
    val all = listOf(
        PresetPocket("Makan", "🍜", 0, PocketKind.SPEND, PocketTag.FOOD),
        PresetPocket("Jajan & kopi", "☕", 0, PocketKind.SPEND, PocketTag.FUN),
        PresetPocket("Belanja bulanan", "🛒", 0, PocketKind.SPEND, PocketTag.NEEDS),
        PresetPocket("Transport", "🛵", 0, PocketKind.SPEND, PocketTag.TRANSPORT),
        PresetPocket("Bensin", "⛽", 0, PocketKind.SPEND, PocketTag.TRANSPORT),
        PresetPocket("Kos & sewa", "🏠", 0, PocketKind.SPEND, PocketTag.BILLS),
        PresetPocket("Listrik & air", "🔌", 0, PocketKind.SPEND, PocketTag.BILLS),
        PresetPocket("Pulsa & internet", "📶", 0, PocketKind.SPEND, PocketTag.BILLS),
        PresetPocket("Langganan", "📺", 0, PocketKind.SPEND, PocketTag.BILLS),
        PresetPocket("Belanja online", "🛍️", 0, PocketKind.SPEND, PocketTag.SHOPPING),
        PresetPocket("Fashion", "👕", 0, PocketKind.SPEND, PocketTag.SHOPPING),
        PresetPocket("Skincare", "🧴", 0, PocketKind.SPEND, PocketTag.SHOPPING),
        PresetPocket("Nongkrong", "🎧", 0, PocketKind.SPEND, PocketTag.FUN),
        PresetPocket("Hiburan", "🎬", 0, PocketKind.SPEND, PocketTag.FUN),
        PresetPocket("Olahraga", "🏋️", 0, PocketKind.SPEND, PocketTag.FUN),
        PresetPocket("Kesehatan", "💊", 0, PocketKind.SPEND, PocketTag.NEEDS),
        PresetPocket("Pendidikan", "📚", 0, PocketKind.SPEND, PocketTag.NEEDS),
        PresetPocket("Keluarga", "👨‍👩‍👧", 0, PocketKind.SPEND, PocketTag.NEEDS),
        PresetPocket("Hewan peliharaan", "🐾", 0, PocketKind.SPEND, PocketTag.NEEDS),
        PresetPocket("Hadiah", "🎁", 0, PocketKind.SPEND, PocketTag.WANTS),
        PresetPocket("Gadget", "💻", 0, PocketKind.SPEND, PocketTag.WANTS),
        PresetPocket("Sedekah", "🙏", 0, PocketKind.SPEND, PocketTag.OTHER),
        PresetPocket("Tabungan", "🌱", 0, PocketKind.SAVE, PocketTag.SAVINGS),
        PresetPocket("Dana darurat", "🛟", 0, PocketKind.SAVE, PocketTag.SAVINGS),
        PresetPocket("Liburan", "✈️", 0, PocketKind.SAVE, PocketTag.SAVINGS),
        PresetPocket("Investasi", "📈", 0, PocketKind.SAVE, PocketTag.SAVINGS),
        PresetPocket("Bayar hutang", "🧾", 0, PocketKind.DEBT, PocketTag.DEBT),
        PresetPocket("Lainnya", "✨", 0, PocketKind.SPEND, PocketTag.OTHER),
    )

    /** Ikon tambahan untuk kantong buatan sendiri. */
    val emojis = listOf(
        "🍜", "🍔", "🍱", "☕", "🧋", "🛒", "🛵", "🚗", "⛽", "🚌", "🏠", "🔌", "📶", "📺", "🎮", "🛍️",
        "👕", "👟", "🧴", "💄", "🎧", "🎬", "🎤", "🏋️", "⚽", "💊", "🩺", "📚", "✏️", "👶", "👨‍👩‍👧", "🐾",
        "🎁", "💍", "💻", "📱", "🙏", "🕌", "⛪", "🌱", "🛟", "✈️", "🏝️", "📈", "💰", "🧾", "💳", "✨",
    )
}
