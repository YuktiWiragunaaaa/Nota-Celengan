package id.cukup.ui.pockets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.domain.PocketBalance
import id.cukup.domain.PocketKind
import id.cukup.domain.Rupiah
import id.cukup.ui.components.Bubble
import id.cukup.ui.components.BubbleChart
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.LightStatusBarIcons
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.LineField
import id.cukup.ui.components.Pill
import id.cukup.ui.components.PocketBadge
import id.cukup.ui.components.RoundIcon
import id.cukup.ui.components.SplitEditor
import id.cukup.ui.components.TopBar
import id.cukup.ui.components.UsageBar
import id.cukup.ui.components.dayLabel
import id.cukup.ui.components.label
import id.cukup.ui.components.localDate
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

/** Tab Kantong: gelembung besar, lalu kartu tiap kantong. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PocketsScreen(
    contentPadding: PaddingValues,
    onOpenPocket: (Long) -> Unit,
    onEditSplit: () -> Unit,
    onMove: () -> Unit,
    onOpenGoals: () -> Unit,
    vm: PocketsViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    val summary = s.summary ?: return
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        Modifier.fillMaxSize().background(c.brandBrush).verticalScroll(rememberScrollState()),
    ) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Kantong", style = Type.title, color = Color.White, modifier = Modifier.weight(1f))
                RoundIcon(Icons.Rounded.Flag, "Target tabungan", onOpenGoals, background = Color.White.copy(alpha = 0.18f), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                RoundIcon(Icons.Rounded.PieChart, "Ubah pembagian", onEditSplit, background = Color.White.copy(alpha = 0.18f), tint = Color.White)
            }
            BubbleChart(
                bubbles = summary.pockets.mapIndexed { i, pb -> pb.toBubble(c.of(pb.pocket, i)) },
                selected = selected,
                onSelect = { selected = if (selected == it) null else it },
                modifier = Modifier.fillMaxWidth().height(340.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(20.dp))
        }
        // Lembar putih.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(c.paper)
                .padding(top = 24.dp, bottom = contentPadding.calculateBottomPadding()),
        ) {
            Row(Modifier.padding(horizontal = Gutter), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Total uang", style = Type.bodySmall, color = c.mute)
                    Text(Rupiah.format(summary.total), style = Type.number.copy(fontSize = Type.number.fontSize * 1.3f), color = c.ink)
                }
                RoundIcon(Icons.Rounded.SwapVert, "Pindah uang", onMove)
            }
            Spacer(Modifier.height(16.dp))
            FlowRow(
                Modifier.padding(horizontal = Gutter),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2,
            ) {
                summary.pockets.forEachIndexed { i, pb ->
                    val color = c.of(pb.pocket, i)
                    val isSel = pb.pocket.id == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(CardShape)
                            .background(if (isSel) color.copy(alpha = 0.16f) else c.card)
                            .clickable { onOpenPocket(pb.pocket.id) }
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pb.pocket.name, style = Type.bodySmall, color = c.mute, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(pb.pocket.emoji)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(Rupiah.format(pb.balance), style = Type.amount, color = if (pb.balance < 0) c.over else c.ink, maxLines = 1)
                        Spacer(Modifier.height(10.dp))
                        if (pb.pocket.kind == PocketKind.SPEND) {
                            UsageBar(pb.usedRatio, color, height = 5.dp)
                        } else {
                            Text("${pb.pocket.percent}% · ${pb.pocket.kind.label().lowercase()}", style = Type.label, color = c.faint)
                        }
                    }
                }
                if (summary.pockets.size % 2 == 1) Spacer(Modifier.weight(1f))
            }
            InkButton(
                "Ubah pembagian",
                onClick = onEditSplit,
                icon = Icons.Rounded.PieChart,
                modifier = Modifier.padding(Gutter).fillMaxWidth(),
            )
        }
    }
}

internal fun PocketBalance.toBubble(color: Color): Bubble {
    val available = balance + spentThisCycle
    val fill = when {
        pocket.kind != PocketKind.SPEND -> -1f
        available <= 0 -> 0f
        else -> balance.coerceAtLeast(0).toFloat() / available
    }
    return Bubble(pocket.id, pocket.emoji, pocket.name, Rupiah.short(balance), balance.coerceAtLeast(0).toFloat().coerceAtLeast(1f), fill, color)
}

/** Layar mengubah pembagian persen. */
@Composable
fun SplitScreen(onBack: () -> Unit, vm: PocketsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    LaunchedEffect(Unit) { vm.startEditing() }
    val editing = s.editing

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        TopBar("Pembagian", onBack = onBack)
        if (editing == null) return@Column
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                "Berlaku untuk uang yang masuk setelah ini.",
                style = Type.bodySmall, color = c.mute,
                modifier = Modifier.padding(horizontal = Gutter),
            )
            SplitEditor(editing, onChange = vm::edit)
        }
        InkButton(
            "Simpan",
            onClick = { vm.save(onBack) },
            enabled = s.canSave,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
        )
    }
}

