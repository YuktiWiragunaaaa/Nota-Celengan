package id.cukup.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.cukup.domain.Category
import id.cukup.domain.Rupiah
import id.cukup.domain.Transaction
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

private enum class Stage { MENU, LOOK, MOVE }

/**
 * Tekan-tahan sebuah kategori di grafik. Dua pilihan saja: ubah tampilannya, atau pindahkan catatannya
 * ke kategori lain. [category] null = "Tanpa kategori" (tidak punya tampilan, jadi langsung ke pindahkan).
 * [txs] = catatan kategori ini yang sedang tampil di grafik; [choices] = kategori sejenis.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryActions(
    category: Category?,
    txs: List<Transaction>,
    choices: List<Category>,
    onDismiss: () -> Unit,
    onSaveCategory: (Category) -> Unit,
    onMove: (Transaction, Long) -> Unit,
) {
    val c = colors
    var stage by remember { mutableStateOf(if (category == null) Stage.MOVE else Stage.MENU) }
    val name = category?.name ?: "Tanpa kategori"

    if (stage == Stage.LOOK && category != null) {
        CategoryLookDialog(category, onDismiss = onDismiss) { onSaveCategory(it); onDismiss() }
        return
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.card) {
        Row(Modifier.padding(horizontal = Gutter).padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(category?.emoji ?: "🧾", colorOf(category), size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, style = Type.title, color = c.ink)
                Text("${txs.size} catatan · ${Rupiah.format(txs.sumOf { it.amount })}", style = Type.bodySmall, color = c.mute)
            }
        }
        if (stage == Stage.MENU) {
            Action("Ubah tampilan", "Ikon dan warna kategori ini") { stage = Stage.LOOK }
            Hairline(Modifier.padding(horizontal = Gutter))
            Action("Pindahkan catatan", "Ganti kategori catatan di sini") { stage = Stage.MOVE }
            Spacer(Modifier.padding(bottom = 24.dp))
            return@ModalBottomSheet
        }
        // Satu catatan terbuka sekaligus; pilihannya kategori lain yang sejenis.
        var open by remember { mutableStateOf<Long?>(null) }
        val targets = choices.filter { it.id != category?.id }
        if (txs.isEmpty()) {
            Text("Tidak ada catatan di sini.", style = Type.body, color = c.faint, modifier = Modifier.padding(horizontal = Gutter).padding(bottom = 32.dp))
            return@ModalBottomSheet
        }
        Text("Ketuk catatan, lalu pilih kategorinya.", style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(horizontal = Gutter).padding(bottom = 6.dp))
        LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            items(txs, key = { it.id }) { tx ->
                val title = tx.merchant.ifBlank { tx.note }.ifBlank { "Tanpa keterangan" }
                // Catatan lain dari tempat yang sama ikut pindah, supaya tidak perlu diulang satu per satu.
                val same = if (tx.merchant.isBlank()) emptyList() else txs.filter { it.id != tx.id && it.merchant.equals(tx.merchant, ignoreCase = true) }
                Column(Modifier.fillMaxWidth().clickable { open = if (open == tx.id) null else tx.id }.padding(horizontal = Gutter, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = Type.strong, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(dayLabel(localDate(tx.occurredAt)), style = Type.bodySmall, color = c.faint)
                        }
                        Text(Rupiah.format(tx.amount), style = Type.amount, color = c.ink)
                    }
                    if (open == tx.id) {
                        if (same.isNotEmpty()) {
                            Text("${same.size} catatan lain dari tempat ini ikut pindah.", style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 6.dp))
                        }
                        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            targets.forEach { t ->
                                Row(
                                    Modifier.clip(Pill).cardSurface(Pill).clickable {
                                        (listOf(tx) + same).forEach { onMove(it, t.id) }
                                        open = null
                                    }.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    GlassIcon(t.emoji, colorOf(t), size = 26.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(t.name, style = Type.bodySmall, color = c.ink)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Action(title: String, body: String, onClick: () -> Unit) {
    val c = colors
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Gutter, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.strong, color = c.ink)
            Text(body, style = Type.bodySmall, color = c.mute)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.faint)
    }
}
