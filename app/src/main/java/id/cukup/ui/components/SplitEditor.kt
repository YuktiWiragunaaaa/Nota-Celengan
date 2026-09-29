package id.cukup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.cukup.domain.Pocket
import id.cukup.domain.PocketKind
import id.cukup.domain.PocketTag
import id.cukup.domain.PresetPocket
import id.cukup.domain.Templates
import id.cukup.ui.theme.PocketPalette
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
    val color: Int? = null,
    val target: Long? = null,
) {
    fun toPocket(order: Int) = Pocket(id, name.trim().ifBlank { "Kantong" }, emoji, percent, kind, tag, order, color, target)

    companion object {
        fun from(p: Pocket, index: Int) = EditablePocket(index.toLong() + 1, p.id, p.name, p.emoji, p.percent, p.kind, p.tag, p.color, p.target)
    }
}

/** Kata sehari-hari untuk jenis kantong. */
fun PocketKind.label(): String = when (this) {
    PocketKind.SPEND -> "Untuk belanja"
    PocketKind.SAVE -> "Ditabung"
    PocketKind.DEBT -> "Bayar hutang"
}

/** Batang yang menunjukkan pembagian persen secara visual. */
@Composable
fun SplitBar(percents: List<Int>, modifier: Modifier = Modifier, barColors: List<Color>? = null) {
    val c = colors
    val total = percents.sum()
    Row(modifier.fillMaxWidth().height(12.dp).clip(Pill).background(c.line), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        percents.forEachIndexed { i, p ->
            if (p > 0) Box(Modifier.weight(p.toFloat()).height(12.dp).background(barColors?.getOrNull(i) ?: c.pocket(i)))
        }
        if (total < 100) Box(Modifier.weight((100 - total).toFloat()).height(12.dp))
    }
}