@Composable
fun PocketDetailScreen(
    onBack: () -> Unit,
    onOpenTx: (Long) -> Unit,
    onAdd: (Long) -> Unit,
    vm: PocketDetailViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    val pb = s.balance
    var editBalance by remember { mutableStateOf(false) }
    if (pb == null) {
        Box(Modifier.fillMaxSize().background(c.paper))
        return
    }
    val color = c.of(pb.pocket, s.index)
    LazyColumn(Modifier.fillMaxSize().background(c.paper)) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Brush.verticalGradient(listOf(color.darken(0.55f), color)))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(bottom = 40.dp),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onBack, background = Color.White.copy(alpha = 0.2f), tint = Color.White)
                }
                Text(pb.pocket.emoji, style = Type.display, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(pb.pocket.name, style = Type.strong, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(Rupiah.format(pb.balance), style = Type.hero, color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 1)
                Spacer(Modifier.height(10.dp))
                Text(
                    "${pb.pocket.percent}% dari tiap uang masuk · ${pb.pocket.kind.label().lowercase()}",
                    style = Type.label, color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally).clip(Pill).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        item {
            Row(Modifier.padding(horizontal = Gutter).offset(y = (-26).dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LineButton("Atur saldo", onClick = { editBalance = true }, icon = Icons.Rounded.Edit, modifier = Modifier.weight(1f))
                if (pb.pocket.kind != PocketKind.SAVE) {
                    InkButton("Keluar", onClick = { onAdd(pb.pocket.id) }, icon = Icons.Rounded.ArrowUpward, modifier = Modifier.weight(1f))
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = Gutter).fillMaxWidth()) {
                Stat("Masuk periode ini", pb.inThisCycle, Modifier.weight(1f))
                Stat("Keluar periode ini", pb.outThisCycle, Modifier.weight(1f))
            }
            Text("Riwayat", style = Type.title, color = c.ink, modifier = Modifier.padding(horizontal = Gutter).padding(top = 24.dp, bottom = 8.dp))
        }
        if (s.entries.isEmpty()) {
            item { Text("Belum ada catatan di kantong ini.", style = Type.body, color = c.faint, modifier = Modifier.padding(horizontal = Gutter)) }
        }
        items(s.entries, key = { it.tx.id }) { e ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpenTx(e.tx.id) }.padding(horizontal = Gutter, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PocketBadge(if (e.amount >= 0) "💰" else pb.pocket.emoji, if (e.amount >= 0) c.good else color, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.label, style = Type.strong, color = c.ink, maxLines = 1)
                    Text(
                        buildString {
                            append(dayLabel(localDate(e.tx.occurredAt)))
                            if (e.percent != null) append(" · ${e.percent}% dari ${Rupiah.short(e.tx.amount)}")
                            if (e.tx.isPaylater) append(" · paylater")
                        },
                        style = Type.bodySmall, color = c.faint, maxLines = 1,
                    )
                }
                Text((if (e.amount > 0) "+" else "") + Rupiah.format(e.amount), style = Type.amount, color = if (e.amount > 0) c.good else c.ink)
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }

    if (editBalance) {
        var draft by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { editBalance = false },
            title = { Text("Atur saldo ${pb.pocket.name}", style = Type.title) },
            text = {
                Column {
                    Text("Sekarang ${Rupiah.format(pb.balance)}. Isi jumlah yang benar.", style = Type.body, color = c.mute)
                    Spacer(Modifier.height(12.dp))
                    LineField(draft, { v -> draft = v.filter(Char::isDigit).take(12) }, "Misalnya 250000", keyboardType = KeyboardType.Number)
                    if (draft.isNotEmpty()) Text(Rupiah.format(draft.toLong()), style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Kosongkan kantong",
                        style = Type.strong, color = c.over,
                        modifier = Modifier.clip(Pill).clickable { vm.setBalance(0); editBalance = false }.padding(vertical = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton({ draft.toLongOrNull()?.let(vm::setBalance); editBalance = false }, enabled = draft.isNotEmpty()) { Text("Simpan", color = c.ink) }
            },
            dismissButton = { TextButton({ editBalance = false }) { Text("Batal", color = c.mute) } },
            containerColor = c.card,
        )
    }
}

private fun Color.darken(f: Float) = Color(red * f, green * f, blue * f, alpha)

@Composable
private fun Stat(label: String, amount: Long, modifier: Modifier = Modifier) {
    val c = colors
    Column(modifier) {
        Text(label, style = Type.bodySmall, color = c.mute)
        Text(Rupiah.format(amount), style = Type.amount, color = c.ink)
    }
}
