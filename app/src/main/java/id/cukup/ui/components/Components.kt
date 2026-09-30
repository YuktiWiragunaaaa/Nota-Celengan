package id.cukup.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import id.cukup.domain.Account
import id.cukup.domain.Category
import id.cukup.domain.Rupiah
import id.cukup.domain.Transaction
import id.cukup.domain.TxStatus
import id.cukup.domain.TxType
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val Gutter = 20.dp
val Id = Locale.forLanguageTag("id-ID")
val Pill = RoundedCornerShape(100)
val CardShape = RoundedCornerShape(24.dp)

/** Label kecil di atas angka/bagian. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = colors.mute) {
    Text(text, style = Type.label, color = color, modifier = modifier)
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(colors.line))
}

/** Tombol utama: pil gelap. */
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = colors
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(Pill)
            .background(if (enabled) c.ink else c.line)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) c.card else c.faint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Type.strong, color = if (enabled) c.card else c.faint, maxLines = 1)
    }
}

/** Tombol kedua: pil terang. */
@Composable
fun LineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    icon: ImageVector? = null,
) {
    val c = colors
    Row(
        modifier = modifier
            .height(height)
            .clip(Pill)
            .background(c.card)
            .border(1.dp, c.line, Pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Type.strong, color = c.ink, maxLines = 1)
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = colors.mute) {
    Text(
        text,
        style = Type.bodySmall,
        color = color,
        modifier = modifier.clip(Pill).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp),
    )
}

/** Tombol ikon bulat. */
@Composable
fun RoundIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = colors.card,
    tint: Color = colors.ink,
    size: Dp = 44.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.45f)) }
}

/** Baris atas: tombol kembali bulat dan judul. */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, action: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onBack)
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = Type.title, color = colors.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        action()
    }
}

/** Pilihan berbentuk pil; terisi saat dipilih. */
@Composable
fun Choice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = colors
    Box(
        modifier = modifier
            .clip(Pill)
            .background(if (selected) c.ink else c.card)
            .border(1.dp, if (selected) c.ink else c.line, Pill)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.bodySmall, color = if (selected) c.card else c.ink, maxLines = 1)
    }
}

/** Cincin sederhana: [segments] = (warna, porsi 0..1, sisa 0..1). */
@Composable
fun PocketRing(segments: List<Triple<Color, Float, Float>>, modifier: Modifier = Modifier, stroke: Dp = 10.dp) {
    val track = colors.line
    val progress by animateFloatAsState(1f, tween(900), label = "ring")
    Canvas(modifier) {
        val w = stroke.toPx()
        val d = size.minDimension - w
        val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
        val arc = Size(d, d)
        drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(w))
        var start = -90f
        for ((color, share, remaining) in segments) {
            val sweep = 360f * share * progress
            if (sweep > 2f) {
                drawArc(color.copy(alpha = 0.25f), start, sweep - 2f, false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Butt))
                drawArc(color, start, (sweep - 2f) * remaining.coerceIn(0f, 1f), false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Butt))
            }
            start += sweep
        }
    }
}

/** Batang pemakaian kantong. */
@Composable
fun UsageBar(ratio: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val c = colors
    val animated by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(700), label = "bar")
    val barColor = when {
        ratio >= 1f -> c.over
        ratio >= 0.8f -> c.caution
        else -> color
    }
    Box(modifier.height(height).fillMaxWidth().clip(Pill).background(c.line)) {
        Box(Modifier.fillMaxWidth(animated).height(height).clip(Pill).background(barColor))
    }
}

@Composable
fun PocketDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Ikon kantong: emoji di dalam lingkaran berwarna lembut. */
@Composable
fun PocketBadge(emoji: String, color: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, style = Type.body.copy(fontSize = Type.body.fontSize * (size.value / 44f))) }
}

@Composable
fun AmountText(amount: Long, style: TextStyle = Type.amount, color: Color = colors.ink, modifier: Modifier = Modifier) {
    Text(Rupiah.format(amount), style = style, color = color, modifier = modifier, maxLines = 1)
}

/** Satu baris transaksi: ikon kategori, judul, dompet & jam, nominal. */
@Composable
fun TxRow(
    tx: Transaction,
    category: Category?,
    account: Account?,
    toAccount: Account?,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = colors
    val title = when {
        tx.merchant.isNotBlank() -> tx.merchant
        tx.note.isNotBlank() -> tx.note
        tx.type == TxType.TRANSFER -> "Pindah uang"
        category != null -> category.name
        tx.type == TxType.INCOME -> "Uang masuk"
        else -> "Uang keluar"
    }
    val emoji = when (tx.type) {
        TxType.TRANSFER -> "🔁"
        else -> category?.emoji ?: if (tx.type == TxType.INCOME) "💰" else "🧾"
    }
    val subtitle = buildList {
        when (tx.type) {
            TxType.TRANSFER -> add("${account?.name ?: "?"} → ${toAccount?.name ?: "?"}")
            else -> {
                if (category != null && title != category.name) add(category.name)
                account?.let { add(it.name) }
            }
        }
        if (tx.status == TxStatus.PENDING) add("perlu dicek")
        add(time(tx.occurredAt))
    }.filter { it.isNotBlank() }.joinToString(" · ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Gutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PocketBadge(emoji, if (tx.type == TxType.TRANSFER) c.mute else color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.strong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = Type.bodySmall, color = c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            when (tx.type) {
                TxType.INCOME -> "+" + Rupiah.format(tx.amount)
                TxType.EXPENSE -> "−" + Rupiah.format(tx.amount)
                TxType.TRANSFER -> Rupiah.format(tx.amount)
            },
            style = Type.amount,
            color = when {
                tx.status == TxStatus.PENDING -> c.faint
                tx.type == TxType.INCOME -> c.good
                tx.type == TxType.EXPENSE -> c.ink
                else -> c.mute
            },
        )
    }
}

private val timeFmt = DateTimeFormatter.ofPattern("HH.mm", Id)
private val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Id)

fun time(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFmt)

fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Hari ini"
    today.minusDays(1) -> "Kemarin"
    else -> date.format(dayFmt).replaceFirstChar { it.titlecase(Id) }
}

fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = 28.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = Type.title, color = colors.ink)
        trailing?.invoke()
    }
}

/** Kartu putih membulat. */
@Composable
fun Card(modifier: Modifier = Modifier, padding: Dp = 18.dp, content: @Composable () -> Unit) {
    Box(modifier.clip(CardShape).background(colors.card).padding(padding)) { content() }
}

/**
 * Ikon status bar terang (untuk latar gradien) atau gelap. Dikembalikan saat layar ditinggal.
 */
@Composable
fun LightStatusBarIcons(light: Boolean) {
    val activity = LocalActivity.current ?: return
    val isDarkTheme = colors.isDark
    DisposableEffect(light, isDarkTheme) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = !light && !isDarkTheme
        onDispose { }
    }
}

val ScreenPadding = PaddingValues(horizontal = Gutter)
