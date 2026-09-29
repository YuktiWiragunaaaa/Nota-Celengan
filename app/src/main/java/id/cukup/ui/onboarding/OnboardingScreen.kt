package id.cukup.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.domain.Presets
import id.cukup.domain.Rupiah
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.Keypad
import id.cukup.ui.components.LineField
import id.cukup.ui.components.PocketDot
import id.cukup.ui.components.PocketEditor
import id.cukup.ui.components.PocketRing
import id.cukup.ui.components.SplitPreview
import id.cukup.ui.components.TextAction
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    BackHandler(enabled = s.step.ordinal in 1..4) { vm.onIntent(OnboardingIntent.Back) }

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        // Penanda langkah: garis tipis yang terisi.
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OnboardingStep.entries.forEach { step ->
                Box(Modifier.weight(1f).height(2.dp).background(if (step.ordinal <= s.step.ordinal) c.ink else c.line))
            }
        }
        AnimatedContent(
            targetState = s.step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "step",
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> Welcome(s.name) { vm.onIntent(OnboardingIntent.Name(it)) }
                OnboardingStep.PAYDAY -> Payday(s.payday) { vm.onIntent(OnboardingIntent.Payday(it)) }
                OnboardingStep.PRESET -> PresetStep(s.presetId) { vm.onIntent(OnboardingIntent.ChoosePreset(it)) }
                OnboardingStep.POCKETS -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    Heading("Sesuaikan <i>posmu</i>.", "Ganti nama, emoji, dan persentase. Total harus 100%.")
                    PocketEditor(s.pockets, onChange = { vm.onIntent(OnboardingIntent.EditPockets(it)) })
                }
                OnboardingStep.START -> StartStep(s)  { vm.onIntent(OnboardingIntent.StartAmount(it)) }
                OnboardingStep.NOTIF -> NotifStep()
            }
        }
        Hairline()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (s.step.ordinal in 1..4) {
                TextAction("Kembali", onClick = { vm.onIntent(OnboardingIntent.Back) }, color = c.mute)
            }
            Spacer(Modifier.weight(1f))
            when (s.step) {
                OnboardingStep.START -> InkButton(
                    if (s.startAmount > 0) "Bagi & lanjut" else "Lewati",
                    onClick = { vm.onIntent(OnboardingIntent.Next) },
                    enabled = !s.saving,
                    modifier = Modifier.padding(start = 12.dp).fillMaxWidth(0.6f),
                )
                OnboardingStep.NOTIF -> InkButton("Mulai", onClick = { vm.complete(onDone) }, modifier = Modifier.fillMaxWidth(0.6f))
                else -> InkButton(
                    "Lanjut",
                    onClick = { vm.onIntent(OnboardingIntent.Next) },
                    enabled = s.canContinue,
                    modifier = Modifier.fillMaxWidth(0.6f),
                )
            }
        }
    }
}

/** Judul serif dengan satu frasa italic: tulis frasa di antara <i>…</i>. */
@Composable
internal fun Heading(title: String, body: String?) {
    val c = colors
    Column(Modifier.padding(horizontal = Gutter).padding(top = 24.dp, bottom = 16.dp)) {
        Text(italicize(title), style = Type.display, color = c.ink)
        if (body != null) {
            Spacer(Modifier.height(10.dp))
            Text(body, style = Type.body, color = c.mute)
        }
    }
}

internal fun italicize(s: String) = androidx.compose.ui.text.buildAnnotatedString {
    val parts = s.split("<i>", "</i>")
    parts.forEachIndexed { i, part ->
        if (i % 2 == 1) {
            pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
            append(part)
            pop()
        } else {
            append(part)
        }
    }
}

@Composable
private fun Welcome(name: String, onName: (String) -> Unit) {
    val c = colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.padding(horizontal = Gutter).size(120.dp)) {
            PocketRing(
                listOf(
                    Triple(c.pocket(0), 0.5f, 0.7f),
                    Triple(c.pocket(1), 0.3f, 0.5f),
                    Triple(c.pocket(2), 0.2f, 1f),
                ),
                Modifier.fillMaxSize(),
            )
        }
        Heading("Uangmu, <i>dipilah.</i>", "Setiap uang masuk dibagi ke pos-pos yang kamu tentukan. Kamu selalu tahu masih cukup atau tidak.")
        Column(Modifier.padding(horizontal = Gutter)) {
            Eyebrow("Panggil kamu siapa?")
            Spacer(Modifier.height(10.dp))
            LineField(name, onName, "Nama panggilan")
            Spacer(Modifier.height(24.dp))
            Text(
                "Semua data tersimpan di HP ini saja. Tanpa akun, tanpa server, tanpa iklan.",
                style = Type.bodySmall,
                color = c.faint,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Payday(selected: Int, onSelect: (Int) -> Unit) {
    val c = colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading("Kapan kamu <i>gajian?</i>", "Siklus anggaran dimulai di tanggal ini. Kalau tidak tentu, pilih tanggal kamu biasanya menerima uang.")
        FlowRow(
            Modifier.padding(horizontal = Gutter),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            maxItemsInEachRow = 7,
        ) {
            for (d in 1..31) {
                val on = d == selected
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (on) c.ink else c.paper)
                        .border(1.dp, if (on) c.ink else c.line, RoundedCornerShape(2.dp))
                        .clickable(role = Role.RadioButton) { onSelect(d) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$d", style = Type.body, color = if (on) c.paper else c.ink)
                }
            }
            // Isi sisa baris agar kolom tetap rata.
            repeat(4) { Spacer(Modifier.weight(1f).height(44.dp)) }
        }
        Text(
            "Tanggal 29–31 otomatis menyesuaikan di bulan yang lebih pendek.",
            style = Type.bodySmall, color = c.faint,
            modifier = Modifier.padding(horizontal = Gutter, vertical = 16.dp),
        )
    }
}

