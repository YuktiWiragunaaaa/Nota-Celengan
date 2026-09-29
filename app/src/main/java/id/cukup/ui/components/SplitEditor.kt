package id.cukup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import id.cukup.domain.Pocket
import id.cukup.domain.PocketKind
import id.cukup.domain.PocketTag
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlin.math.roundToInt

/** Kantong yang sedang diedit (id 0 = kantong baru). [key] stabil untuk daftar Compose. */
data class EditablePocket(
    val key: Long,
    val id: Long,
    val name: String,
    val emoji: String,
    val percent: Int,
    val kind: PocketKind,
    val tag: PocketTag,
) {
    fun toPocket(order: Int) = Pocket(id, name.trim().ifBlank { "Kantong" }, emoji, percent, kind, tag, order)

    companion object {
        fun from(p: Pocket, index: Int) = EditablePocket(index.toLong() + 1, p.id, p.name, p.emoji, p.percent, p.kind, p.tag)
    }
}

/** Kata sehari-hari untuk jenis kantong. */
fun PocketKind.label(): String = when (this) {
    PocketKind.SPEND -> "Untuk belanja"
    PocketKind.SAVE -> "Ditabung"
    PocketKind.DEBT -> "Bayar hutang"
}

private val emojis = listOf("🛍️", "🏠", "🍜", "☕", "🛵", "🔌", "🎧", "🌱", "🧾", "🎁", "📚", "💊", "🐾", "✈️", "💻", "🙏")

/** Batang tipis yang menunjukkan pembagian persen secara visual. */
@Composable
fun SplitBar(percents: List<Int>, modifier: Modifier = Modifier) {
    val c = colors
    val total = percents.sum()
    Row(modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(2.dp)).background(c.line)) {
        percents.forEachIndexed { i, p ->
            if (p > 0) Box(Modifier.weight(p.toFloat()).height(10.dp).background(c.pocket(i)))
        }
        if (total < 100) Box(Modifier.weight((100 - total).toFloat()).height(10.dp))
    }
}

/**
 * Editor pembagian uang ke kantong. Setiap kantong: nama, persen (penggeser), dan untuk apa.
 * Total harus 100%.
 */
@Composable
fun SplitEditor(
    pockets: List<EditablePocket>,
    onChange: (List<EditablePocket>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = colors
    val total = pockets.sumOf { it.percent }
    Column(modifier) {
        Column(Modifier.padding(horizontal = Gutter, vertical = 12.dp)) {
            SplitBar(pockets.map { it.percent })
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Total $total%",
                    style = Type.strong,
                    color = if (total == 100) c.ink else c.over,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    when {
                        total == 100 -> "Pas ✓"
                        total < 100 -> "Kurang ${100 - total}%"
                        else -> "Lebih ${total - 100}%"
                    },
                    style = Type.bodySmall,
                    color = if (total == 100) c.mute else c.over,
                )
                if (total != 100 && pockets.isNotEmpty()) {
                    TextAction(
                        "  Pasin otomatis",
                        onClick = {
                            // Selisih ditaruh ke kantong terakhir yang masih muat.
                            val diff = 100 - total
                            val list = pockets.toMutableList()
                            val i = list.indices.reversed().firstOrNull { (list[it].percent + diff) in 0..100 } ?: return@TextAction
                            list[i] = list[i].copy(percent = list[i].percent + diff)
                            onChange(list)
                        },
                    )
                }
            }
        }
        Hairline()
        pockets.forEachIndexed { index, p ->
            PocketEditRow(
                pocket = p,
                color = c.pocket(index),
                canRemove = pockets.size > 1,
                onChange = { updated -> onChange(pockets.toMutableList().also { it[index] = updated }) },
                onRemove = { onChange(pockets.toMutableList().also { it.removeAt(index) }) },
            )
            Hairline()
        }
        LineButton(
            "+ Tambah kantong",
            onClick = {
                val key = (pockets.maxOfOrNull { it.key } ?: 0) + 1
                onChange(
                    pockets + EditablePocket(
                        key, 0, "", emojis[pockets.size % emojis.size], (100 - total).coerceIn(0, 100),
                        PocketKind.SPEND, PocketTag.OTHER,
                    ),
                )
            },
            height = 44.dp,
            modifier = Modifier.padding(horizontal = Gutter, vertical = 14.dp),
        )
    }
}

@Composable
private fun PocketEditRow(
    pocket: EditablePocket,
    color: Color,
    canRemove: Boolean,
    onChange: (EditablePocket) -> Unit,
    onRemove: () -> Unit,
) {
    val c = colors
    Column(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).border(1.dp, c.line, CircleShape)
                    .clickable { onChange(pocket.copy(emoji = emojis[(emojis.indexOf(pocket.emoji) + 1).mod(emojis.size)])) }
                    .semantics { contentDescription = "Ganti ikon" },
                contentAlignment = Alignment.Center,
            ) { Text(pocket.emoji, style = Type.body) }
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = pocket.name,
                onValueChange = { onChange(pocket.copy(name = it.take(24))) },
                singleLine = true,
                textStyle = Type.strong.copy(color = c.ink),
                cursorBrush = SolidColor(c.ink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (pocket.name.isEmpty()) Text("Nama kantong", style = Type.strong, color = c.faint)
                    inner()
                },
            )
            Text("${pocket.percent}%", style = Type.number, color = c.ink)
            if (canRemove) {
                Box(
                    Modifier.padding(start = 6.dp).size(36.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onRemove)
                        .semantics { contentDescription = "Hapus kantong" },
                    contentAlignment = Alignment.Center,
                ) { Text("×", style = Type.body, color = c.faint) }
            }
        }
        Slider(
            value = pocket.percent.toFloat(),
            onValueChange = { onChange(pocket.copy(percent = (it / 5f).roundToInt() * 5)) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = c.ink,
                activeTrackColor = color,
                inactiveTrackColor = c.line,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PocketKind.entries.forEach { k ->
                val on = pocket.kind == k
                Text(
                    k.label(),
                    style = Type.bodySmall,
                    color = if (on) c.paper else c.mute,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(if (on) c.ink else Color.Transparent)
                        .border(1.dp, if (on) c.ink else c.line, RoundedCornerShape(100))
                        .clickable(role = Role.RadioButton) { onChange(pocket.copy(kind = k)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}
