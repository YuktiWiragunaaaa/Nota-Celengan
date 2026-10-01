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

/** Satu pilihan di [ChipPicker]. */
data class PickItem(val id: Long, val label: String, val emoji: String, val color: androidx.compose.ui.graphics.Color, val detail: String = "", val mark: String? = null)

/** Deretan pilihan (kategori, dompet) yang bergulir horizontal. */
@Composable
fun ChipPicker(
    items: List<PickItem>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = colors
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { p ->
            val selected = p.id == selectedId
            Row(
                Modifier
                    .clip(Pill)
                    .background(if (selected) c.accent else c.card)
                    .border(1.dp, if (selected) c.accent else c.line, Pill)
                    .clickable(role = Role.RadioButton) { onSelect(p.id) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIcon(p.emoji, p.color, size = 26.dp, mark = p.mark)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(p.label, style = Type.bodySmall, color = if (selected) c.onAccent else c.ink, maxLines = 1)
                    if (p.detail.isNotBlank()) {
                        Text(p.detail, style = Type.label, color = if (selected) c.onAccent.copy(alpha = 0.75f) else c.faint, maxLines = 1)
                    }
                }
            }
        }
        trailing?.invoke()
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
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(BorderStroke(1.dp, c.line), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, style = Type.body, color = c.faint)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = Type.body.copy(color = c.ink),
            cursorBrush = SolidColor(c.accent),
            visualTransformation = if (keyboardType == KeyboardType.Number) ThousandDots else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
                capitalization = if (capitalize) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


/** Menampilkan "4000000" sebagai "4.000.000" saat diketik; nilai aslinya tetap angka polos. */
private object ThousandDots : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val raw = text.text
        if (raw.isEmpty() || !raw.all(Char::isDigit)) {
            return androidx.compose.ui.text.input.TransformedText(text, androidx.compose.ui.text.input.OffsetMapping.Identity)
        }
        val out = raw.reversed().chunked(3).joinToString(".").reversed()
        val mapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var digits = 0
                for (j in out.indices) {
                    if (digits == offset) return j
                    if (out[j] != '.') digits++
                }
                return out.length
            }
            override fun transformedToOriginal(offset: Int): Int = out.take(offset.coerceIn(0, out.length)).count { it != '.' }
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(out), mapping)
    }
}
