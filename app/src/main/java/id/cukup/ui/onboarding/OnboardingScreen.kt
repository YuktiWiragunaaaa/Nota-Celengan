package id.cukup.ui.onboarding

import id.cukup.ui.components.GlassIcon
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.domain.AccountKind
import id.cukup.domain.Presets
import id.cukup.domain.Rupiah
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.Choice
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.InfoBox
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.LineField
import id.cukup.ui.components.ScheduleEditor
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.italicize
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    val canGoBack = s.step.ordinal > 0
    BackHandler(enabled = canGoBack) { vm.onIntent(OnboardingIntent.Back) }

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Langkah ${s.step.ordinal + 1} dari ${OnboardingStep.entries.size}", style = Type.bodySmall, color = c.mute, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OnboardingStep.entries.forEach { step ->
                    Box(Modifier.width(18.dp).height(3.dp).background(if (step.ordinal <= s.step.ordinal) c.ink else c.line))
                }
            }
        }
        AnimatedContent(s.step, transitionSpec = { fadeIn() togetherWith fadeOut() }, modifier = Modifier.weight(1f), label = "step") { step ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                when (step) {
                    OnboardingStep.NAME -> {
                        NameStep(s.name) { vm.onIntent(OnboardingIntent.Name(it)) }
                        RestoreEntry(onDone)
                    }
                    OnboardingStep.WALLETS -> WalletsStep(s, vm::onIntent)
                    OnboardingStep.SCHEDULE -> {
                        Heading("Biasanya <i>gajian</i> kapan?", "Bisa gaji, uang saku, atau hasil jualan. Ini menentukan awal dan akhir tiap periode (\"bulan ini\" / \"minggu ini\").")
                        ScheduleEditor(s.schedule, { vm.onIntent(OnboardingIntent.SetSchedule(it)) })
                    }
                    OnboardingStep.PLAN -> PlanStep(s, vm::onIntent)
                    OnboardingStep.PERMISSIONS -> PermissionsStep()
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        Hairline()
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (canGoBack) TextAction("Kembali", onClick = { vm.onIntent(OnboardingIntent.Back) }, color = c.mute)
            Spacer(Modifier.weight(1f))
            if (s.step == OnboardingStep.PERMISSIONS) {
                InkButton("Mulai pakai", onClick = { vm.complete(onDone) }, enabled = !s.saving, modifier = Modifier.fillMaxWidth(0.6f))
            } else {
                InkButton("Lanjut", onClick = { vm.onIntent(OnboardingIntent.Next) }, enabled = s.canContinue, modifier = Modifier.fillMaxWidth(0.6f))
            }
        }
    }
}

/** Pindah HP atau habis hapus data: langsung pulihkan dari file cadangan, lewati pengenalan. */
@Composable
private fun RestoreEntry(onDone: () -> Unit, app: id.cukup.ui.AppViewModel = hiltViewModel()) {
    val c = colors
    val message by app.backupMessage.collectAsStateWithLifecycle()
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) app.restoreBackup(uri, onDone) }
    Column(
        Modifier.padding(horizontal = Gutter).padding(top = 24.dp).fillMaxWidth().clip(CardShape).background(c.card)
            .clickable { pick.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }.padding(14.dp),
    ) {
        Text("Punya file cadangan Cukup?", style = Type.strong, color = c.ink)
        Text("Ketuk untuk memulihkan semua data dan langsung mulai.", style = Type.bodySmall, color = c.mute)
        message?.takeIf { it.startsWith("Gagal") }?.let { Text(it, style = Type.bodySmall, color = c.over) }
    }
}

@Composable
private fun Heading(title: String, body: String?) {
    val c = colors
    Column(Modifier.padding(horizontal = Gutter).padding(top = 12.dp, bottom = 20.dp)) {
        Text(italicize(title), style = Type.display, color = c.ink)
        if (body != null) {
            Spacer(Modifier.height(10.dp))
            Text(body, style = Type.body, color = c.mute)
        }
    }
}

