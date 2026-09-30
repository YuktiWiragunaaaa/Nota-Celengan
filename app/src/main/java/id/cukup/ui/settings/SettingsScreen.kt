package id.cukup.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.Settings
import id.cukup.data.SettingsStore
import id.cukup.domain.Rupiah
import id.cukup.notif.MoneyNotificationListener
import id.cukup.ui.AppViewModel
import id.cukup.ui.components.AmountDialog
import id.cukup.ui.components.Bullet
import id.cukup.ui.components.Group
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.Link
import id.cukup.ui.components.ScheduleEditor
import id.cukup.ui.components.Toggle
import id.cukup.ui.components.describe
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Dipakai layar Profil. */
@HiltViewModel
class SettingsViewModel @Inject constructor(private val store: SettingsStore) : ViewModel() {
    val settings: StateFlow<Settings> = store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { store.update(transform) }
    }
}

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onOpenAccount: (Long) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHelp: () -> Unit,
    vm: AppViewModel = hiltViewModel(),
) {
    val o by vm.overview.collectAsStateWithLifecycle()
    val data = o ?: return
    val s = data.settings
    val c = colors
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var listenerOn by remember { mutableStateOf(MoneyNotificationListener.isEnabled(context)) }
    var notifAllowed by remember { mutableStateOf(canNotify(context)) }
    var editSchedule by remember { mutableStateOf(false) }
    var editLimit by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifAllowed = it }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            listenerOn = MoneyNotificationListener.isEnabled(context)
            notifAllowed = canNotify(context)
        }
    }
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    Column(Modifier.fillMaxSize().background(c.paper).verticalScroll(rememberScrollState()).padding(contentPadding)) {
        Text("Setelan", style = Type.display, color = c.ink, modifier = Modifier.padding(horizontal = Gutter).padding(top = 24.dp, bottom = 8.dp))

        Group("Kamu")
        Link("Profil", s.name.ifBlank { "Nama & foto" }, onClick = onOpenProfile)
        Link("Cara pakai Cukup", "Beda Catatan dan Rencana, dan apa efeknya", onClick = onOpenHelp)

        Group("Catatan (uang sungguhan)")
        data.accounts.forEach { ab ->
            Link(
                ab.account.name,
                Rupiah.format(ab.balance) + if (ab.account.id == s.defaultAccountId) " · utama" else "",
                leading = ab.account.emoji,
            ) { onOpenAccount(ab.account.id) }
        }
        Link("Tambah dompet", "Tunai, rekening, e-wallet, paylater", leading = "➕") { onOpenAccount(0) }
        Link("Kategori", "${data.categories.size} kategori · ubah nama, ikon, tambah", onClick = onOpenCategories)
        Link("Jadwal gajian", s.schedule.describe() + " · menentukan awal periode") { editSchedule = true }

        Group("Rencana (batas yang kamu tetapkan)")
        Link(
            "Rencana belanja",
            if (data.planStatus.active) data.plan.joinToString(" · ") { "${it.name} ${it.percent}%" } else "Belum ada",
            onClick = onOpenPlan,
        )
        Link(
            "Batas sekali belanja",
            if (s.singleLimit > 0) "Tanya dulu kalau di atas ${Rupiah.format(s.singleLimit)}" else "Belum diatur",
        ) { editLimit = true }
        Toggle(
            "Beri tahu kalau belanja kebanyakan",
            if (notifAllowed) "Saat 80% batas rencana terpakai, saat lewat, dan saat belanja di atas batas sekali belanja."
            else "Izin notifikasinya belum ada. Nyalakan untuk minta izin.",
            s.budgetAlerts && notifAllowed,
        ) { on ->
            if (on && !notifAllowed && Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
            vm.settings { it.copy(budgetAlerts = on) }
        }

        Group("Catat otomatis")
        Link(
            "Baca notifikasi e-wallet & bank",
            if (listenerOn) "Aktif" else "Belum nyala. Ketuk di sini.",
            valueColor = if (listenerOn) null else c.caution,
        ) { context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        Toggle(
            "Langsung simpan tanpa dicek",
            "Kalau mati, transaksi masuk \"Perlu dicek\" dulu dan saldo belum berubah sampai kamu simpan.",
            s.autoConfirm,
        ) { on -> vm.settings { it.copy(autoConfirm = on) } }
        Text(
            "Dompet ditebak dari nama aplikasi: notifikasi GoPay masuk ke dompet bernama \"GoPay\". Pakai Xiaomi? Buka Setelan HP › Aplikasi › Cukup, ubah Penghemat baterai ke \"Tanpa batasan\" dan nyalakan \"Mulai otomatis\".",
            style = Type.bodySmall, color = c.faint,
            modifier = Modifier.padding(horizontal = Gutter, vertical = 12.dp),
        )
        Hairline()

        Group("Keamanan")
        Toggle(
            "Kunci dengan sidik jari / PIN",
            if (biometricAvailable) "Diminta saat membuka Cukup setelah layar HP mati." else "Atur kunci layar HP dulu.",
            s.biometricLock && biometricAvailable,
            enabled = biometricAvailable,
        ) { on -> vm.settings { it.copy(biometricLock = on) } }

        Group("Data")
        Row(
            Modifier.fillMaxWidth().clickable { confirmErase = true }.padding(horizontal = Gutter, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Hapus semua data", style = Type.strong, color = c.over)
                Text("Mulai dari nol lagi. Dompet, catatan, rencana, dan setelan ikut terhapus.", style = Type.bodySmall, color = c.mute)
            }
        }
        Hairline()

        Group("Privasi")
        Column(Modifier.padding(horizontal = Gutter, vertical = 8.dp)) {
            Bullet("Cukup nggak pakai internet, jadi datamu nggak ke mana-mana.")
            Bullet("Datanya nggak ikut backup Google. Kalau aplikasinya dihapus, datanya ikut hilang.")
        }
        Text("Cukup 0.6", style = Type.bodySmall, color = c.faint, modifier = Modifier.padding(horizontal = Gutter, vertical = 20.dp))
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Hapus semua data?", style = Type.title) },
            text = { Text("Semua dompet, catatan, rencana, dan setelan hilang dan nggak bisa dikembalikan.", style = Type.body) },
            confirmButton = { TextButton({ confirmErase = false; vm.eraseEverything() }) { Text("Hapus semua", color = c.over) } },
            dismissButton = { TextButton({ confirmErase = false }) { Text("Batal", color = c.ink) } },
            containerColor = c.card,
        )
    }
    if (editLimit) {
        AmountDialog(
            "Batas sekali belanja",
            "Berlaku per transaksi. Kalau satu kali belanja lebih dari ini, Cukup tanya dulu sebelum menyimpan. Isi 0 untuk mematikan.",
            s.singleLimit, onDismiss = { editLimit = false },
        ) { v -> vm.settings { it.copy(singleLimit = v.coerceAtLeast(0)) }; editLimit = false }
    }
    if (editSchedule) {
        var draft by remember { mutableStateOf(s.schedule) }
        AlertDialog(
            onDismissRequest = { editSchedule = false },
            title = { Text("Jadwal gajian", style = Type.title) },
            text = { ScheduleEditor(draft, { draft = it }) },
            confirmButton = { TextButton({ vm.settings { it.copy(schedule = draft) }; editSchedule = false }) { Text("Simpan", color = c.ink) } },
            dismissButton = { TextButton({ editSchedule = false }) { Text("Batal", color = c.mute) } },
            containerColor = c.paper,
        )
    }
}

private fun canNotify(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
