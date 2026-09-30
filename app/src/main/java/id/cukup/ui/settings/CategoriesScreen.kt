package id.cukup.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.domain.Category
import id.cukup.domain.CategoryKind
import id.cukup.domain.Tag
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.Choice
import id.cukup.ui.components.Group
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.LineField
import id.cukup.ui.components.Link
import id.cukup.ui.components.LookPicker
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TopBar
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

@Composable
fun CategoriesScreen(onBack: () -> Unit, vm: AppViewModel = hiltViewModel()) {
    val o by vm.overview.collectAsStateWithLifecycle()
    val c = colors
    val data = o ?: return
    var editing by remember { mutableStateOf<Category?>(null) }
    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        TopBar("Kategori", onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                "Kategori hanya label supaya tahu uang habis ke mana. Mengubahnya tidak mengubah saldo dompet.",
                style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(horizontal = Gutter),
            )
            listOf(CategoryKind.EXPENSE to "Uang keluar", CategoryKind.INCOME to "Uang masuk").forEach { (kind, title) ->
                Group(title)
                data.categories.filter { it.kind == kind }.forEach { cat ->
                    val pos = data.plan.firstOrNull { it.id == cat.planId }
                    Link(cat.name, if (kind == CategoryKind.EXPENSE) (pos?.let { "Rencana: ${it.name}" } ?: "Tidak masuk rencana") else "", leading = cat.emoji) {
                        editing = cat
                    }
                }
                TextAction("+ Tambah kategori", { editing = Category(0, "", "✨", kind, Tag.OTHER) }, Modifier.padding(horizontal = Gutter, vertical = 6.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    editing?.let { cat ->
        var name by remember(cat) { mutableStateOf(cat.name) }
        var emoji by remember(cat) { mutableStateOf(cat.emoji) }
        var color by remember(cat) { mutableStateOf(cat.color) }
        var kind by remember(cat) { mutableStateOf(cat.kind) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (cat.id == 0L) "Kategori baru" else "Ubah kategori", style = Type.title) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LineField(name, { name = it.take(24) }, "Nama kategori")
                    if (cat.id == 0L) {
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Choice("Uang keluar", kind == CategoryKind.EXPENSE, { kind = CategoryKind.EXPENSE })
                            Choice("Uang masuk", kind == CategoryKind.INCOME, { kind = CategoryKind.INCOME })
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    LookPicker(emoji, color, { emoji = it }, { color = it })
                    if (cat.id != 0L) {
                        Spacer(Modifier.height(14.dp))
                        TextAction("Hapus kategori ini", { vm.archiveCategory(cat.id); editing = null }, color = c.over)
                        Text("Catatan lama tetap ada dengan nama kategori ini.", style = Type.label, color = c.faint)
                    }
                }
            },
            confirmButton = {
                TextButton({ vm.saveCategory(cat.copy(name = name.trim(), emoji = emoji, color = color, kind = kind)); editing = null }, enabled = name.isNotBlank()) {
                    Text("Simpan", color = c.ink)
                }
            },
            dismissButton = { TextButton({ editing = null }) { Text("Batal", color = c.mute) } },
            containerColor = c.card,
        )
    }
}
