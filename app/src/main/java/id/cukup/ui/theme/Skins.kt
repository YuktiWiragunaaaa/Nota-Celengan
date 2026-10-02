package id.cukup.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Warna widget layar utama (ARGB). Widget tidak memakai Compose biasa, jadi warnanya disimpan terpisah. */
@Immutable
data class WidgetColors(
    val paper: Long,
    val ink: Long,
    val mute: Long,
    val line: Long,
    val good: Long,
    val caution: Long,
    val over: Long,
    /** Teks di atas [brand]. */
    val onColor: Long,
    val inkInverse: Long,
    /** Latar tombol lembut. */
    val accent: Long,
    /** Warna utama: tombol dan angka yang ditonjolkan. */
    val brand: Long,
)

/**
 * Satu skin = satu set warna lengkap: aplikasi (terang & gelap) dan widget (terang & gelap).
 * Menambah skin baru cukup menambah satu entri di [Skins.all]; layar lain tidak perlu disentuh.
 */
@Immutable
data class Skin(
    /** Kunci yang disimpan di setelan. Jangan diubah setelah dirilis. */
    val id: String,
    val name: String,
    val light: CukupColors,
    val dark: CukupColors,
    val widgetLight: WidgetColors,
    val widgetDark: WidgetColors,
    /** Skin berbayar: hanya bisa dipakai kalau [id] ada di daftar skin yang dimiliki. */
    val premium: Boolean = false,
    /** ID produk di Play Store untuk skin berbayar. Dipakai saat pembelian disambungkan. */
    val productId: String? = null,
) {
    fun usable(owned: Set<String>): Boolean = !premium || id in owned
}

object Skins {
    /** Bawaan: biru dan putih. */
    val Blue = Skin(
        id = "blue",
        name = "Biru",
        light = Light,
        dark = Dark,
        widgetLight = WidgetColors(
            paper = 0xFFFFFFFF, ink = 0xFF0B1B33, mute = 0xFF586A82, line = 0xFFE0E7F0,
            good = 0xFF12855A, caution = 0xFFE08A1E, over = 0xFFCC2F22,
            onColor = 0xFFFFFFFF, inkInverse = 0xFFFFFFFF, accent = 0xFFDCE9FA, brand = 0xFF1D63C8,
        ),
        widgetDark = WidgetColors(
            paper = 0xFF0F1828, ink = 0xFFEEF3FA, mute = 0xFF9FB0C7, line = 0xFF1C2A40,
            good = 0xFF1FA06F, caution = 0xFFD1A263, over = 0xFFE0564A,
            onColor = 0xFFFFFFFF, inkInverse = 0xFF0F1828, accent = 0xFF16304F, brand = 0xFF5AA2FF,
        ),
    )

    /**
     * Contoh skin kedua, sekaligus pola untuk skin berikutnya: salin warna bawaan,
     * lalu ganti yang perlu saja (aksen, sapuan Beranda, latar, kartu).
     */
    val Forest = Skin(
        id = "forest",
        name = "Hutan",
        light = Light.copy(
            surface = Color(0xFFECF4EE), line = Color(0xFFD7E5DB), mute = Color(0xFF4A6355), faint = Color(0xFF7D9487),
            ink = Color(0xFF0C2418), dark = Color(0xFF0C2418),
            accent = Color(0xFF1B8A5A),
            brand = listOf(Color(0xFF06261A), Color(0xFF0C3D2A), Color(0xFF12563B), Color(0xFF196F4C)),
            hero = listOf(Color(0xFF8FD3B0), Color(0xFFC2E8D3), Color(0xFFEBF7F0)),
        ),
        dark = Dark.copy(
            paper = Color(0xFF07120C), card = Color(0xFF0F1F17), surface = Color(0xFF182E22), line = Color(0xFF1C3527),
            mute = Color(0xFFA0BFAE), faint = Color(0xFF648373),
            accent = Color(0xFF5AD49B), onAccent = Color(0xFF052014),
            brand = listOf(Color(0xFF030B07), Color(0xFF082719), Color(0xFF0E402A), Color(0xFF16603F)),
            hero = listOf(Color(0xFF0D3A26), Color(0xFF0A2318)),
        ),
        widgetLight = Blue.widgetLight.copy(ink = 0xFF0C2418, mute = 0xFF4A6355, line = 0xFFD7E5DB, accent = 0xFFDDF1E6, brand = 0xFF1B8A5A),
        widgetDark = Blue.widgetDark.copy(paper = 0xFF0F1F17, line = 0xFF1C3527, mute = 0xFFA0BFAE, inkInverse = 0xFF0F1F17, accent = 0xFF173A29, brand = 0xFF5AD49B),
    )

    /** Semua skin yang tampil di Setelan, urut seperti di sini. Skin baru ditambahkan di daftar ini. */
    val all: List<Skin> = listOf(Blue, Forest)

    /** Skin terpilih; kembali ke bawaan kalau id tidak dikenal atau skinnya belum dimiliki. */
    fun of(id: String?, owned: Set<String> = emptySet()): Skin =
        all.firstOrNull { it.id == id }?.takeIf { it.usable(owned) } ?: Blue
}
