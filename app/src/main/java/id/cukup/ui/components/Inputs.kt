package id.cukup.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.cukup.domain.Pocket
import id.cukup.domain.PocketKind
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

/** Keypad angka besar. Nilai maksimal 12 digit (ratusan miliar). */
@Composable
fun Keypad(value: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier, keyHeight: androidx.compose.ui.unit.Dp = 58.dp) {
    val haptic = LocalHapticFeedback.current
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("000", "0", "⌫"))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (key in row) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(keyHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .clickable(role = Role.Button) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val s = value.takeIf { it > 0 }?.toString() ?: ""
                                val next = when (key) {
                                    "⌫" -> s.dropLast(1)
                                    else -> (s + key).take(12)
                                }
                                onChange(next.toLongOrNull() ?: 0L)
                            }
                            .semantics { if (key == "⌫") contentDescription = "Hapus" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, style = Type.number, color = colors.ink)
                    }
                }
            }
        }
    }
}

/** Deretan pos yang bisa dipilih, bergulir horizontal. */
@Composable
fun PocketPicker(
    pockets: List<Pocket>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    extra: (@Composable () -> Unit)? = null,
) {
    val c = colors
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        extra?.invoke()
        pockets.forEachIndexed { i, p ->
            val selected = p.id == selectedId
            Row(
                Modifier
                    .clip(RoundedCornerShape(100))
                    .background(if (selected) c.ink else c.paper)
                    .border(1.dp, if (selected) c.ink else c.line, RoundedCornerShape(100))
                    .clickable(role = Role.RadioButton) { onSelect(p.id) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PocketDot(c.pocket(i))
                Spacer(Modifier.width(8.dp))
                Text("${p.emoji} ${p.name}", style = Type.bodySmall, color = if (selected) c.paper else c.ink, maxLines = 1)
            }
        }
    }
}

@Composable
fun LineField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalize: Boolean = true,
) {
    val c = colors
    Box(
        modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, c.line), RoundedCornerShape(2.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, style = Type.body, color = c.faint)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = Type.body.copy(color = c.ink),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
                capitalization = if (capitalize) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Data pos yang sedang diedit (id 0 = pos baru). [key] stabil untuk daftar Compose. */
data class EditablePocket(
    val key: Long,
    val id: Long,
    val name: String,
    val emoji: String,
    val percent: Int,
    val kind: PocketKind,
    val tag: id.cukup.domain.PocketTag,
) {
    fun toPocket(order: Int) = Pocket(id, name.trim().ifBlank { "Pos" }, emoji, percent, kind, tag, order)

    companion object {
        fun from(p: Pocket) = EditablePocket(p.id * 1000 + p.sortOrder + 1, p.id, p.name, p.emoji, p.percent, p.kind, p.tag)
    }
}

private val emojis = listOf("🏠", "🍜", "☕", "🛵", "🔌", "🎧", "🛍️", "🌱", "🧾", "🎁", "📚", "💊", "🐾", "✈️", "💻", "🙏", "💍", "🧸")

/**
 * Editor persentase pos. Menampilkan total dan sisa agar mudah mencapai 100%.
 */
@Composable
fun PocketEditor(
    pockets: List<EditablePocket>,
    onChange: (List<EditablePocket>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = colors
    val total = pockets.sumOf { it.percent }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Total")
                Text("$total%", style = Type.number.copy(fontSize = Type.display.fontSize), color = if (total == 100) c.ink else c.over)
            }
            Text(
                when {
                    total == 100 -> "Pas. Semua uang punya tempat."
                    total < 100 -> "Sisa ${100 - total}% belum punya pos."
                    else -> "Lebih ${total - 100}%. Kurangi salah satu."
                },
                style = Type.statement.copy(fontSize = Type.bodySmall.fontSize * 1.25f),
                color = c.mute,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Hairline()
        pockets.forEachIndexed { index, p ->
            PocketEditRow(
                pocket = p,
                color = c.pocket(index),
                canRemove = pockets.size > 1,
                remainder = 100 - total,
                onChange = { updated -> onChange(pockets.toMutableList().also { it[index] = updated }) },
                onRemove = { onChange(pockets.toMutableList().also { it.removeAt(index) }) },
                onMoveUp = if (index > 0) {
                    { onChange(pockets.toMutableList().also { val x = it.removeAt(index); it.add(index - 1, x) }) }
                } else {
                    null
                },
            )
            Hairline()
        }
        Row(Modifier.padding(horizontal = Gutter, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LineButton("+ Tambah pos", onClick = {
                val key = (pockets.maxOfOrNull { it.key } ?: 0) + 1
                onChange(
                    pockets + EditablePocket(
                        key, 0, "", emojis[pockets.size % emojis.size], (100 - total).coerceIn(0, 100),
                        PocketKind.SPEND, id.cukup.domain.PocketTag.OTHER,
                    ),
                )
            }, height = 44.dp)
        }
    }
}

@Composable
private fun PocketEditRow(
    pocket: EditablePocket,
    color: androidx.compose.ui.graphics.Color,
    canRemove: Boolean,
    remainder: Int,
    onChange: (EditablePocket) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: (() -> Unit)?,
) {
    val c = colors
    Column(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).border(1.dp, c.line, CircleShape)
                    .clickable { onChange(pocket.copy(emoji = emojis[(emojis.indexOf(pocket.emoji) + 1).mod(emojis.size)])) },
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
                    if (pocket.name.isEmpty()) Text("Nama pos", style = Type.strong, color = c.faint)
                    inner()
                },
            )
            Stepper(pocket.percent, onChange = { onChange(pocket.copy(percent = it)) }, remainder = remainder)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PocketDot(color, Modifier.padding(start = 16.dp, end = 24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                KindChip("Pakai", pocket.kind == PocketKind.SPEND) { onChange(pocket.copy(kind = PocketKind.SPEND)) }
                KindChip("Simpan", pocket.kind == PocketKind.SAVE) { onChange(pocket.copy(kind = PocketKind.SAVE)) }
                KindChip("Cicilan", pocket.kind == PocketKind.DEBT) { onChange(pocket.copy(kind = PocketKind.DEBT)) }
            }
            if (onMoveUp != null) SmallGlyph("↑", "Naikkan", onMoveUp)
            if (canRemove) SmallGlyph("×", "Hapus pos", onRemove)
        }
    }
}

@Composable
private fun KindChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = colors
    Text(
        text,
        style = Type.label,
        color = if (selected) c.ink else c.faint,
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, if (selected) c.ink else c.line, RoundedCornerShape(2.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
private fun SmallGlyph(glyph: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Text(glyph, style = Type.body, color = colors.mute) }
}

/** −  25%  +  (langkah 5; ketuk angka untuk mengisi sisa). */
@Composable
private fun Stepper(value: Int, onChange: (Int) -> Unit, remainder: Int) {
    val c = colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        SmallGlyph("−", "Kurangi") { onChange((value - 5).coerceAtLeast(0)) }
        Text(
            "$value%",
            style = Type.amount,
            color = c.ink,
            modifier = Modifier
                .width(56.dp)
                .clickable(enabled = remainder != 0) { onChange((value + remainder).coerceIn(0, 100)) }
                .semantics { contentDescription = "$value persen. Ketuk untuk mengisi sisa." },
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        SmallGlyph("+", "Tambah") { onChange((value + 5).coerceAtMost(100)) }
    }
}