@Composable
private fun PresetStep(selected: String, onSelect: (String) -> Unit) {
    val c = colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading("Mulai dari <i>mana?</i>", "Pilih titik awal. Semua pos dan persentase bisa diubah kapan saja.")
        Presets.all.forEach { preset ->
            val on = preset.id == selected
            Column(
                Modifier
                    .padding(horizontal = Gutter, vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .border(if (on) 1.5.dp else 1.dp, if (on) c.ink else c.line, RoundedCornerShape(2.dp))
                    .clickable(role = Role.RadioButton) { onSelect(preset.id) }
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(preset.title, style = Type.title, color = c.ink, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.size(18.dp).clip(CircleShape).border(1.dp, c.ink, CircleShape).padding(4.dp)
                            .clip(CircleShape).background(if (on) c.ink else c.paper),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(preset.subtitle, style = Type.bodySmall, color = c.mute)
                Spacer(Modifier.height(14.dp))
                // Batang proporsi pos.
                Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    preset.pockets.forEachIndexed { i, p ->
                        Box(Modifier.weight(p.percent.toFloat()).height(6.dp).background(c.pocket(i)))
                    }
                }
                Spacer(Modifier.height(10.dp))
                preset.pockets.forEachIndexed { i, p ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        PocketDot(c.pocket(i), size = 6.dp)
                        Text("  ${p.name}", style = Type.bodySmall, color = c.ink, modifier = Modifier.weight(1f))
                        Text("${p.percent}%", style = Type.bodySmall, color = c.mute)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StartStep(s: OnboardingState, onAmount: (Long) -> Unit) {
    val c = colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading("Berapa uangmu <i>sekarang?</i>", "Total di dompet, rekening, dan e-wallet. Akan langsung dibagi ke pos. Boleh dilewati.")
        Text(
            Rupiah.format(s.startAmount),
            style = Type.hero,
            color = if (s.startAmount > 0) c.ink else c.faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp),
        )
        SplitPreview(s.startAmount, s.previewPockets, Modifier.padding(vertical = 8.dp))
        Keypad(s.startAmount, onAmount, Modifier.padding(horizontal = Gutter, vertical = 12.dp))
    }
}

@Composable
private fun NotifStep() {
    val c = colors
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading("Biar Cukup yang <i>mencatat.</i>", null)
        Column(Modifier.padding(horizontal = Gutter)) {
            Text(
                "Cukup bisa membaca notifikasi transaksi dari e-wallet, m-banking, dan paylater " +
                    "(GoPay, OVO, DANA, ShopeePay, BCA, BRImo, Livin', Jago, Kredivo, dan lainnya).",
                style = Type.body, color = c.ink,
            )
            Spacer(Modifier.height(16.dp))
            Bullet("Hanya notifikasi dari aplikasi keuangan yang dikenal. Chat dan lainnya diabaikan.")
            Bullet("Yang disimpan hanya nominal, nama toko, dan jenisnya. Bukan isi notifikasi.")
            Bullet("Semua tetap di HP ini. Cukup tidak punya akses internet.")
            Bullet("Transaksi masuk ke \"Perlu dicek\" dulu, kamu yang memastikan.")
            Spacer(Modifier.height(20.dp))
            InkButton(
                "Buka pengaturan akses",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Pilih \"Cukup\" lalu izinkan. Bisa juga nanti dari Setelan.",
                style = Type.bodySmall, color = c.faint,
            )
        }
    }
}

@Composable
internal fun Bullet(text: String) {
    val c = colors
    Row(Modifier.padding(vertical = 6.dp)) {
        Text("—", style = Type.body, color = c.faint, modifier = Modifier.padding(end = 10.dp))
        Text(text, style = Type.body, color = c.mute)
    }
}
