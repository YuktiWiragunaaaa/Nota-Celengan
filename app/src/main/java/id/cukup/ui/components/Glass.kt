package id.cukup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Church
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.cukup.domain.Account
import id.cukup.domain.Brands
import id.cukup.ui.theme.colors

/** Emoji pilihan pengguna → ikon garis yang seragam. Emoji tanpa pasangan tetap tampil sebagai emoji. */
private val glyphs: Map<String, ImageVector> = mapOf(
    "🍜" to Icons.Rounded.RamenDining, "🍔" to Icons.Rounded.LunchDining, "🍱" to Icons.Rounded.Fastfood,
    "☕" to Icons.Rounded.LocalCafe, "🧋" to Icons.Rounded.LocalDrink, "🛒" to Icons.Rounded.ShoppingCart,
    "🛵" to Icons.Rounded.TwoWheeler, "🚗" to Icons.Rounded.DirectionsCar, "⛽" to Icons.Rounded.LocalGasStation,
    "🚌" to Icons.Rounded.DirectionsBus, "🏠" to Icons.Rounded.Home, "🔌" to Icons.Rounded.Power,
    "📶" to Icons.Rounded.Wifi, "📺" to Icons.Rounded.Tv, "🎮" to Icons.Rounded.SportsEsports,
    "🛍️" to Icons.Rounded.ShoppingBag, "👕" to Icons.Rounded.Checkroom, "👟" to Icons.Rounded.Checkroom,
    "🧴" to Icons.Rounded.Spa, "💄" to Icons.Rounded.Face, "🎧" to Icons.Rounded.Headphones,
    "🎬" to Icons.Rounded.Movie, "🎤" to Icons.Rounded.Mic, "🏋️" to Icons.Rounded.FitnessCenter,
    "⚽" to Icons.Rounded.SportsSoccer, "💊" to Icons.Rounded.Medication, "🩺" to Icons.Rounded.MedicalServices,
    "📚" to Icons.AutoMirrored.Rounded.MenuBook, "✏️" to Icons.Rounded.Edit, "👶" to Icons.Rounded.ChildCare,
    "👨‍👩‍👧" to Icons.Rounded.FamilyRestroom, "🐾" to Icons.Rounded.Pets, "🎁" to Icons.Rounded.CardGiftcard,
    "💍" to Icons.Rounded.Diamond, "💻" to Icons.Rounded.Laptop, "📱" to Icons.Rounded.PhoneAndroid,
    "🙏" to Icons.Rounded.VolunteerActivism, "🕌" to Icons.Rounded.Mosque, "⛪" to Icons.Rounded.Church,
    "🌱" to Icons.Rounded.Eco, "🛟" to Icons.Rounded.Security, "✈️" to Icons.Rounded.Flight,
    "🏝️" to Icons.Rounded.BeachAccess, "📈" to Icons.AutoMirrored.Rounded.TrendingUp, "💰" to Icons.Rounded.Savings,
    "🧾" to Icons.Rounded.Receipt, "💳" to Icons.Rounded.CreditCard, "✨" to Icons.Rounded.AutoAwesome,
    "💵" to Icons.Rounded.Payments, "🏦" to Icons.Rounded.AccountBalance, "👛" to Icons.Rounded.AccountBalanceWallet,
    "🟢" to Icons.Rounded.AccountBalanceWallet, "🟣" to Icons.Rounded.AccountBalanceWallet,
    "🔵" to Icons.Rounded.AccountBalanceWallet, "🟠" to Icons.Rounded.AccountBalanceWallet,
    "🐷" to Icons.Rounded.Savings, "💼" to Icons.Rounded.Work, "💌" to Icons.Rounded.Mail,
    "➕" to Icons.Rounded.Add, "💾" to Icons.Rounded.CloudUpload, "📂" to Icons.Rounded.CloudDownload,
)

fun glyphOf(emoji: String): ImageVector? = glyphs[emoji]

/**
 * Ikon kaca: kotak membulat tembus pandang berwarna [tint], kilau di bagian atas, garis tepi terang.
 * [mark] (mis. "BCA") dipakai untuk dompet bermerek; selain itu [emoji] diubah jadi ikon garis.
 * [onDark] untuk dipasang di atas gradien gelap/oranye.
 */
@Composable
fun GlassIcon(
    emoji: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    mark: String? = null,
    onDark: Boolean = false,
) {
    val c = colors
    val shape = RoundedCornerShape(size * 0.34f)
    val base = if (onDark) Color.White else tint
    val fill = Brush.linearGradient(
        listOf(base.copy(alpha = if (onDark) 0.26f else 0.30f), base.copy(alpha = if (onDark) 0.10f else 0.10f)),
    )
    val rim = Brush.linearGradient(
        listOf(Color.White.copy(alpha = if (c.isDark) 0.35f else 0.9f), base.copy(alpha = 0.25f), Color.White.copy(alpha = 0.15f)),
    )
    // Ikon sedikit lebih gelap dari warna aslinya supaya kontras di atas kaca terang.
    val ink = if (onDark) Color.White else if (c.isDark) lerp(tint, Color.White, 0.25f) else lerp(tint, Color.Black, 0.28f)
    Box(
        modifier.size(size).clip(shape).background(fill).border(1.dp, rim, shape),
        contentAlignment = Alignment.Center,
    ) {
        // Kilau kaca di separuh atas.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (c.isDark) 0.10f else 0.45f),
                    0.5f to Color.White.copy(alpha = 0f),
                ),
            ),
        )
        val glyph = glyphOf(emoji)
        when {
            mark != null -> Text(
                mark, color = ink, fontWeight = FontWeight.Bold,
                fontSize = (size.value * if (mark.length >= 3) 0.26f else 0.34f).sp, maxLines = 1,
            )
            glyph != null -> Icon(glyph, null, tint = ink, modifier = Modifier.size(size * 0.5f))
            else -> Text(emoji, fontSize = (size.value * 0.46f).sp)
        }
    }
}

/** Ikon dompet: monogram brand (BCA, OVO) atau ikon jenisnya. */
@Composable
fun AccountIcon(account: Account?, modifier: Modifier = Modifier, size: Dp = 44.dp, onDark: Boolean = false) {
    val brand = account?.let { Brands.forAccountName(it.name) }
    GlassIcon(account?.emoji ?: "👛", colorOf(account), modifier, size, mark = brand?.mark, onDark = onDark)
}