@Composable
private fun NameStep(name: String, onName: (String) -> Unit) {
    val c = colors
    Heading("Hai! Ini <i>Cukup</i>.", "Aplikasi untuk tahu uangmu ada di mana, habis ke mana, dan masih cukup atau tidak.")
    Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).clip(CardShape).background(c.card).padding(14.dp)) {
            Eyebrow("CATATAN", color = c.accent)
            Text("Uang sungguhan: dompet, saldo, masuk & keluar.", style = Type.bodySmall, color = c.mute)
        }
        Column(Modifier.weight(1f).clip(CardShape).background(c.card).padding(14.dp)) {
            Eyebrow("RENCANA", color = c.accent)
            Text("Batas yang kamu buat sendiri. Tidak mengubah saldo.", style = Type.bodySmall, color = c.mute)
        }
    }
    Spacer(Modifier.height(24.dp))
    Column(Modifier.padding(horizontal = Gutter)) {
        Eyebrow("Panggil kamu siapa?")
        Spacer(Modifier.height(8.dp))
        LineField(name, onName, "Nama panggilan")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletsStep(s: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val c = colors
    Heading("Uangmu ada <i>di mana</i> saja?", "Pilih semua tempat uangmu, lalu isi saldonya sekarang. Nanti bisa ditambah atau diubah.")
    FlowRow(
        Modifier.padding(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Presets.accounts.forEach { seed ->
            Choice(seed.name, s.accounts.any { it.name == seed.name }, { onIntent(OnboardingIntent.Toggle(seed.name)) })
        }
    }
    Spacer(Modifier.height(20.dp))
    s.accounts.forEach { a ->
        Column(Modifier.padding(horizontal = Gutter, vertical = 6.dp).fillMaxWidth().clip(CardShape).background(c.card).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIcon(a.emoji, Presets.accounts.firstOrNull { it.name == a.name }?.color?.let { androidx.compose.ui.graphics.Color(it) } ?: c.accent, size = 32.dp, mark = id.cukup.domain.Brands.forAccountName(a.name)?.mark)
                Spacer(Modifier.width(10.dp))
                Text(a.name, style = Type.strong, color = c.ink)
            }
            Text(if (a.kind == AccountKind.PAYLATER) "Hutang sekarang" else "Saldo sekarang", style = Type.label, color = c.mute, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
            LineField(
                if (a.amount > 0) a.amount.toString() else "",
                { v -> onIntent(OnboardingIntent.Amount(a.key, v.filter(Char::isDigit).take(12).toLongOrNull() ?: 0)) },
                "0", keyboardType = KeyboardType.Number,
            )
            if (a.amount > 0) Text(Rupiah.format(a.amount), style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 4.dp))
        }
    }
    val total = s.accounts.filter { it.kind != AccountKind.PAYLATER }.sumOf { it.amount }
    Text(
        "Total uangmu: ${Rupiah.format(total)}",
        style = Type.title, color = c.ink, modifier = Modifier.padding(horizontal = Gutter).padding(top = 12.dp),
    )
    Text(
        "Nggak tahu pasti? Kira-kira dulu. Nanti bisa disamakan lewat \"Samakan saldo\" di tiap dompet.",
        style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(horizontal = Gutter, vertical = 6.dp),
    )
}

@Composable
private fun PlanStep(s: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val c = colors
    Heading("Mau dibantu <i>jaga belanja</i>?", "Opsional. Rencana membagi gajimu jadi batas-batas belanja. Uangnya tetap di dompet; Cukup hanya mengingatkan kalau hampir lewat.")
    Option("Nanti saja", "Catat dulu, atur rencana kapan-kapan di tab Rencana.", s.preset == null) { onIntent(OnboardingIntent.Preset(null)) }
    Presets.plans.forEach { p ->
        Option(p.title, p.pos.joinToString(" · ") { "${it.name} ${it.percent}%" }, s.preset?.id == p.id) { onIntent(OnboardingIntent.Preset(p)) }
    }
    if (s.preset != null) {
        Column(Modifier.padding(horizontal = Gutter).padding(top = 16.dp)) {
            Eyebrow("Kira-kira uang masukmu tiap ${s.schedule.periodName.removeSuffix(" ini").ifBlank { "periode" }}?")
            Spacer(Modifier.height(8.dp))
            LineField(
                if (s.income > 0) s.income.toString() else "",
                { v -> onIntent(OnboardingIntent.Income(v.filter(Char::isDigit).take(12).toLongOrNull() ?: 0)) },
                "Misalnya 4000000", keyboardType = KeyboardType.Number,
            )
            if (s.income > 0) {
                Spacer(Modifier.height(10.dp))
                val parts = id.cukup.domain.Planner.split(
                    s.income,
                    s.preset.pos.mapIndexed { i, p -> id.cukup.domain.PlanPos(i.toLong() + 1, p.name, p.emoji, p.percent, p.kind, i) },
                )
                InfoBox(
                    "Hasilnya",
                    s.preset.pos.mapIndexed { i, p ->
                        val v = parts.getOrNull(i)?.second ?: 0
                        if (p.kind == id.cukup.domain.PlanKind.SAVE) "${p.name}: sisihkan ${Rupiah.format(v)}"
                        else "${p.name}: belanja maksimal ${Rupiah.format(v)}"
                    },
                )
            }
        }
    }
}

@Composable
private fun Option(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val c = colors
    Column(
        Modifier.padding(horizontal = Gutter, vertical = 5.dp).fillMaxWidth().clip(CardShape)
            .background(if (selected) c.accent else c.card).clickable(onClick = onClick).padding(16.dp),
    ) {
        Text(title, style = Type.strong, color = if (selected) c.onAccent else c.ink)
        Text(subtitle, style = Type.bodySmall, color = if (selected) c.onAccent.copy(alpha = 0.8f) else c.mute)
    }
}

@Composable
private fun PermissionsStep() {
    val c = colors
    val context = LocalContext.current
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Heading("Terakhir, dua izin", "Keduanya opsional dan bisa diubah di Setelan.")
    Column(Modifier.padding(horizontal = Gutter)) {
        Text("1. Peringatan belanja", style = Type.strong, color = c.ink)
        Text("Supaya bisa diingatkan kalau belanja sudah 80% dari rencana, atau satu kali belanja terlalu besar.", style = Type.body, color = c.mute)
        Spacer(Modifier.height(10.dp))
        LineButton("Izinkan notifikasi", onClick = { if (Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS) }, height = 44.dp)
        Spacer(Modifier.height(28.dp))
        Text("2. Catat otomatis dari e-wallet & m-banking", style = Type.strong, color = c.ink)
        Text(
            "Pembayaran lewat GoPay, OVO, DANA, ShopeePay, BCA, BRImo, dan lainnya terbaca dari notifikasi, lalu masuk \"Perlu dicek\". " +
                "Yang disimpan cuma nominal dan nama tokonya.",
            style = Type.body, color = c.mute,
        )
        Spacer(Modifier.height(10.dp))
        LineButton("Buka pengaturan akses", onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, height = 44.dp)
        Spacer(Modifier.height(6.dp))
        Text("Cari Cukup di daftar, lalu nyalakan.", style = Type.bodySmall, color = c.faint)
    }
}
