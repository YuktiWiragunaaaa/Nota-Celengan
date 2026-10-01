package id.cukup.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import id.cukup.R

/**
 * Cukup v0.9.7: biru dan putih. Biru tua yang tenang untuk bagian utama (cukup netral supaya warna
 * kategori di grafik tidak bertabrakan), kartu putih untuk isi, dan satu biru terang sebagai aksen.
 */
@Immutable
data class CukupColors(
    val paper: Color,
    val card: Color,
    val surface: Color,
    val ink: Color,
    val mute: Color,
    val faint: Color,
    val line: Color,
    val dark: Color,
    val onDark: Color,
    val onDarkMute: Color,
    val good: Color,
    val caution: Color,
    val over: Color,
    val brand: List<Color>,
    val pockets: List<Color>,
    val isDark: Boolean,
    /** Satu-satunya warna aksen: tombol terpilih, tautan, penanda aktif. */
    val accent: Color,
) {
    fun pocket(index: Int): Color = pockets[((index % pockets.size) + pockets.size) % pockets.size]

    /** Warna pilihan pengguna, atau warna bawaan sesuai urutan. */
    fun of(color: Int?, index: Int): Color = color?.let { Color(it) } ?: pocket(index)

    val brandBrush: Brush get() = Brush.verticalGradient(brand)

    /** Sapuan lembut di belakang saldo Beranda; memudar ke warna halaman. */
    val heroBrush: Brush
        get() = Brush.verticalGradient(
            if (isDark) listOf(Color(0xFF0D2548), Color(0xFF0A1830), paper) else listOf(Color(0xFFD6E6FB), Color(0xFFE9F1FC), paper),
        )
}

/** Warna yang bisa dipilih untuk kantong. Cerah, terbaca di atas gradien dan di atas putih. */
val PocketPalette = listOf(
    Color(0xFFFFC53D), // kuning
    Color(0xFF20C9A6), // tosca
    Color(0xFF8B6CFF), // ungu
    Color(0xFF3DA5FF), // biru
    Color(0xFFFF5C7A), // merah muda
    Color(0xFF7ED957), // hijau
    Color(0xFFF25FC6), // magenta
    Color(0xFFFF8A3D), // jingga
    Color(0xFFB98A64), // cokelat
    Color(0xFFFFB38A), // peach
    Color(0xFFC4A1FF), // lavender
    Color(0xFF2FB8D6), // biru kehijauan
    Color(0xFFFF6F59), // koral
    Color(0xFFB5C94A), // zaitun
    Color(0xFFD65A8A), // anggur
    Color(0xFF7C8CFF), // periwinkle
)

private val Light = CukupColors(
    paper = Color(0xFFF3F6FB),
    card = Color(0xFFFFFFFF),
    surface = Color(0xFFE8EEF6),
    ink = Color(0xFF0B1B33),
    mute = Color(0xFF586A82),
    faint = Color(0xFF9AA9BD),
    line = Color(0xFFE0E7F0),
    dark = Color(0xFF0B1B33),
    onDark = Color(0xFFFFFFFF),
    onDarkMute = Color(0xB3FFFFFF),
    good = Color(0xFF1F9D6B),
    caution = Color(0xFFE08A1E),
    over = Color(0xFFD9392B),
    // Biru tua ke biru sedang: latar grafik tetap gelap dan netral, bukan biru menyala.
    brand = listOf(Color(0xFF081C3A), Color(0xFF0E2F5C), Color(0xFF13427F), Color(0xFF1A559F)),
    pockets = PocketPalette,
    isDark = false,
    accent = Color(0xFF1D63C8),
)

private val Dark = Light.copy(
    // Gelap kebiruan: kartu terasa seperti kaca di atas latar pekat.
    paper = Color(0xFF070C16),
    card = Color(0xFF0F1828),
    surface = Color(0xFF182438),
    ink = Color(0xFFEEF3FA),
    mute = Color(0xFF9FB0C7),
    faint = Color(0xFF63748C),
    line = Color(0xFF1C2A40),
    dark = Color(0xFF000000),
    brand = listOf(Color(0xFF03070F), Color(0xFF081A35), Color(0xFF0E2F5C), Color(0xFF164A8C)),
    isDark = true,
    accent = Color(0xFF5AA2FF),
)

val LocalCukupColors = staticCompositionLocalOf { Light }

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Sans = FontFamily(
    Font(R.font.inter, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.inter, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/**
 * Skala huruf mengikuti rasio emas (1,618): 13 → 21 → 34 → 55.
 * Angka memakai angka tabular supaya rapi saat berubah.
 */
object Type {
    val hero = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 46.sp, lineHeight = 52.sp, letterSpacing = (-0.03).em, fontFeatureSettings = "tnum")
    val number = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
    val display = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em)
    val title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em)
    val statement = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp)
    val amount = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, fontFeatureSettings = "tnum")
    val body = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp)
    val bodySmall = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp)
    val strong = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium)
    val label = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em, fontWeight = FontWeight.Medium)
}

@Composable
fun CukupTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) Dark else Light
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = Color.White, background = c.paper, onBackground = c.ink,
            surface = c.card, onSurface = c.ink, surfaceVariant = c.surface, onSurfaceVariant = c.mute,
            outline = c.line, outlineVariant = c.line, error = c.over, secondary = c.mute,
            surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerLow = c.paper,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = Color.White, background = c.paper, onBackground = c.ink,
            surface = c.card, onSurface = c.ink, surfaceVariant = c.surface, onSurfaceVariant = c.mute,
            outline = c.line, outlineVariant = c.line, error = c.over, secondary = c.mute,
            surfaceContainer = c.card, surfaceContainerHigh = c.card, surfaceContainerLow = c.paper,
        )
    }
    CompositionLocalProvider(LocalCukupColors provides c) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(bodyLarge = Type.body, bodyMedium = Type.bodySmall, labelLarge = Type.strong),
            content = content,
        )
    }
}

val colors: CukupColors
    @Composable get() = LocalCukupColors.current
