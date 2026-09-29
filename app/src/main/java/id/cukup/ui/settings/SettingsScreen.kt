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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
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
import id.cukup.notif.MoneyNotificationListener
import id.cukup.ui.components.Eyebrow
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.LineField
import id.cukup.ui.components.ScheduleEditor
import id.cukup.ui.components.describe
import id.cukup.domain.Rupiah
import androidx.compose.ui.text.input.KeyboardType
import id.cukup.ui.onboarding.Bullet
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val repository: id.cukup.data.MoneyRepository,
) : ViewModel() {
    fun eraseEverything() {
        viewModelScope.launch { repository.eraseEverything() }
    }

    val settings: StateFlow<Settings> = store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { store.update(transform) }
    }
}

@Composable
fun SettingsScreen(contentPadding: PaddingValues, onEditSplit: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
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
        Text(
            "Setelan",
            style = Type.display, color = c.ink,
            modifier = Modifier.padding(horizontal = Gutter).padding(top = 24.dp, bottom = 8.dp),
        )

        Group("Kamu")
        Column(Modifier.padding(horizontal = Gutter, vertical = 12.dp)) {
            LineField(s.name, { v -> vm.update { it.copy(name = v.take(20)) } }, "Nama panggilan")
        }
        Hairline()

        Group("Uang masuk & pembagian")
        Link("Jadwal gajian", s.schedule.describe()) { editSchedule = true }
        Link("Pembagian ke kantong", "Ubah persen tiap kantong", onClick = onEditSplit)

        Group("Peringatan")
        Toggle(
            "Beri tahu kalau belanja kebanyakan",
            if (notifAllowed) "Saat 80% jatah terpakai, dan saat lewat." else "Izin notifikasinya belum ada. Nyalakan untuk minta izin.",
            s.budgetAlerts && notifAllowed,
        ) { on ->
            if (on && !notifAllowed && Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
            vm.update { it.copy(budgetAlerts = on) }
        }
        Link(
            "Batas sekali belanja",
            if (s.singleLimit > 0) "Tanya dulu kalau di atas ${Rupiah.format(s.singleLimit)}" else "Belum diatur",
        ) { editLimit = true }

        Group("Catat otomatis")
        Link(
            "Baca notifikasi e-wallet & bank",
            if (listenerOn) "Aktif" else "Belum nyala. Ketuk di sini.",
            valueColor = if (listenerOn) null else c.caution,
        ) { context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        Toggle(
            "Langsung simpan tanpa dicek",
            "Kalau mati, kamu cek dulu sebelum tersimpan.",
            s.autoConfirm,
        ) { on -> vm.update { it.copy(autoConfirm = on) } }
        Text(
            "Pakai Xiaomi? Biar tetap jalan, buka Setelan HP, pilih Aplikasi, cari Cukup, lalu ubah Penghemat baterai ke \"Tanpa batasan\" dan nyalakan \"Mulai otomatis\".",
            style = Type.bodySmall, color = c.faint,
            modifier = Modifier.padding(horizontal = Gutter, vertical = 12.dp),
        )
        Hairline()

        Group("Keamanan")
        Toggle(
            "Kunci dengan sidik jari / PIN",
            if (biometricAvailable) "Diminta setiap membuka Cukup." else "Atur kunci layar HP dulu.",
            s.biometricLock && biometricAvailable,
            enabled = biometricAvailable,
        ) { on -> vm.update { it.copy(biometricLock = on) } }

        Group("Data")
        Row(
            Modifier.fillMaxWidth().clickable { confirmErase = true }.padding(horizontal = Gutter, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Hapus semua data", style = Type.strong, color = c.over)
                Text("Mulai dari nol lagi. Kantong, catatan, dan setelan ikut terhapus.", style = Type.bodySmall, color = c.mute)
            }
        }
        Hairline()

        Group("Privasi")
        Column(Modifier.padding(horizontal = Gutter, vertical = 8.dp)) {
            Bullet("Cukup nggak pakai internet, jadi datamu nggak ke mana-mana.")
            Bullet("Datanya nggak ikut backup Google. Kalau aplikasinya dihapus, datanya ikut hilang.")
        }
        Text(
            "Cukup 0.4",
            style = Type.bodySmall, color = c.faint,
            modifier = Modifier.padding(horizontal = Gutter, vertical = 20.dp),
        )
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Hapus semua data?", style = Type.title) },
            text = { Text("Semua kantong, catatan, dan setelan hilang dan nggak bisa dikembalikan.", style = Type.body) },
            confirmButton = { TextButton({ confirmErase = false; vm.eraseEverything() }) { Text("Hapus semua", color = c.over) } },
            dismissButton = { TextButton({ confirmErase = false }) { Text("Batal", color = c.ink) } },
            containerColor = c.card,
        )
    }

    if (editLimit) {
        var draft by remember { mutableStateOf(if (s.singleLimit > 0) s.singleLimit.toString() else "") }
        AlertDialog(
            onDismissRequest = { editLimit = false },
            title = { Text("Batas sekali belanja", style = Type.title) },
            text = {
                Column {
                    Text("Kalau satu kali belanja lebih dari ini, Cukup tanya dulu sebelum menyimpan. Kosongkan untuk mematikan.", style = Type.body, color = c.mute)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                    LineField(draft, { v -> draft = v.filter(Char::isDigit).take(10) }, "Misalnya 200000", keyboardType = KeyboardType.Number)
                    if (draft.isNotEmpty()) Text(Rupiah.format(draft.toLong()), style = Type.bodySmall, color = c.mute, modifier = Modifier.padding(top = 6.dp))
                }
            },
            confirmButton = {
                TextButton({ vm.update { it.copy(singleLimit = draft.toLongOrNull() ?: 0) }; editLimit = false }) { Text("Simpan", color = c.ink) }
            },
            dismissButton = { TextButton({ editLimit = false }) { Text("Batal", color = c.mute) } },
            containerColor = c.paper,
        )
    }

    if (editSchedule) {
        var draft by remember { mutableStateOf(s.schedule) }
        AlertDialog(
            onDismissRequest = { editSchedule = false },
            title = { Text("Jadwal gajian", style = Type.title) },
            text = { ScheduleEditor(draft, { draft = it }) },
            confirmButton = {
                TextButton({ vm.update { it.copy(schedule = draft) }; editSchedule = false }) { Text("Simpan", color = c.ink) }
            },
            dismissButton = { TextButton({ editSchedule = false }) { Text("Batal", color = c.mute) } },
            containerColor = c.paper,
        )
    }
}

private fun canNotify(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

@Composable
private fun Group(title: String) {
    Eyebrow(title, Modifier.padding(horizontal = Gutter).padding(top = 28.dp, bottom = 8.dp))
    Hairline()
}

@Composable
private fun Link(title: String, value: String, valueColor: Color? = null, onClick: () -> Unit) {
    val c = colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Gutter, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.strong, color = c.ink)
            Text(value, style = Type.bodySmall, color = valueColor ?: c.mute)
        }
        Text("›", style = Type.title, color = c.faint)
    }
    Hairline()
}

@Composable
private fun Toggle(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val c = colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = Type.strong, color = if (enabled) c.ink else c.faint)
            Text(subtitle, style = Type.bodySmall, color = c.mute)
        }
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.ink, checkedThumbColor = c.paper,
                uncheckedBorderColor = c.faint, uncheckedThumbColor = c.faint, uncheckedTrackColor = c.paper,
            ),
        )
    }
    Hairline()
}
