package id.cukup.ui.onboarding

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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import id.cukup.ui.components.LineButton
import id.cukup.ui.components.LineField
import id.cukup.ui.components.ScheduleEditor
import id.cukup.ui.components.SplitBar
import id.cukup.ui.components.SplitEditor
import id.cukup.ui.components.SplitPreview
import id.cukup.ui.components.TextAction
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = colors
    val canGoBack = s.step.ordinal in 1..OnboardingStep.START.ordinal
    BackHandler(enabled = canGoBack) { vm.onIntent(OnboardingIntent.Back) }

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Langkah ${s.step.ordinal + 1} dari ${OnboardingStep.entries.size}",
                style = Type.bodySmall, color = c.mute, modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OnboardingStep.entries.forEach { step ->
                    Box(Modifier.width(18.dp).height(3.dp).background(if (step.ordinal <= s.step.ordinal) c.ink else c.line))
                }
            }
        }
        AnimatedContent(
            targetState = s.step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "step",
        ) { step ->
            when (step) {
                OnboardingStep.NAME -> NameStep(s.name) { vm.onIntent(OnboardingIntent.Name(it)) }
                OnboardingStep.SCHEDULE -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Heading("Biasanya gajian kapan?", "Bisa gaji, uang saku, atau hasil jualan. Jatah belanjamu dihitung dari hari ini sampai gajian berikutnya.")
                    ScheduleEditor(s.schedule, { vm.onIntent(OnboardingIntent.SetSchedule(it)) })
                    Spacer(Modifier.height(24.dp))
                }
                OnboardingStep.SPLIT -> SplitStep(s, vm::onIntent)
                OnboardingStep.START -> StartStep(s) { vm.onIntent(OnboardingIntent.StartAmount(it)) }
                OnboardingStep.PERMISSIONS -> PermissionsStep()
            }
        }
        Hairline()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canGoBack) TextAction("Kembali", onClick = { vm.onIntent(OnboardingIntent.Back) }, color = c.mute)
            Spacer(Modifier.weight(1f))
            when (s.step) {
                OnboardingStep.START -> InkButton(
                    if (s.startAmount > 0) "Simpan & lanjut" else "Lewati",
                    onClick = { vm.onIntent(OnboardingIntent.Next) },
                    enabled = s.canContinue,
                    modifier = Modifier.fillMaxWidth(0.6f),
                )
                OnboardingStep.PERMISSIONS -> InkButton("Selesai", onClick = { vm.complete(onDone) }, modifier = Modifier.fillMaxWidth(0.6f))
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
    Column(Modifier.padding(horizontal = Gutter).padding(top = 12.dp, bottom = 20.dp)) {
        Text(italicize(title), style = Type.display, color = c.ink)
        if (body != null) {
            Spacer(Modifier.height(10.dp))
            Text(body, style = Type.body, color = c.mute)
        }
    }
}

internal fun italicize(s: String) = androidx.compose.ui.text.buildAnnotatedString {
    s.split("<i>", "</i>").forEachIndexed { i, part ->
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
private fun NameStep(name: String, onName: (String) -> Unit) {
    val c = colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading(
            "Halo, kenalan dulu yuk.",
            "Tiap kamu terima uang, Cukup langsung membaginya. Misalnya separuh buat belanja, separuh ditabung. " +
                "Kalau belanjamu mulai kebanyakan, nanti dikasih tahu.",
        )
        Column(Modifier.padding(horizontal = Gutter)) {
            Eyebrow("Nama panggilanmu")
            Spacer(Modifier.height(10.dp))
            LineField(name, onName, "Misalnya: Yukti")
            Spacer(Modifier.height(20.dp))
            Text("Semua datamu cuma disimpan di HP ini.", style = Type.bodySmall, color = c.faint)
        }
    }
}

@Composable
private fun SplitStep(s: OnboardingState, onIntent: (OnboardingIntent) -> Unit) {
    val c = colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading("Uangnya mau dibagi gimana?", "Pilih salah satu dulu. Persennya bisa kamu geser sendiri, kapan saja.")
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Presets.all.forEach { p ->
                val on = p.id == s.presetId
                Column(
                    Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .border(if (on) 1.5.dp else 1.dp, if (on) c.ink else c.line, RoundedCornerShape(2.dp))
                        .clickable(role = Role.RadioButton) { onIntent(OnboardingIntent.ChoosePreset(p.id)) }
                        .padding(12.dp),
                ) {
                    Text(p.title, style = Type.strong, color = c.ink)
                    Spacer(Modifier.height(8.dp))
                    SplitBar(p.pockets.map { it.percent })
                    Spacer(Modifier.height(8.dp))
                    Text(p.subtitle, style = Type.bodySmall, color = c.mute, minLines = 2, maxLines = 3)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SplitEditor(s.pockets, onChange = { onIntent(OnboardingIntent.EditPockets(it)) })
    }
}

@Composable
private fun StartStep(s: OnboardingState, onAmount: (Long) -> Unit) {
    val c = colors
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Heading("Sekarang pegang uang berapa?", "Gabungan uang di dompet, rekening, dan e-wallet. Kalau malas ngitung, lewati saja.")
            Text(
                Rupiah.format(s.startAmount),
                style = Type.hero,
                color = if (s.startAmount > 0) c.ink else c.faint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter),
            )
            SplitPreview(s.startAmount, s.previewPockets, Modifier.padding(vertical = 8.dp))
        }
        Keypad(s.startAmount, onAmount, Modifier.padding(horizontal = Gutter, vertical = 8.dp), keyHeight = 50.dp)
    }
}

@Composable
private fun PermissionsStep() {
    val c = colors
    val context = LocalContext.current
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading("Terakhir, dua izin", null)
        Column(Modifier.padding(horizontal = Gutter)) {
            Text("1. Peringatan belanja", style = Type.strong, color = c.ink)
            Text(
                "Biar bisa diingatkan kalau belanjamu sudah 80% dari jatah, atau sudah lewat.",
                style = Type.body, color = c.mute,
            )
            Spacer(Modifier.height(10.dp))
            LineButton(
                "Izinkan notifikasi",
                onClick = { if (Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS) },
                height = 44.dp,
            )
            Spacer(Modifier.height(28.dp))
            Text("2. Catat otomatis dari e-wallet & m-banking", style = Type.strong, color = c.ink)
            Text(
                "Biar belanja pakai GoPay, OVO, DANA, ShopeePay, BCA, BRImo, dan lainnya tercatat sendiri. " +
                    "Yang disimpan cuma nominal dan nama tokonya. Belanja pakai uang tunai tetap dicatat sendiri ya.",
                style = Type.body, color = c.mute,
            )
            Spacer(Modifier.height(10.dp))
            LineButton(
                "Buka pengaturan akses",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                height = 44.dp,
            )
            Spacer(Modifier.height(6.dp))
            Text("Cari Cukup di daftar, lalu nyalakan.", style = Type.bodySmall, color = c.faint)
        }
        Spacer(Modifier.height(24.dp))
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
