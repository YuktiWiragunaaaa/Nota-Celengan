package id.cukup.ui.help

import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import id.cukup.domain.Rupiah
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.cardSurface
import id.cukup.ui.components.Choice
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.GlassIcon
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.InkButton
import id.cukup.ui.components.Pill
import id.cukup.ui.components.TopBar
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

private class Page(val eyebrow: String, val title: String, val body: String, val demo: @Composable () -> Unit)

/** Panduan interaktif: kartu geser, tiap kartu punya simulasi yang bisa dicoba. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val c = colors
    val pages = remember {
        listOf(
            Page("Mulai dari sini", "Kalau begini, harus bagaimana?", "Soal yang paling sering muncul saat dipakai sehari-hari. Ketuk untuk lihat langkahnya.") { ScenarioDemo() },
            Page("1 · Dua bagian", "Catatan vs Rencana", "Catatan = uang sungguhan. Rencana = batas yang kamu buat sendiri. Ketuk untuk bandingkan.") { SidesDemo() },
            Page("2 · Tiga tombol", "Keluar, Masuk, Pindah", "Coba ketuk tombolnya dan lihat saldo dompet berubah. Pindah tidak mengubah total.") { ButtonsDemo() },
            Page("3 · Rencana", "Bagi gaji otomatis", "Geser gajimu. Cukup membagi ke pos 50/30/20 tanpa memindahkan uang. Tidak suka persen? Tiap pos juga bisa diisi nominal bebas per hari, minggu, atau gajian.") { PlanDemo() },
            Page("4 · Aman per hari", "Berapa boleh jajan hari ini?", "Sisa batas belanja dibagi sisa hari sampai gajian. Geser dan lihat angkanya.") { DailyDemo() },
            Page("5 · Catat otomatis", "Notifikasi jadi catatan", "Transfer dan bayar dari bank/e-wallet langsung tercatat ke dompet yang benar.") { NotifDemo() },
            Page("6 · Serba cepat", "Trik biar makin praktis", "Ketuk tiap trik untuk lihat caranya.") { TipsDemo() },
        )
    }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        TopBar("Cara pakai Cukup", onBack)
        // Penanda halaman seperti story.
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pages.indices.forEach { i ->
                val on by animateColorAsState(if (i <= pager.currentPage) c.accent else c.line, label = "dot")
                Box(Modifier.weight(1f).height(4.dp).clip(Pill).background(on))
            }
        }
        HorizontalPager(pager, Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Gutter), pageSpacing = 12.dp) { i ->
            val p = pages[i]
            val focus by animateFloatAsState(if (pager.currentPage == i) 1f else 0.94f, spring(stiffness = 300f), label = "focus")
            Column(
                // Kartu setinggi isinya (tidak ada ruang kosong besar), tetap bisa digulir kalau panjang.
                Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = false).padding(vertical = 8.dp)
                    .graphicsLayer { scaleX = focus; scaleY = focus }
                    .cardSurface().border(1.dp, c.line, CardShape)
                    .verticalScroll(rememberScrollState()).padding(20.dp),
            ) {
                Eyebrow(p.eyebrow, color = c.accent)
                Spacer(Modifier.height(6.dp))
                Text(p.title, style = Type.display, color = c.ink)
                Spacer(Modifier.height(8.dp))
                Text(p.body, style = Type.body, color = c.mute)
                Spacer(Modifier.height(20.dp))
                p.demo()
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage > 0) {
                Text(
                    "Kembali", style = Type.strong, color = c.mute,
                    modifier = Modifier.clip(Pill).clickable { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }.padding(12.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            val last = pager.currentPage == pages.lastIndex
            InkButton(
                if (last) "Siap pakai" else "Lanjut",
                onClick = { if (last) onBack() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                modifier = Modifier.width(160.dp),
            )
        }
    }
}

@Composable
private fun SidesDemo() {
    val c = colors
    var rencana by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice("Catatan", !rencana, { rencana = false })
        Choice("Rencana", rencana, { rencana = true })
    }
    Spacer(Modifier.height(14.dp))
    val items = if (!rencana) {
        listOf("💵" to "Dompet & saldonya", "📈" to "Uang masuk & keluar", "🏦" to "Pindah antar dompet", "✨" to "Mengubah saldo")
    } else {
        listOf("🏠" to "Pembagian 50/30/20", "🛍️" to "Batas belanja", "🌱" to "Target tabungan", "🛟" to "Tidak mengubah saldo")
    }
    items.forEachIndexed { i, (e, t) ->
        var shown by remember(rencana) { mutableStateOf(false) }
        LaunchedEffect(rencana) { delay(70L * i); shown = true }
        AnimatedVisibility(shown, enter = fadeIn() + slideInVertically { it / 2 }) {
            Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIcon(e, if (rencana) c.pocket(i) else c.accent, size = 38.dp)
                Spacer(Modifier.width(12.dp))
                Text(t, style = Type.strong, color = c.ink)
            }
        }
    }
}

@Composable
private fun ButtonsDemo() {
    val c = colors
    var bca by remember { mutableLongStateOf(500_000) }
    var tunai by remember { mutableLongStateOf(100_000) }
    var last by remember { mutableStateOf("Ketuk salah satu tombol.") }
    val total = bca + tunai
    Wallet("🏦", "BCA", bca, Color(0xFF005EB8))
    Wallet("💵", "Tunai", tunai, c.good)
    Row(Modifier.padding(vertical = 10.dp)) {
        Text("Total", style = Type.strong, color = c.mute, modifier = Modifier.weight(1f))
        Text(Rupiah.format(total), style = Type.amount, color = c.ink)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice("Keluar 25 rb", false, { if (tunai >= 25_000) { tunai -= 25_000; last = "Tunai berkurang 25 rb. Total ikut turun." } else last = "Tunai tidak cukup." })
        Choice("Masuk 100 rb", false, { bca += 100_000; last = "BCA bertambah 100 rb. Total ikut naik." })
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Choice("Pindah 50 rb BCA → Tunai", false, {
            if (bca >= 50_000) { bca -= 50_000; tunai += 50_000; last = "Uang pindah dompet. Total tetap sama." } else last = "Saldo BCA tidak cukup."
        })
        Choice("Ulang", false, { bca = 500_000; tunai = 100_000; last = "Kembali ke awal." })
    }
    Spacer(Modifier.height(12.dp))
    Text(last, style = Type.bodySmall, color = c.accent)
}

@Composable
private fun Wallet(emoji: String, name: String, amount: Long, tint: Color) {
    val c = colors
    val shown by animateFloatAsState(amount.toFloat(), spring(stiffness = 120f), label = "saldo")
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassIcon(emoji, tint, size = 38.dp, mark = if (name == "BCA") "BCA" else null)
        Spacer(Modifier.width(12.dp))
        Text(name, style = Type.strong, color = c.ink, modifier = Modifier.weight(1f))
        Text(Rupiah.format(shown.roundToLong()), style = Type.amount, color = c.ink)
    }
}

@Composable
private fun PlanDemo() {
    val c = colors
    var gaji by remember { mutableFloatStateOf(4_000_000f) }
    val step = (gaji / 100_000).roundToLong() * 100_000
    Text(Rupiah.format(step), style = Type.number, color = c.ink)
    Slider(
        gaji, { gaji = it }, valueRange = 1_000_000f..15_000_000f,
        colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.line),
    )
    listOf(Triple("🏠", "Kebutuhan", 50), Triple("☕", "Keinginan", 30), Triple("🌱", "Tabungan", 20)).forEachIndexed { i, (e, n, pct) ->
        val part = step * pct / 100
        val w by animateFloatAsState(pct / 50f, spring(stiffness = 200f), label = "w")
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon(e, c.pocket(i), size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text("$n $pct%", style = Type.strong, color = c.ink, modifier = Modifier.weight(1f))
                    Text(Rupiah.short(part), style = Type.amount, color = c.ink)
                }
                Box(Modifier.padding(top = 4.dp).fillMaxWidth(w.coerceIn(0f, 1f)).height(6.dp).clip(Pill).background(c.pocket(i)))
            }
        }
    }
    Text("Uangnya tetap di dompet. Cukup hanya mengingatkan kalau pos hampir habis.", style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun DailyDemo() {
    val c = colors
    var sisa by remember { mutableFloatStateOf(1_500_000f) }
    var hari by remember { mutableFloatStateOf(15f) }
    val perDay = (sisa / hari.coerceAtLeast(1f)).roundToLong()
    val tone by animateColorAsState(if (perDay < 50_000) c.over else if (perDay < 100_000) c.caution else c.good, label = "tone")
    val size by animateDpAsState(if (perDay < 50_000) 150.dp else 170.dp, label = "size")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape).background(tone.copy(alpha = 0.15f)).border(2.dp, tone, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Rupiah.short(perDay), style = Type.number, color = tone)
                Text("aman/hari", style = Type.label, color = c.mute)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Sisa batas belanja: ${Rupiah.short((sisa / 50_000).roundToLong() * 50_000)}", style = Type.bodySmall, color = c.mute)
    Slider(sisa, { sisa = it }, valueRange = 100_000f..4_000_000f, colors = SliderDefaults.colors(thumbColor = tone, activeTrackColor = tone, inactiveTrackColor = c.line))
    Text("Sisa hari sampai gajian: ${hari.toInt()}", style = Type.bodySmall, color = c.mute)
    Slider(hari, { hari = it }, valueRange = 1f..30f, steps = 28, colors = SliderDefaults.colors(thumbColor = tone, activeTrackColor = tone, inactiveTrackColor = c.line))
}

@Composable
private fun NotifDemo() {
    val c = colors
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(step) {
        if (step in 1..2) { delay(900); step++ }
    }
    // Notifikasi contoh.
    Column(Modifier.fillMaxWidth().clip(CardShape).background(c.surface).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIcon("🏦", Color(0xFF005EB8), size = 30.dp, mark = "BCA")
            Spacer(Modifier.width(10.dp))
            Text("myBCA · sekarang", style = Type.label, color = c.mute)
        }
        Spacer(Modifier.height(6.dp))
        Text("Pengeluaran sebesar IDR 45,000.00 ke GOFOOD", style = Type.bodySmall, color = c.ink)
    }
    Spacer(Modifier.height(10.dp))
    val labels = listOf("Dibaca: Rp45.000 keluar", "Dompet: BCA (dari nama aplikasi)", "Kategori: Makan (dari GOFOOD)")
    labels.forEachIndexed { i, l ->
        AnimatedVisibility(step > i, enter = fadeIn() + expandVertically()) {
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(Pill).background(c.good))
                Spacer(Modifier.width(10.dp))
                Text(l, style = Type.bodySmall, color = c.ink)
            }
        }
    }
    AnimatedVisibility(step >= 3, enter = fadeIn() + slideInVertically { it }) {
        Row(Modifier.padding(top = 10.dp).fillMaxWidth().clip(CardShape).background(c.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlassIcon("🍜", c.pocket(0), size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("GOFOOD", style = Type.strong, color = c.ink)
                Text("BCA · tercatat otomatis", style = Type.bodySmall, color = c.mute)
            }
            Text("−Rp45.000", style = Type.amount, color = c.ink)
        }
    }
    Spacer(Modifier.height(12.dp))
    Choice(if (step == 0) "▶  Coba" else "Ulang", false, { step = if (step == 0) 1 else 0 })
    Text(
        "Transfer ke dompetmu sendiri (mis. BCA → Krom) otomatis jadi Pindah, bukan pengeluaran. Nyalakan di Setelan › Baca notifikasi.",
        style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun TipsDemo() {
    val c = colors
    val tips = listOf(
        Triple("⚡", "Catat cepat", "Di Beranda, ketuk ikon kategori → isi nominal → Simpan. Urutannya menyesuaikan kebiasaanmu."),
        Triple("📱", "Tombol di widget", "Belanja yang sering diulang (mis. ☕ 18 rb) jadi tombol sekali ketuk di widget, tanpa buka aplikasi."),
        Triple("💾", "Cadangan", "Setelan › Simpan cadangan ke Drive. Ganti HP? Pulihkan di layar pertama."),
        Triple("🏦", "Dompet bermerek", "Beri nama dompet sesuai bank (BCA, Krom, OVO) supaya notifikasinya masuk ke dompet yang tepat."),
        Triple("🛟", "Kunci PIN", "Setelan › Kunci pakai PIN. Sidik jari juga bisa."),
    )
    ExpandList(tips)
}

/** Soal yang paling sering muncul saat dipakai sehari-hari, dan langkah persisnya. */
@Composable
private fun ScenarioDemo() {
    ExpandList(
        listOf(
            Triple("🔔", "Transfer tidak tercatat sendiri", "Biasanya karena HP menutup Cukup diam-diam. Buka Setelan › Baca notifikasi: di sana terlihat statusnya, tiga langkah supaya tidak terulang, dan daftar notifikasi terakhir beserta alasan kalau ada yang dilewati."),
            Triple("⚖️", "Saldo beda dengan bank", "Ketuk angka besar di Beranda › \"Samakan\" di dompetnya › isi saldo yang benar. Selisihnya dicatat sebagai penyesuaian, riwayat lain tidak berubah."),
            Triple("🔁", "Uangnya cuma pindah dompet", "Tarik tunai, top up e-wallet, atau transfer ke rekening sendiri = Pindah (tombol ⇄), bukan Keluar. Total uangmu tidak berubah."),
            Triple("💸", "Penghasilanku tidak tetap", "Rencana › Ubah › pilih \"Nominal bebas\" dan ketik batasnya, misalnya Makan Rp300 rb per minggu. Mau tetap pakai persen? Ketuk \"Uang yang dibagi\" › \"Uang masuk periode lalu\": yang masuk kemarin, itu yang dibagi sekarang."),
            Triple("⏰", "Mau diingatkan sebelum kebablasan", "Tiap pos mengingatkan saat 80% batasnya terpakai dan saat lewat, di jangkanya sendiri (hari, minggu, atau gajian). Nyalakan di Setelan › Beri tahu kalau belanja kebanyakan."),
            Triple("🎨", "Ganti warna atau ikon kategori", "Tekan-tahan bagian grafiknya (donat, batang, atau gelembung). Warnanya bisa dipilih bebas."),
        ),
    )
}

@Composable
private fun ExpandList(tips: List<Triple<String, String, String>>) {
    val c = colors
    var open by remember { mutableStateOf<Int?>(null) }
    tips.forEachIndexed { i, (e, t, d) ->
        Column(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(CardShape)
                .background(if (open == i) c.surface else Color.Transparent)
                .clickable { open = if (open == i) null else i }.padding(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIcon(e, c.pocket(i + 2), size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Text(t, style = Type.strong, color = c.ink, modifier = Modifier.weight(1f))
                Text(if (open == i) "−" else "+", style = Type.title, color = c.accent)
            }
            AnimatedVisibility(open == i) {
                Text(d, style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 8.dp, start = 48.dp))
            }
        }
    }
}
