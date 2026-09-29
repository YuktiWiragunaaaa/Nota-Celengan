package id.cukup.domain

data class PresetPocket(val name: String, val emoji: String, val percent: Int, val kind: PocketKind, val tag: PocketTag)
data class Preset(val id: String, val title: String, val subtitle: String, val pockets: List<PresetPocket>)

object Presets {
    val all = listOf(
        Preset(
            "balanced", "Seimbang 50/30/20", "Kebutuhan, keinginan, tabungan. Panduan umum literasi keuangan.",
            listOf(
                PresetPocket("Kebutuhan", "🏠", 50, PocketKind.SPEND, PocketTag.NEEDS),
                PresetPocket("Keinginan", "☕", 30, PocketKind.SPEND, PocketTag.WANTS),
                PresetPocket("Tabungan", "🌱", 20, PocketKind.SAVE, PocketTag.SAVINGS),
            ),
        ),
        Preset(
            "debt", "Lunasi hutang", "Untuk yang sedang punya cicilan atau paylater.",
            listOf(
                PresetPocket("Kebutuhan", "🏠", 40, PocketKind.SPEND, PocketTag.NEEDS),
                PresetPocket("Cicilan", "🧾", 30, PocketKind.DEBT, PocketTag.DEBT),
                PresetPocket("Tabungan", "🌱", 20, PocketKind.SAVE, PocketTag.SAVINGS),
                PresetPocket("Keinginan", "☕", 10, PocketKind.SPEND, PocketTag.WANTS),
            ),
        ),
        Preset(
            "kos", "Anak kos", "Dipilah lebih detail untuk hidup sendiri.",
            listOf(
                PresetPocket("Makan", "🍜", 35, PocketKind.SPEND, PocketTag.FOOD),
                PresetPocket("Kos & tagihan", "🔌", 30, PocketKind.SPEND, PocketTag.BILLS),
                PresetPocket("Transport", "🛵", 10, PocketKind.SPEND, PocketTag.TRANSPORT),
                PresetPocket("Nongkrong", "🎧", 10, PocketKind.SPEND, PocketTag.FUN),
                PresetPocket("Tabungan", "🌱", 15, PocketKind.SAVE, PocketTag.SAVINGS),
            ),
        ),
    )
}
