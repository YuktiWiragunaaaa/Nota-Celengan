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

