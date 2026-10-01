package id.cukup.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import id.cukup.data.NoticeLog
import id.cukup.notif.MoneyNotificationListener
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Link
import id.cukup.ui.components.SectionHeader
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.TopBar
import id.cukup.ui.components.dayLabel
import id.cukup.ui.components.localDate
import id.cukup.ui.components.time
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

/** "6 jam", "40 menit", "2 hari". */
fun span(millis: Long): String {
    val minutes = millis / 60_000
    return when {
        minutes < 90 -> "$minutes menit"
        minutes < 48 * 60 -> "${minutes / 60} jam"
        else -> "${minutes / (24 * 60)} hari"
    }
}

private fun batteryFree(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

private fun open(context: Context, vararg intents: Intent) {
    for (i in intents) {
        if (runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
    }
}

private fun appDetails(context: Context) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

/** Layar izin "Mulai otomatis" di HP Xiaomi/Redmi/POCO; di merek lain jatuh ke info aplikasi. */
private fun autostart(context: Context) = open(
    context,
    Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
    appDetails(context),
)

/**
 * Kesehatan catat otomatis + notifikasi keuangan terakhir dan apa yang Cukup lakukan dengannya.
 * Tujuannya: kalau ada transfer yang tidak tercatat, alasannya terlihat di sini.
 */
@Composable
fun AutoScreen(onBack: () -> Unit, onOpenInbox: () -> Unit, vm: AppViewModel = hiltViewModel()) {
    val c = colors
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val n by vm.notices.collectAsStateWithLifecycle()
    var allowed by remember { mutableStateOf(MoneyNotificationListener.isEnabled(context)) }
    var battery by remember { mutableStateOf(batteryFree(context)) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            allowed = MoneyNotificationListener.isEnabled(context)
            battery = batteryFree(context)
        }
    }
    val xiaomi = remember { android.os.Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco") }

    LazyColumn(Modifier.fillMaxSize().background(c.paper).systemBarsPadding()) {
        item { TopBar("Catat otomatis", onBack) }
        item {
            val ok = allowed && n.connected
            Row(
                Modifier.padding(horizontal = Gutter).fillMaxWidth().clip(CardShape)
                    .background((if (ok) c.good else c.caution).copy(alpha = 0.14f)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Dot(if (ok) c.good else c.caution)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        when {
                            !allowed -> "Belum nyala"
                            !n.connected -> "Menyambung…"
                            else -> "Aktif, sedang membaca notifikasi"
                        },
                        style = Type.strong, color = c.ink,
                    )
                    Text(
                        if (allowed) "Notifikasi bank & e-wallet dibaca saat masuk, lalu dicatat ke dompetnya."
                        else "Cukup belum diizinkan membaca notifikasi, jadi tidak ada yang tercatat sendiri.",
                        style = Type.bodySmall, color = c.mute,
                    )
                }
            }
        }
        n.gap?.let { gap ->
            item {
                Column(
                    Modifier.padding(horizontal = Gutter).padding(top = 10.dp).fillMaxWidth().clip(CardShape)
                        .background(c.over.copy(alpha = 0.12f)).padding(16.dp),
                ) {
                    Text("Sempat mati ${span(gap.to - gap.from)}", style = Type.strong, color = c.ink)
                    Text(
                        "Dari ${dayLabel(localDate(gap.from))} ${time(gap.from)} sampai ${dayLabel(localDate(gap.to))} ${time(gap.to)} " +
                            "Cukup ditutup oleh sistem HP. Transaksi pada selang itu mungkin tidak tercatat: cek mutasi bankmu dan catat yang kurang, " +
                            "atau pakai \"Samakan saldo\" di halaman dompet. Tiga langkah di bawah mencegahnya terulang.",
                        style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 4.dp),
                    )
                    TextAction("Sudah kucek", { vm.dismissGap() }, Modifier.padding(top = 6.dp), color = c.ink)
                }
            }
        }

        item { SectionHeader("Supaya tidak ada yang terlewat") }
        item {
            Link(
                "1. Izin baca notifikasi",
                if (allowed) "Sudah nyala" else "Belum. Ketuk, cari Cukup, lalu nyalakan.",
                valueColor = if (allowed) c.good else c.caution,
            ) { open(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        item {
            Link(
                "2. Baterai: tanpa batasan",
                if (battery) "Sudah bebas dari penghemat baterai" else "Masih dibatasi, jadi Cukup bisa ditutup diam-diam. Ketuk untuk ubah.",
                valueColor = if (battery) c.good else c.caution,
            ) { open(context, appDetails(context)) }
        }
        item {
            Link(
                "3. Mulai otomatis",
                if (xiaomi) "Cari Cukup lalu nyalakan. Tanpa ini HP Xiaomi tidak menghidupkan Cukup lagi setelah ditutup."
                else "Kalau HP-mu punya setelan \"Mulai otomatis\" / \"Autostart\", nyalakan untuk Cukup.",
            ) { autostart(context) }
        }
        item {
            Text(
                "Langkah 3 tidak bisa diperiksa dari dalam aplikasi, jadi tidak ada tanda centangnya. Kalau peringatan \"sempat mati\" muncul lagi, berarti langkah 2 atau 3 belum benar.",
                style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(horizontal = Gutter, vertical = 8.dp),
            )
        }

        item {
            SectionHeader("Notifikasi terakhir", trailing = {
                if (n.entries.isNotEmpty()) TextAction("Hapus", { vm.clearNotices() })
            })
        }
        item {
            Text(
                "${NoticeLog.KEEP} notifikasi keuangan terakhir dan hasilnya. Teksnya hanya tersimpan di HP ini.",
                style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(horizontal = Gutter).padding(bottom = 8.dp),
            )
        }
        if (n.entries.isEmpty()) {
            item {
                Text(
                    "Belum ada. Begitu ada notifikasi dari bank atau e-wallet, hasilnya muncul di sini.",
                    style = Type.body, color = c.mute, modifier = Modifier.padding(horizontal = Gutter),
                )
            }
        }
        items(n.entries) { e ->
            val (label, tint) = when (e.result) {
                NoticeLog.Result.RECORDED -> "Dicatat" to c.good
                NoticeLog.Result.PENDING -> "Perlu dicek" to c.caution
                NoticeLog.Result.SKIPPED -> "Dilewati" to c.faint
            }
            Column(
                Modifier.padding(horizontal = Gutter, vertical = 4.dp).fillMaxWidth().clip(CardShape).background(c.card)
                    .then(if (e.result == NoticeLog.Result.PENDING) Modifier.clickable(onClick = onOpenInbox) else Modifier).padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(tint)
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = Type.strong, color = c.ink, modifier = Modifier.weight(1f))
                    Text("${e.app} · ${dayLabel(localDate(e.at))} ${time(e.at)}", style = Type.bodySmall, color = c.faint)
                }
                Text(e.detail, style = Type.body, color = c.ink, modifier = Modifier.padding(top = 4.dp))
                Text("\"${e.text}\"", style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 4.dp))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Dot(color: Color) {
    Spacer(Modifier.size(10.dp).clip(CircleShape).background(color))
}

