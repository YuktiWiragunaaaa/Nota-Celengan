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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import id.cukup.R

/**
 * Cukup mengikuti arah "classic editorial": putih pecah, arang, garis tipis, tanpa warna aksen.
 * Warna hanya muncul pada pos (tanah, zaitun, pasir…) dan status (oker = hati-hati, bata = lewat).
 */
@Immutable
data class CukupColors(
    val paper: Color,
    val surface: Color,
    val ink: Color,
    val mute: Color,
    val faint: Color,
    val line: Color,
    val dark: Color,
    val onDark: Color,
    val onDarkMute: Color,
    val caution: Color,
    val over: Color,
    val pockets: List<Color>,
    val isDark: Boolean,
) {
    fun pocket(index: Int): Color = pockets[((index % pockets.size) + pockets.size) % pockets.size]
}

private val Light = CukupColors(
    paper = Color(0xFFFBFAF7),
    surface = Color(0xFFF4F2ED),
    ink = Color(0xFF1F1E1C),
    mute = Color(0xFF77756F),
    faint = Color(0xFFAEACA5),
    line = Color(0xFFE6E4DE),
    dark = Color(0xFF2A2926),
    onDark = Color(0xFFF3F1EC),
    onDarkMute = Color(0xFFB9B6AE),
    caution = Color(0xFF9A6B2F),
    over = Color(0xFFA3402F),
    pockets = listOf(
        Color(0xFF2A2926), // arang
        Color(0xFFA0674B), // tanah liat
        Color(0xFF6F7355), // zaitun
        Color(0xFFC4A77D), // pasir
        Color(0xFF66737D), // batu
        Color(0xFFB48780), // mawar kering
        Color(0xFF8C7A5B), // kayu
    ),
    isDark = false,
)

private val Dark = CukupColors(
    paper = Color(0xFF171614),
    surface = Color(0xFF201F1C),
    ink = Color(0xFFF3F1EC),
    mute = Color(0xFFB9B6AE),
    faint = Color(0xFF7D7A73),
    line = Color(0xFF34322E),
    dark = Color(0xFF0F0E0D),
    onDark = Color(0xFFF3F1EC),
    onDarkMute = Color(0xFFB9B6AE),
    caution = Color(0xFFD1A263),
    over = Color(0xFFD9826F),
    pockets = listOf(
        Color(0xFFE8E4DC),
        Color(0xFFC98B6D),
        Color(0xFF9FA37F),
        Color(0xFFD9C095),
        Color(0xFF93A1AB),
        Color(0xFFD1A69F),
        Color(0xFFB5A07C),
    ),
    isDark = true,
)

val LocalCukupColors = staticCompositionLocalOf { Light }

val Serif = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Sans = FontFamily(
    Font(R.font.inter, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

object Type {
    /** Angka besar di beranda. */
    val hero = TextStyle(fontFamily = Serif, fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-0.01).em)
    val display = TextStyle(fontFamily = Serif, fontSize = 36.sp, lineHeight = 40.sp)
    val title = TextStyle(fontFamily = Serif, fontSize = 26.sp, lineHeight = 30.sp)
    val statement = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 20.sp, lineHeight = 26.sp)
    val amount = TextStyle(fontFamily = Serif, fontSize = 20.sp, lineHeight = 24.sp)
    val body = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp)
    val bodySmall = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp)
    val strong = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    /** Eyebrow kecil huruf kapital. */
    val label = TextStyle(fontFamily = Sans, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.16.em, fontWeight = FontWeight.Medium)
}

@Composable
fun CukupTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) Dark else Light
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.ink, onPrimary = c.paper, background = c.paper, onBackground = c.ink,
            surface = c.paper, onSurface = c.ink, surfaceVariant = c.surface, onSurfaceVariant = c.mute,
            outline = c.line, outlineVariant = c.line, error = c.over, secondary = c.mute,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surface, surfaceContainerLow = c.paper,
        )
    } else {
        lightColorScheme(
            primary = c.ink, onPrimary = c.paper, background = c.paper, onBackground = c.ink,
            surface = c.paper, onSurface = c.ink, surfaceVariant = c.surface, onSurfaceVariant = c.mute,
            outline = c.line, outlineVariant = c.line, error = c.over, secondary = c.mute,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surface, surfaceContainerLow = c.paper,
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
