package id.cukup.ui.components

import id.cukup.ui.theme.colors
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.cukup.ui.theme.Type
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/** Satu bagian grafik. */
data class Slice(val key: Long, val label: String, val emoji: String, val value: Float, val color: Color, val valueText: String)

/**
 * Donat interaktif. Sentuh irisan untuk memilih; irisan terpilih menebal, yang lain meredup.
 * [center] digambar di tengah lubang.
 */
@Composable
fun DonutChart(
    slices: List<Slice>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    track: Color = Color.White.copy(alpha = 0.12f),
    /** Tekan-tahan sebuah irisan (mis. untuk ganti warnanya). */
    onLongPress: (Long) -> Unit = {},
    center: @Composable () -> Unit = {},
) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) { sweep.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val total = slices.sumOf { it.value.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
    // Tiap irisan punya animasi pilih sendiri (pegas). Nilainya baru dibaca saat menggambar,
    // jadi animasi hanya menggambar ulang, tidak menyusun ulang seluruh grafik tiap frame.
    val lift = slices.map { s ->
        key(s.key) {
            animateFloatAsState(
                if (s.key == selected) 1f else if (selected != null) -1f else 0f,
                spring(dampingRatio = 0.6f, stiffness = 300f), label = "lift",
            )
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(slices) {
                // Irisan di titik sentuh; null bila di lubang tengah atau di luar cincin.
                fun hit(p: Offset): Slice? {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = min(size.width, size.height) / 2f
                    val d = hypot(p.x - cx, p.y - cy)
                    if (d < r * 0.5f || d > r * 1.05f) return null
                    var angle = Math.toDegrees(atan2((p.y - cy).toDouble(), (p.x - cx).toDouble())).toFloat() + 90f
                    if (angle < 0) angle += 360f
                    var acc = 0f
                    for (s in slices) {
                        acc += s.value / total * 360f
                        if (angle <= acc) return s
                    }
                    return null
                }
                detectTapGestures(
                    onLongPress = { p -> hit(p)?.let { onLongPress(it.key) } },
                    onTap = { p -> onSelect(hit(p)?.key) },
                )
            },
        ) {
            val r = size.minDimension / 2f
            val thick = r * 0.28f
            val box = Size((r - thick / 2) * 2, (r - thick / 2) * 2)
            val tl = Offset(size.width / 2 - box.width / 2, size.height / 2 - box.height / 2)
            drawArc(track, 0f, 360f, false, tl, box, style = Stroke(thick))
            // Celah antar irisan dalam derajat, termasuk ruang untuk ujung membulat.
            val gap = if (slices.size > 1) Math.toDegrees((thick / (r - thick / 2)).toDouble()).toFloat() * 1.2f else 0f
            var start = -90f
            slices.forEachIndexed { i, s ->
                val full = s.value / total * 360f
                val l = lift[i].value
                val w = thick * (1f + 0.22f * l.coerceAtLeast(0f))
                val alpha = 1f - 0.6f * (-l).coerceAtLeast(0f)
                val sweepDeg = ((full - gap).coerceAtLeast(0.5f)) * sweep.value
                if (full > 0.5f) {
                    // Cahaya lembut di belakang irisan.
                    drawArc(s.color.copy(alpha = 0.18f * alpha), start + gap / 2, sweepDeg, false, tl, box, style = Stroke(w * 1.7f, cap = StrokeCap.Round))
                    drawArc(s.color.copy(alpha = alpha), start + gap / 2, sweepDeg, false, tl, box, style = Stroke(w, cap = StrokeCap.Round))
                }
                start += full
            }
        }
        center()
    }
}

/** Batang horizontal per kantong. Sentuh baris untuk memilih. */
@Composable
fun HBarChart(
    slices: List<Slice>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    track: Color = Color.White.copy(alpha = 0.12f),
    onLongPress: (Long) -> Unit = {},
) {
    val max = slices.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        slices.forEach { s ->
            key(s.key) {
                // State animasi dibaca di drawBehind (fase gambar), bukan saat menyusun layout.
                val grow = animateFloatAsState(s.value / max, spring(dampingRatio = 0.75f, stiffness = 120f), label = "bar")
                val dim = selected != null && selected != s.key
                val fade = animateFloatAsState(if (dim) 0.35f else 1f, tween(250), label = "fade")
                Row(
                    Modifier.fillMaxWidth().clip(Pill)
                        .combinedClickable(onLongClick = { onLongPress(s.key) }) { onSelect(if (selected == s.key) null else s.key) }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassIcon(s.emoji, s.color, size = 28.dp, onDark = textColor == Color.White)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(
                                s.label, style = Type.bodySmall, color = textColor.copy(alpha = if (dim) 0.5f else 0.9f),
                                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(s.valueText, style = Type.label, color = textColor.copy(alpha = if (dim) 0.5f else 1f))
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth().height(10.dp).clip(Pill).background(track)) {
                            Box(
                                Modifier.fillMaxSize().drawBehind {
                                    val w = size.width * grow.value.coerceIn(0.02f, 1f)
                                    val a = fade.value
                                    drawRoundRect(
                                        Brush.horizontalGradient(listOf(s.color.copy(alpha = 0.55f * a), s.color.copy(alpha = a)), endX = w),
                                        size = Size(w, size.height), cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Batang vertikal per hari. Sentuh batang untuk memilih hari; nilai tampil di atas batang terpilih.
 */
@Composable
fun DayBarChart(
    values: List<Long>,
    labels: List<String>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    barColor: Color,
    modifier: Modifier = Modifier,
    faint: Color = Color.Gray,
    limitPerDay: Long = 0,
    limitColor: Color = Color.Gray,
) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(values) { grow.snapTo(0f); grow.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 90f)) }
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(limitPerDay).coerceAtLeast(1L)
    Column(modifier) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            val n = values.size.coerceAtLeast(1)
            Canvas(
                Modifier.fillMaxSize().pointerInput(values) {
                    detectTapGestures { p ->
                        val i = (p.x / (size.width / n)).toInt().coerceIn(0, n - 1)
                        onSelect(if (selected == i) null else i)
                    }
                },
            ) {
                val slot = size.width / n
                val barW = (slot * 0.56f).coerceAtMost(28.dp.toPx())
                val topPad = 22.dp.toPx()
                val h = size.height - topPad
                if (limitPerDay > 0) {
                    val y = topPad + h - h * (limitPerDay.toFloat() / max)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(limitColor, Offset(x, y), Offset((x + 6.dp.toPx()).coerceAtMost(size.width), y), strokeWidth = 1.5.dp.toPx())
                        x += 10.dp.toPx()
                    }
                }
                values.forEachIndexed { i, v ->
                    val bh = (h * (v.toFloat() / max) * grow.value).coerceAtLeast(if (v > 0) 3.dp.toPx() else 2.dp.toPx())
                    val left = slot * i + (slot - barW) / 2
                    val isSel = selected == i
                    val color = when {
                        v == 0L -> faint
                        selected != null && !isSel -> barColor.copy(alpha = 0.35f)
                        else -> barColor
                    }
                    val top = topPad + h - bh
                    drawRoundRect(
                        Brush.verticalGradient(listOf(color, color.copy(alpha = color.alpha * 0.45f)), startY = top, endY = top + bh),
                        Offset(left, top), Size(barW, bh), CornerRadius(barW / 2, barW / 2),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text(
                    l, style = Type.label.copy(fontSize = 10.sp),
                    color = if (selected == i) barColor else faint,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1,
                )
            }
        }
    }
}

/** Pilihan jenis grafik: ikon kecil dalam pil. */
@Composable
fun ChartSwitch(options: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>, current: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(Pill).background(Color.White.copy(alpha = 0.14f)).padding(3.dp)) {
        options.forEach { (id, icon) ->
            val on = id == current
            Box(
                Modifier.clip(Pill).background(if (on) Color.White else Color.Transparent).clickable { onPick(id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(icon, id, tint = if (on) colors.accent else Color.White, modifier = Modifier.width(18.dp).height(18.dp))
            }
        }
    }
}