/**
 * Editor pembagian: tiap kantong punya ikon, nama, warna, persen (penggeser) dan jenis. Total harus 100%.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitEditor(
    pockets: List<EditablePocket>,
    onChange: (List<EditablePocket>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = colors
    val total = pockets.sumOf { it.percent }
    var editing by remember { mutableStateOf<Long?>(null) }
    var adding by remember { mutableStateOf(false) }
    fun colorOf(p: EditablePocket, i: Int) = p.color?.let { Color(it) } ?: c.pocket(i)

    Column(modifier) {
        Column(Modifier.padding(horizontal = Gutter, vertical = 12.dp)) {
            SplitBar(pockets.map { it.percent }, barColors = pockets.mapIndexed { i, p -> colorOf(p, i) })
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$total%", style = Type.number, color = if (total == 100) c.ink else c.over)
                Spacer(Modifier.width(10.dp))
                Text(
                    when {
                        total == 100 -> "Pas"
                        total < 100 -> "Kurang ${100 - total}%"
                        else -> "Lebih ${total - 100}%"
                    },
                    style = Type.bodySmall, color = if (total == 100) c.good else c.over, modifier = Modifier.weight(1f),
                )
                if (total != 100 && pockets.isNotEmpty()) {
                    TextAction("Pasin otomatis", onClick = {
                        val diff = 100 - total
                        val list = pockets.toMutableList()
                        val i = list.indices.reversed().firstOrNull { (list[it].percent + diff) in 0..100 } ?: return@TextAction
                        list[i] = list[i].copy(percent = list[i].percent + diff)
                        onChange(list)
                    }, color = c.accent)
                }
            }
        }
        pockets.forEachIndexed { index, p ->
            PocketEditCard(
                pocket = p,
                color = colorOf(p, index),
                canRemove = pockets.size > 1,
                onChange = { updated -> onChange(pockets.toMutableList().also { it[index] = updated }) },
                onRemove = { onChange(pockets.toMutableList().also { it.removeAt(index) }) },
                onEditLook = { editing = p.key },
            )
        }
        LineButton(
            "Tambah kantong",
            onClick = { adding = true },
            icon = Icons.Rounded.Add,
            modifier = Modifier.padding(horizontal = Gutter, vertical = 12.dp).fillMaxWidth(),
        )
    }

    // Panel ubah tampilan kantong: ikon & warna.
    val target = pockets.firstOrNull { it.key == editing }
    if (target != null) {
        val index = pockets.indexOf(target)
        ModalBottomSheet(onDismissRequest = { editing = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.card) {
            LookPicker(
                emoji = target.emoji,
                color = colorOf(target, index),
                onEmoji = { e -> onChange(pockets.toMutableList().also { it[index] = target.copy(emoji = e) }) },
                onColor = { col -> onChange(pockets.toMutableList().also { it[index] = target.copy(color = col.toArgb()) }) },
                onDone = { editing = null },
            )
        }
    }

    // Panel tambah kantong dari kategori.
    if (adding) {
        ModalBottomSheet(onDismissRequest = { adding = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.card) {
            CategoryPicker(existing = pockets.map { it.name }) { t ->
                val key = (pockets.maxOfOrNull { it.key } ?: 0) + 1
                // Kunci warna kantong yang ada (supaya tidak berubah saat urutan berubah), lalu pilih warna yang belum dipakai.
                val fixed = pockets.mapIndexed { i, p -> p.copy(color = colorOf(p, i).toArgb()) }
                val used = fixed.mapNotNull { it.color }.toSet()
                val nextColor = PocketPalette.firstOrNull { it.toArgb() !in used } ?: PocketPalette[pockets.size % PocketPalette.size]
                onChange(fixed + EditablePocket(key, 0, t.name, t.emoji, (100 - total).coerceIn(0, 100), t.kind, t.tag, nextColor.toArgb()))
                adding = false
            }
        }
    }
}

@Composable
private fun PocketEditCard(
    pocket: EditablePocket,
    color: Color,
    canRemove: Boolean,
    onChange: (EditablePocket) -> Unit,
    onRemove: () -> Unit,
    onEditLook: () -> Unit,
) {
    val c = colors
    Column(
        Modifier.padding(horizontal = Gutter, vertical = 5.dp).fillMaxWidth().clip(CardShape).background(c.card).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.18f))
                    .border(2.dp, color, CircleShape)
                    .clickable(onClick = onEditLook)
                    .semantics { contentDescription = "Ganti ikon dan warna" },
                contentAlignment = Alignment.Center,
            ) { Text(pocket.emoji, fontSize = 20.sp) }
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
                    Modifier.padding(start = 4.dp).size(36.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onRemove)
                        .semantics { contentDescription = "Hapus kantong" },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, null, tint = c.faint, modifier = Modifier.size(18.dp)) }
            }
        }
        Slider(
            value = pocket.percent.toFloat(),
            onValueChange = { onChange(pocket.copy(percent = (it / 5f).roundToInt() * 5)) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = color,
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
                    style = Type.label,
                    color = if (on) c.card else c.mute,
                    modifier = Modifier
                        .clip(Pill)
                        .background(if (on) c.ink else c.surface)
                        .clickable(role = Role.RadioButton) { onChange(pocket.copy(kind = k)) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LookPicker(emoji: String, color: Color, onEmoji: (String) -> Unit, onColor: (Color) -> Unit, onDone: () -> Unit) {
    val c = colors
    Column(Modifier.padding(horizontal = Gutter).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)).border(2.dp, color, CircleShape), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(14.dp))
            Text("Tampilan kantong", style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
            RoundIcon(Icons.Rounded.Check, "Selesai", onDone, background = c.ink, tint = c.card)
        }
        Spacer(Modifier.height(20.dp))
        Eyebrow("Warna")
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PocketPalette.forEach { col ->
                val on = col.toArgb() == color.toArgb()
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(col)
                        .border(if (on) 3.dp else 0.dp, if (on) c.ink else Color.Transparent, CircleShape)
                        .clickable(role = Role.RadioButton) { onColor(col) },
                    contentAlignment = Alignment.Center,
                ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Eyebrow("Ikon")
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxItemsInEachRow = 8) {
            Templates.emojis.forEach { e ->
                val on = e == emoji
                Box(
                    Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (on) color.copy(alpha = 0.25f) else c.surface)
                        .clickable { onEmoji(e) },
                    contentAlignment = Alignment.Center,
                ) { Text(e, fontSize = 20.sp) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(existing: List<String>, onPick: (PresetPocket) -> Unit) {
    val c = colors
    Column(Modifier.padding(horizontal = Gutter).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
        Text("Pilih kategori", style = Type.title, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text("Nama dan ikonnya bisa diubah nanti.", style = Type.bodySmall, color = c.mute)
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4) {
            Templates.all.forEach { t ->
                val taken = existing.any { it.equals(t.name, ignoreCase = true) }
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(c.surface)
                        .clickable(enabled = !taken) { onPick(t) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(t.emoji, fontSize = 22.sp, color = if (taken) c.faint else Color.Unspecified)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t.name, style = Type.label, color = if (taken) c.faint else c.ink,
                        textAlign = TextAlign.Center, maxLines = 2, minLines = 2,
                    )
                }
            }
        }
    }
}
