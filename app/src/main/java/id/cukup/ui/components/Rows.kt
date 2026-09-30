package id.cukup.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.cukup.domain.Presets
import id.cukup.domain.Rupiah
import id.cukup.ui.theme.PocketPalette
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

/** "<i>kata</i>" dicetak miring. */
fun italicize(s: String): AnnotatedString = buildAnnotatedString {
    s.split("<i>", "</i>").forEachIndexed { i, part ->
        if (i % 2 == 1) {
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(part)
            pop()
        } else {
            append(part)
        }
    }
}

/** Judul kecil pengelompokan dalam daftar setelan. */
@Composable
fun Group(title: String) {
    Eyebrow(title, Modifier.padding(horizontal = Gutter).padding(top = 24.dp, bottom = 8.dp), color = colors.accent)
}

/** Baris yang bisa diketuk: judul, keterangan, panah. */
@Composable
fun Link(
    title: String,
    value: String,
    valueColor: Color? = null,
    leading: String? = null,
    leadingTint: Color? = null,
    leadingMark: String? = null,
    onClick: () -> Unit,
) {
    val c = colors
    Row(
        Modifier.padding(horizontal = Gutter, vertical = 4.dp).fillMaxWidth().clip(CardShape).background(c.card)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            GlassIcon(leading, leadingTint ?: c.accent, size = 38.dp, mark = leadingMark)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.strong, color = c.ink)
            if (value.isNotBlank()) Text(value, style = Type.bodySmall, color = valueColor ?: c.mute)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.faint)
    }
}

@Composable
fun Toggle(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val c = colors
    Row(
        Modifier.padding(horizontal = Gutter, vertical = 4.dp).fillMaxWidth().clip(CardShape).background(c.card)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = Type.strong, color = if (enabled) c.ink else c.faint)
            Text(subtitle, style = Type.bodySmall, color = c.mute)
        }
        Switch(
            checked = checked, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.accent, checkedThumbColor = Color.White, checkedBorderColor = c.accent,
                uncheckedBorderColor = c.line, uncheckedThumbColor = c.faint, uncheckedTrackColor = c.surface,
            ),
        )
    }
}

/** Pilihan radio dengan judul dan penjelasan dampaknya. */
@Composable
fun Option(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val c = colors
    Row(
        Modifier.fillMaxWidth().clip(CardShape)
            .background(if (selected) c.surface else Color.Transparent)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = c.accent))
        Column(Modifier.padding(start = 6.dp)) {
            Text(title, style = Type.strong, color = c.ink)
            Text(subtitle, style = Type.bodySmall, color = c.mute)
        }
    }
}

@Composable
fun Bullet(text: String, color: Color = colors.mute) {
    Row(Modifier.padding(vertical = 5.dp)) {
        Text("•", style = Type.body, color = colors.faint, modifier = Modifier.padding(end = 10.dp))
        Text(text, style = Type.body, color = color)
    }
}

/**
 * Kotak penjelasan: "apa ini, dan apa dampaknya". Dipakai di setiap tempat yang bisa membingungkan.
 */
@Composable
fun InfoBox(title: String, lines: List<String>, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.accent.copy(alpha = 0.08f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, tint = c.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = Type.strong, color = c.ink)
        }
        Spacer(Modifier.height(6.dp))
        lines.forEach { Text(it, style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(vertical = 2.dp)) }
    }
}

/** Dialog isi nominal. [allowNegative] untuk saldo paylater / minus. */
@Composable
fun AmountDialog(
    title: String,
    body: String,
    initial: Long,
    onDismiss: () -> Unit,
    allowNegative: Boolean = false,
    confirmText: String = "Simpan",
    onConfirm: (Long) -> Unit,
) {
    val c = colors
    var negative by remember { mutableStateOf(initial < 0) }
    var draft by remember { mutableStateOf(if (initial != 0L) kotlin.math.abs(initial).toString() else "") }
    val value = (draft.toLongOrNull() ?: 0L) * if (negative) -1 else 1
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = Type.title) },
        text = {
            Column {
                if (body.isNotBlank()) Text(body, style = Type.body, color = c.mute)
                Spacer(Modifier.height(12.dp))
                LineField(draft, { v -> draft = v.filter(Char::isDigit).take(12) }, "0", keyboardType = KeyboardType.Number)
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Rupiah.format(value), style = Type.strong, color = if (value < 0) c.over else c.ink, modifier = Modifier.weight(1f))
                    if (allowNegative) TextAction(if (negative) "Jadikan plus" else "Jadikan minus", { negative = !negative })
                }
            }
        },
        confirmButton = { TextButton({ onConfirm(value) }) { Text(confirmText, color = c.ink) } },
        dismissButton = { TextButton(onDismiss) { Text("Batal", color = c.mute) } },
        containerColor = c.card,
    )
}

/** Pilihan ikon emoji dan warna untuk dompet / kategori / pos / target. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LookPicker(emoji: String, color: Int?, onEmoji: (String) -> Unit, onColor: (Int?) -> Unit, fallback: Color? = null) {
    val c = colors
    // Warna yang sebenarnya dipakai: pilihan pengguna, atau warna bawaan item ini.
    val current = color?.let { Color(it) } ?: fallback ?: c.accent
    Eyebrow("Ikon")
    Spacer(Modifier.height(8.dp))
    val tint = current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Hanya ikon yang punya versi garis, supaya semua ikon tetap seragam.
        Presets.emojis.distinctBy { glyphOf(it) }.filter { glyphOf(it) != null }.forEach { e ->
            val selected = glyphOf(e) == glyphOf(emoji)
            GlassIcon(
                e, if (selected) tint else c.mute, size = if (selected) 46.dp else 40.dp,
                modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).clickable { onEmoji(e) },
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Eyebrow("Warna")
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PocketPalette.forEach { col ->
            val argb = col.toArgb()
            val selected = current.toArgb() == argb
            androidx.compose.foundation.layout.Box(
                Modifier.size(34.dp).clip(CircleShape)
                    .border(2.dp, if (selected) c.ink else Color.Transparent, CircleShape)
                    .padding(4.dp).clip(CircleShape).background(col).clickable { onColor(argb) },
            )
        }
    }
}
