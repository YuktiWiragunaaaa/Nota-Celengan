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
