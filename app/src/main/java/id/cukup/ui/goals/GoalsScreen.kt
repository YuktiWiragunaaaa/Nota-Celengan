package id.cukup.ui.goals

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.domain.GoalProgress
import id.cukup.domain.PocketBalance
import id.cukup.domain.PocketKind
import id.cukup.domain.Rupiah
import id.cukup.domain.Templates
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.LineField
import id.cukup.ui.components.Pill
import id.cukup.ui.components.TopBar
import id.cukup.ui.theme.PocketPalette
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Goal(val pb: PocketBalance, val index: Int, val progress: GoalProgress)

data class GoalsState(
    val loaded: Boolean = false,
    val goals: List<Goal> = emptyList(),
    /** Kantong tabungan yang belum punya target. */
    val candidates: List<PocketBalance> = emptyList(),
    val periodName: String = "minggu",
)

@HiltViewModel
class GoalsViewModel @Inject constructor(private val repository: MoneyRepository) : ViewModel() {

    val state: StateFlow<GoalsState> = combine(repository.summary, repository.settings) { summary, settings ->
        val basis = maxOf(summary.incomeThisCycle, summary.basisIncome)
        val goals = summary.pockets.mapIndexedNotNull { i, pb ->
            val t = pb.pocket.target ?: return@mapIndexedNotNull null
            Goal(pb, i, GoalProgress.of(pb.balance.coerceAtLeast(0), t, basis * pb.pocket.percent / 100))
        }
        GoalsState(
            loaded = true,
            goals = goals,
            candidates = summary.pockets.filter { it.pocket.target == null && it.pocket.kind == PocketKind.SAVE },
            periodName = when (settings.schedule.frequency) {
                id.cukup.domain.Frequency.WEEKLY -> "minggu"
                id.cukup.domain.Frequency.BIWEEKLY -> "x gajian"
                id.cukup.domain.Frequency.MONTHLY -> "bulan"
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsState())

    fun setTarget(pocketId: Long, target: Long?) {
        viewModelScope.launch { repository.setTarget(pocketId, target) }
    }

    fun create(name: String, emoji: String, color: Int, target: Long) {
        viewModelScope.launch { repository.createGoal(name, emoji, color, target) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(onBack: () -> Unit, onFill: (Long) -> Unit, onEditSplit: () -> Unit, vm: GoalsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Goal?>(null) }

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        TopBar("Target tabungan", onBack = onBack)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (s.loaded && s.goals.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(Gutter), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎯", fontSize = 56.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Belum ada target", style = Type.title, color = c.ink)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Mau nabung buat apa? Liburan, HP baru, dana darurat. Pasang target, nanti kelihatan kapan tercapai.",
                            style = Type.body, color = c.mute, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            items(s.goals, key = { it.pb.pocket.id }) { g ->
                GoalCard(g, s.periodName, onClick = { editing = g }, onFill = { onFill(g.pb.pocket.id) })
            }
        }
        InkButton(
            "Tambah target",
            onClick = { adding = true },
            icon = Icons.Rounded.Add,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
        )
    }

    if (adding) {
        ModalBottomSheet({ adding = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.card) {
            NewGoal(s.candidates, onExisting = { id, t -> vm.setTarget(id, t); adding = false }) { name, emoji, color, t ->
                vm.create(name, emoji, color, t)
                adding = false
            }
        }
    }
    editing?.let { g ->
        ModalBottomSheet({ editing = null }, containerColor = c.card) {
            var draft by remember { mutableStateOf(g.progress.target.toString()) }
            Column(Modifier.padding(horizontal = Gutter).padding(bottom = 28.dp)) {
                Text("${g.pb.pocket.emoji}  ${g.pb.pocket.name}", style = Type.title, color = c.ink)
                Spacer(Modifier.height(16.dp))
                Text("Target", style = Type.bodySmall, color = c.mute)
                Spacer(Modifier.height(6.dp))
                LineField(draft, { v -> draft = v.filter(Char::isDigit).take(12) }, "Misalnya 3000000", keyboardType = KeyboardType.Number)
                if (draft.isNotEmpty()) Text(Rupiah.format(draft.toLong()), style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 6.dp))
                if (g.pb.pocket.percent == 0) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Kantong ini belum dapat bagian dari uang masuk. Atur persennya supaya terisi otomatis.",
                        style = Type.bodySmall, color = c.caution,
                    )
                    LineButton("Atur pembagian", onClick = { editing = null; onEditSplit() }, height = 44.dp, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LineButton("Hapus target", onClick = { vm.setTarget(g.pb.pocket.id, null); editing = null }, modifier = Modifier.weight(1f))
                    InkButton("Simpan", onClick = { vm.setTarget(g.pb.pocket.id, draft.toLongOrNull()); editing = null }, enabled = (draft.toLongOrNull() ?: 0) > 0, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GoalCard(g: Goal, periodName: String, onClick: () -> Unit, onFill: () -> Unit) {
    val c = colors
    val color = c.of(g.pb.pocket, g.index)
    val p = g.progress
    val ring by animateFloatAsState(p.ratio, tween(900), label = "goal")
    Row(
        Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(CardShape).background(c.card).clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            val track = c.line
            Canvas(Modifier.fillMaxSize()) {
                val w = 9.dp.toPx()
                val d = size.minDimension - w
                val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
                drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(w))
                drawArc(color, -90f, 360f * ring, false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
            }
            if (p.reached) Celebrate() else Text("${(p.ratio * 100).toInt()}%", style = Type.amount, color = c.ink)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("${g.pb.pocket.emoji}  ${g.pb.pocket.name}", style = Type.strong, color = c.ink, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text("${Rupiah.short(p.saved)} dari ${Rupiah.short(p.target)}", style = Type.title, color = c.ink)
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    p.reached -> "Tercapai! Keren."
                    p.periodsLeft != null -> "Kurang ${Rupiah.short(p.remaining)} · kira-kira ${p.periodsLeft} $periodName lagi"
                    else -> "Kurang ${Rupiah.short(p.remaining)}"
                },
                style = Type.bodySmall, color = if (p.reached) c.good else c.mute,
            )
            if (!p.reached) {
                Text(
                    "Isi sekarang",
                    style = Type.label, color = color,
                    modifier = Modifier.padding(top = 8.dp).clip(Pill).background(color.copy(alpha = 0.14f)).clickable(onClick = onFill)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Perayaan kecil saat target tercapai. */
@Composable
private fun Celebrate() {
    val t = rememberInfiniteTransition(label = "yay")
    val s by t.animateFloat(0.9f, 1.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "s")
    Text("🎉", fontSize = 30.sp, modifier = Modifier.scale(s))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewGoal(
    candidates: List<PocketBalance>,
    onExisting: (Long, Long) -> Unit,
    onCreate: (String, String, Int, Long) -> Unit,
) {
    val c = colors
    val ideas = remember { Templates.all.filter { it.kind == PocketKind.SAVE } + Templates.all.filter { it.name in setOf("Gadget", "Fashion", "Pendidikan", "Hadiah") } }
    var picked by remember { mutableStateOf<Pair<String, String>?>(null) }
    var existing by remember { mutableStateOf<Long?>(null) }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    Column(Modifier.padding(horizontal = Gutter).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
        Text("Nabung buat apa?", style = Type.title, color = c.ink)
        Spacer(Modifier.height(14.dp))
        if (candidates.isNotEmpty()) {
            Text("Kantong yang sudah ada", style = Type.bodySmall, color = c.mute)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                candidates.forEach { pb ->
                    val on = existing == pb.pocket.id
                    Text(
                        "${pb.pocket.emoji} ${pb.pocket.name}",
                        style = Type.bodySmall, color = if (on) c.card else c.ink,
                        modifier = Modifier.clip(Pill).background(if (on) c.ink else c.surface)
                            .clickable { existing = pb.pocket.id; picked = null; name = pb.pocket.name }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Atau buat baru", style = Type.bodySmall, color = c.mute)
            Spacer(Modifier.height(8.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4) {
            ideas.forEach { t ->
                val on = picked?.first == t.name
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) c.ink else c.surface)
                        .clickable { picked = t.name to t.emoji; existing = null; name = t.name }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(t.emoji, fontSize = 22.sp)
                    Text(t.name, style = Type.label, color = if (on) c.card else c.ink, maxLines = 1)
                }
            }
        }
        if (picked != null) {
            Spacer(Modifier.height(14.dp))
            LineField(name, { name = it.take(24) }, "Nama target (mis. Liburan Bali)")
        }
        Spacer(Modifier.height(14.dp))
        Text("Targetnya berapa?", style = Type.bodySmall, color = c.mute)
        Spacer(Modifier.height(6.dp))
        LineField(amount, { v -> amount = v.filter(Char::isDigit).take(12) }, "Misalnya 3000000", keyboardType = KeyboardType.Number)
        if (amount.isNotEmpty()) Text(Rupiah.format(amount.toLong()), style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(18.dp))
        val target = amount.toLongOrNull() ?: 0
        InkButton(
            "Pasang target",
            enabled = target > 0 && (existing != null || (picked != null && name.isNotBlank())),
            onClick = {
                val e = existing
                val p = picked
                if (e != null) onExisting(e, target)
                else if (p != null) onCreate(name.trim(), p.second, PocketPalette[(name.hashCode() and 0x7fffffff) % PocketPalette.size].toArgb(), target)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (picked != null) {
            Text(
                "Kantong baru dibuat dengan 0%. Atur persennya di Pembagian supaya terisi otomatis, atau isi manual.",
                style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
