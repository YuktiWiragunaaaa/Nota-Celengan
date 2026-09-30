package id.cukup.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.cukup.ui.theme.Type
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Satu gelembung: [weight] menentukan besar, [fill] 0..1 = porsi berwarna (mis. sisa uang); negatif = tint penuh. */
data class Bubble(
    val key: Long,
    val emoji: String,
    val label: String,
    val value: String,
    val weight: Float,
    val fill: Float,
    val color: Color,
)

private data class Placed(val index: Int, val x: Float, val y: Float, val r: Float)

/**
 * Menyusun lingkaran sepadat mungkin di sekitar titik tengah (greedy circle packing).
 * Hasil dalam satuan relatif; nanti diskalakan agar muat di area gambar.
 */
private fun pack(weights: List<Float>): List<Placed> {
    if (weights.isEmpty()) return emptyList()
    val total = weights.sum().takeIf { it > 0f } ?: weights.size.toFloat()
    // Jari-jari sebanding akar porsi (luas sebanding nilai), dengan batas minimum agar tetap bisa disentuh.
    val radii = weights.map { max(sqrt((if (total > 0f) it else 1f) / total), 0.30f) }
    val order = radii.indices.sortedByDescending { radii[it] }
    val gap = 0.04f
    val placed = mutableListOf<Placed>()
    for (i in order) {
        val r = radii[i]
        if (placed.isEmpty()) {
            placed += Placed(i, 0f, 0f, r)
            continue
        }
        var best: Placed? = null
        var bestDist = Float.MAX_VALUE
        for (p in placed) {
            for (step in 0 until 36) {
                val a = step * (Math.PI * 2 / 36)
                val d = p.r + r + gap
                val x = p.x + (d * cos(a)).toFloat()
                val y = p.y + (d * sin(a)).toFloat()
                val clear = placed.all { q -> hypot(q.x - x, q.y - y) >= q.r + r + gap * 0.9f }
                if (clear) {
                    // Bergerombol, sedikit melebar mengikuti bentuk area (sekitar 3:2).
                    val dist = hypot(x / 1.4f, y)
                    if (dist < bestDist) {
                        bestDist = dist
                        best = Placed(i, x, y, r)
                    }
                }
            }
        }
        placed += best ?: Placed(i, placed.maxOf { it.x + it.r } + r + gap, 0f, r)
    }
    return placed
}

/**
 * Grafik gelembung interaktif. Ketuk gelembung untuk memilih; yang terpilih membesar.
 * Dirancang untuk latar gradien gelap: gelembung putih transparan, irisan berwarna.
 */
@Composable
fun BubbleChart(
    bubbles: List<Bubble>,
    selected: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = true,
) {
    val layout = remember(bubbles.map { it.key to it.weight }) { pack(bubbles.map { it.weight }) }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }

    BoxWithConstraints(modifier) {
        if (layout.isEmpty()) return@BoxWithConstraints
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val minX = layout.minOf { it.x - it.r }
        val maxX = layout.maxOf { it.x + it.r }
        val minY = layout.minOf { it.y - it.r }
        val maxY = layout.maxOf { it.y + it.r }
        val scale = min(w / (maxX - minX), h / (maxY - minY)) * 0.96f
        val cx = w / 2 - (minX + maxX) / 2 * scale
        val cy = h / 2 - (minY + maxY) / 2 * scale

        for (p in layout) key(bubbles[p.index].key) {
            val b = bubbles[p.index]
            val isSelected = b.key == selected
            val pop by animateFloatAsState(if (isSelected) 1.08f else 1f, spring(dampingRatio = 0.5f), label = "pop")
            val fill by animateFloatAsState(b.fill.coerceIn(0f, 1f), tween(800), label = "fill")
            val rPx = p.r * scale
            val size = with(density) { (rPx * 2).toDp() }
            val left = with(density) { (cx + p.x * scale - rPx).toDp() }
            val top = with(density) { (cy + p.y * scale - rPx).toDp() }
            Box(
                Modifier
                    .offset(left, top)
                    .size(size)
                    .graphicsLayer {
                        val s = appear.value * pop
                        scaleX = s
                        scaleY = s
                        alpha = appear.value.coerceIn(0f, 1f)
                    }
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                    ) { onSelect(b.key) }
                    .semantics { contentDescription = "${b.label}, ${b.value}" },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = this.size.minDimension / 2f
                    val glow = if (isSelected || bubbles.size == 1) 1f else 0.75f
                    // Liquid glass: badan kaca bening, warna kategori hanya jadi inti bercahaya di tengah.
                    drawCircle(
                        Brush.radialGradient(
                            0f to b.color.copy(alpha = 0.85f * glow),
                            0.3f to b.color.copy(alpha = 0.55f * glow),
                            0.7f to b.color.copy(alpha = 0.10f * glow),
                            1f to Color.White.copy(alpha = if (onDark) 0.08f else 0.05f),
                            center = center, radius = r,
                        ),
                    )
                    if (fill > 0f) {
                        // Irisan dari atas, searah jarum jam = porsi yang tersisa (dipakai di Rencana).
                        drawArc(b.color.copy(alpha = 0.45f * glow), -90f, 360f * fill, useCenter = true)
                    }
                    // Bayangan lembut di bawah supaya terasa bervolume.
                    drawCircle(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.18f)))
                    // Kilau kaca di kiri atas.
                    drawCircle(
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.38f),
                            1f to Color.Transparent,
                            center = Offset(this.size.width * 0.34f, this.size.height * 0.26f), radius = r * 0.55f,
                        ),
                    )
                    // Tepi kaca: terang di atas, memudar ke warna kategori di bawah.
                    drawCircle(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = if (isSelected) 0.95f else 0.6f), b.color.copy(alpha = 0.35f), Color.White.copy(alpha = 0.12f))),
                        radius = r - 0.6.dp.toPx(),
                        style = Stroke(width = if (isSelected) 2.dp.toPx() else 1.2.dp.toPx()),
                    )
                }
                val big = rPx > with(density) { 46.dp.toPx() }
                val textColor = if (onDark) Color.White else Color(0xFF17110E)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val glyph = glyphOf(b.emoji)
                    if (glyph != null) androidx.compose.material3.Icon(glyph, null, tint = textColor, modifier = Modifier.size(if (big) 22.dp else 18.dp))
                    else Text(b.emoji, fontSize = if (big) 20.sp else 16.sp)
                    if (big) {
                        Text(
                            b.label,
                            style = Type.bodySmall.copy(fontSize = 11.sp, lineHeight = 13.sp, shadow = Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 6f)),
                            color = textColor.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.size(width = with(density) { (rPx * 1.2f).toDp() }, height = 14.dp),
                        )
                    }
                    Text(
                        b.value,
                        style = Type.amount.copy(fontSize = if (big) 14.sp else 11.sp, fontWeight = FontWeight.SemiBold, shadow = Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 6f)),
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
