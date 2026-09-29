package id.cukup.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.cukup.domain.Pocket
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

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = colors.mute) {
    Text(text.uppercase(Id), style = Type.label, color = color, modifier = modifier)
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(colors.line))
}

@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = colors
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(if (enabled) c.ink else c.line)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(Id), style = Type.label, color = if (enabled) c.paper else c.faint)
    }
}

@Composable
fun LineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, height: Dp = 52.dp) {
    val c = colors
    Box(
        modifier = modifier
            .height(height)
            .border(BorderStroke(1.dp, c.ink.copy(alpha = 0.8f)), RoundedCornerShape(2.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(Id), style = Type.label, color = c.ink)
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = colors.ink) {
    Text(
        text,
        style = Type.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
        color = color,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp),
    )
}

/** Baris atas sederhana: tombol kembali berupa glyph, judul serif, aksi opsional di kanan. */
@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, action: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("←", style = Type.title, color = colors.ink) }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(title, style = Type.title, color = colors.ink, modifier = Modifier.weight(1f).padding(start = 4.dp))
        action()
    }
}

/** Pilihan kecil berbingkai garis; terisi saat dipilih. */
@Composable
fun Choice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100))
            .background(if (selected) c.ink else Color.Transparent)
            .border(1.dp, if (selected) c.ink else c.line, RoundedCornerShape(100))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.bodySmall, color = if (selected) c.paper else c.ink, maxLines = 1)
    }
}

/**
 * Cincin pos: setiap segmen = persentase pos, isian gelap = sisa uang pos tersebut.
 * [segments]: (warna, porsi dari lingkaran 0..1, sisa 0..1).
 */
@Composable
fun PocketRing(segments: List<Triple<Color, Float, Float>>, modifier: Modifier = Modifier, stroke: Dp = 10.dp) {
    val track = colors.line
    val progress by animateFloatAsState(1f, tween(900), label = "ring")
    Canvas(modifier) {
        val w = stroke.toPx()
        val d = size.minDimension - w
        val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
        val arc = Size(d, d)
        val gap = if (segments.size > 1) 3f else 0f
        drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(w * 0.35f))
        var start = -90f
        for ((color, share, remaining) in segments) {
            val sweep = 360f * share * progress
            val visible = (sweep - gap).coerceAtLeast(0f)
            if (visible > 0f) {
                drawArc(color.copy(alpha = 0.22f), start, visible, false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Butt))
                drawArc(color, start, visible * remaining.coerceIn(0f, 1f), false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Butt))
            }
            start += sweep
        }
    }
}

/** Batang tipis pemakaian pos. */
@Composable
fun UsageBar(ratio: Float, color: Color, modifier: Modifier = Modifier) {
    val c = colors
    val animated by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(700), label = "bar")
    val barColor = when {
        ratio >= 1f -> c.over
        ratio >= 0.8f -> c.caution
        else -> color
    }
    Box(modifier.height(3.dp).fillMaxWidth().background(c.line)) {
        Box(Modifier.fillMaxWidth(animated).height(3.dp).background(barColor))
    }
}

@Composable
fun PocketDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

@Composable
fun AmountText(amount: Long, style: TextStyle = Type.amount, color: Color = colors.ink, modifier: Modifier = Modifier, signed: Boolean = false) {
    val text = if (signed && amount > 0) "+" + Rupiah.format(amount) else Rupiah.format(amount)
    Text(text, style = style, color = color, modifier = modifier, maxLines = 1)
}

/** Satu baris transaksi. */
@Composable
fun TxRow(
    tx: Transaction,
    pocket: Pocket?,
    toPocket: Pocket?,
    pocketColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = colors
    val title = when {
        tx.merchant.isNotBlank() -> tx.merchant
        tx.note.isNotBlank() -> tx.note
        tx.type == TxType.INCOME -> "Pemasukan"
        tx.type == TxType.MOVE -> "Pindah pos"
        else -> "Pengeluaran"
    }
    val subtitle = buildList {
        when (tx.type) {
            TxType.EXPENSE -> pocket?.let { add("${it.emoji} ${it.name}") }
            TxType.INCOME -> add(if (toPocket != null) "→ ${toPocket.emoji} ${toPocket.name}" else "Dibagi ke semua pos")
            TxType.MOVE -> add("${pocket?.name ?: "?"} → ${toPocket?.name ?: "?"}")
        }
        if (tx.isPaylater) add("Paylater")
        tx.sourceApp?.let { add(it) }
        add(time(tx.occurredAt))
    }.joinToString(" · ")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Gutter, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PocketDot(if (tx.type == TxType.INCOME) c.ink else pocketColor)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.strong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = Type.bodySmall, color = c.mute, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        val amountColor = when {
            tx.status == TxStatus.PENDING -> c.faint
            tx.type == TxType.INCOME -> c.ink
            tx.type == TxType.MOVE -> c.mute
            else -> c.ink
        }
        Text(
            when (tx.type) {
                TxType.INCOME -> "+" + Rupiah.format(tx.amount)
                TxType.EXPENSE -> "−" + Rupiah.format(tx.amount)
                TxType.MOVE -> Rupiah.format(tx.amount)
            },
            style = Type.amount.copy(fontSize = Type.amount.fontSize * 0.9f),
            color = amountColor,
            textAlign = TextAlign.End,
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
        modifier.fillMaxWidth().padding(horizontal = Gutter).padding(top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Eyebrow(title)
        trailing?.invoke()
    }
}

val ScreenPadding = PaddingValues(horizontal = Gutter)
