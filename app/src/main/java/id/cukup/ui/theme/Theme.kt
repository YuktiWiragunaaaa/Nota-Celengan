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
 * Cukup v0.4: hangat dan hidup. Gradien oranye ke merah tua untuk bagian utama,
 * kartu putih bersih untuk isi, warna kantong cerah dan bisa dipilih sendiri.
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
) {
    fun pocket(index: Int): Color = pockets[((index % pockets.size) + pockets.size) % pockets.size]

    /** Warna pilihan pengguna, atau warna bawaan sesuai urutan. */
    fun of(color: Int?, index: Int): Color = color?.let { Color(it) } ?: pocket(index)

    val brandBrush: Brush get() = Brush.verticalGradient(brand)
    val accent: Color get() = brand.last()
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
    Color(0xFF9AA5B1), // abu
)

private val Light = CukupColors(
    paper = Color(0xFFF4F1EE),
    card = Color(0xFFFFFFFF),
    surface = Color(0xFFEFEAE6),
    ink = Color(0xFF17110E),
    mute = Color(0xFF6F6560),
    faint = Color(0xFFABA29D),
    line = Color(0xFFEAE4DF),
    dark = Color(0xFF17110E),
    onDark = Color(0xFFFFFFFF),
    onDarkMute = Color(0xB3FFFFFF),
    good = Color(0xFF1F9D6B),
    caution = Color(0xFFE08A1E),
    over = Color(0xFFD9392B),
    brand = listOf(Color(0xFF3B0A04), Color(0xFF8C1C07), Color(0xFFD9481A), Color(0xFFF2782E)),
    pockets = PocketPalette,
    isDark = false,
)

private val Dark = Light.copy(
    // Gelap netral sedikit ungu: kartu terasa seperti kaca di atas latar pekat.
    paper = Color(0xFF0B0A0D),
    card = Color(0xFF16141B),
    surface = Color(0xFF211E27),
    ink = Color(0xFFF4F1F6),
    mute = Color(0xFFA8A2AE),
    faint = Color(0xFF6E6875),
    line = Color(0xFF29252F),
    dark = Color(0xFF000000),
    brand = listOf(Color(0xFF1A0503), Color(0xFF5E1405), Color(0xFFB63A12), Color(0xFFF2782E)),
    isDark = true,
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
