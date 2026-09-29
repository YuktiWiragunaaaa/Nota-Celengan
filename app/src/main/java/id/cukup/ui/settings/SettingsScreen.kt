package id.cukup.ui.settings

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import id.cukup.ui.onboarding.Bullet
import id.cukup.ui.onboarding.italicize
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val store: SettingsStore) : ViewModel() {
    val settings: StateFlow<Settings> = store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { store.update(transform) }
    }
}

@Composable
fun SettingsScreen(contentPadding: PaddingValues, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val c = colors
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var listenerOn by remember { mutableStateOf(MoneyNotificationListener.isEnabled(context)) }
    var pickPayday by remember { mutableStateOf(false) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { listenerOn = MoneyNotificationListener.isEnabled(context) }
    }
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    Column(
        Modifier.fillMaxSize().background(c.paper).verticalScroll(rememberScrollState()).padding(contentPadding),
    ) {
        Text(
            italicize("Setelan"),
            style = Type.display, color = c.ink,
            modifier = Modifier.padding(horizontal = Gutter).padding(top = 24.dp, bottom = 16.dp),
        )

        Group("Kamu")
        Column(Modifier.padding(horizontal = Gutter, vertical = 8.dp)) {
            LineField(s.name, { v -> vm.update { it.copy(name = v.take(20)) } }, "Nama panggilan")
        }
        Row(
            Modifier.fillMaxWidth().clickable { pickPayday = true }.padding(horizontal = Gutter, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Tanggal gajian", style = Type.strong, color = c.ink)
                Text("Awal siklus anggaran", style = Type.bodySmall, color = c.mute)
            }
            Text("Tanggal ${s.payday}", style = Type.amount, color = c.ink)
        }
        Hairline()

        Group("Catat otomatis")
        Row(
            Modifier.fillMaxWidth()
                .clickable { context.startActivity(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                .padding(horizontal = Gutter, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Baca notifikasi transaksi", style = Type.strong, color = c.ink)
                Text(
                    if (listenerOn) "Aktif. Ketuk untuk mengubah di pengaturan Android." else "Belum aktif. Ketuk untuk mengizinkan.",
                    style = Type.bodySmall, color = if (listenerOn) c.mute else c.caution,
                )
            }
            Text(if (listenerOn) "Aktif" else "Mati", style = Type.label, color = if (listenerOn) c.ink else c.faint)
        }
        Hairline()
        Toggle(
            "Langsung catat tanpa dicek",
            "Transaksi dari notifikasi langsung masuk pos. Yang mirip duplikat tetap perlu dicek.",
            s.autoConfirm,
        ) { on -> vm.update { it.copy(autoConfirm = on) } }

        Group("Keamanan")
        Toggle(
            "Kunci dengan sidik jari / PIN",
            if (biometricAvailable) "Diminta setiap membuka Cukup." else "Atur kunci layar di HP dulu untuk memakai ini.",
            s.biometricLock && biometricAvailable,
            enabled = biometricAvailable,
        ) { on -> vm.update { it.copy(biometricLock = on) } }

        Group("Privasi")
        Column(Modifier.padding(horizontal = Gutter, vertical = 8.dp)) {
            Bullet("Cukup tidak meminta izin internet. Data tidak bisa keluar dari HP ini.")
            Bullet("Data tidak ikut backup Google, jadi uninstall = data hilang.")
            Bullet("Isi notifikasi tidak disimpan, hanya nominal, toko, dan jenis transaksi.")
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Cukup 0.1 · dibuat pelan-pelan",
            style = Type.statement.copy(fontSize = Type.bodySmall.fontSize * 1.2f), color = c.faint,
            modifier = Modifier.padding(horizontal = Gutter, vertical = 16.dp),
        )
    }

    if (pickPayday) {
        var day by remember { mutableIntStateOf(s.payday) }
        AlertDialog(
            onDismissRequest = { pickPayday = false },
            title = { Text("Tanggal gajian", style = Type.title) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton({ day = if (day == 1) 31 else day - 1 }) { Text("−", style = Type.title, color = c.ink) }
                    Text("$day", style = Type.number.copy(fontSize = Type.display.fontSize), color = c.ink, modifier = Modifier.padding(horizontal = 24.dp))
                    TextButton({ day = if (day == 31) 1 else day + 1 }) { Text("+", style = Type.title, color = c.ink) }
                }
            },
            confirmButton = {
                TextButton({ vm.update { it.copy(payday = day) }; pickPayday = false }) { Text("Simpan", color = c.ink) }
            },
            dismissButton = { TextButton({ pickPayday = false }) { Text("Batal", color = c.mute) } },
            containerColor = c.paper,
        )
    }
}

@Composable
private fun Group(title: String) {
    Eyebrow(title, Modifier.padding(horizontal = Gutter).padding(top = 28.dp, bottom = 8.dp))
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
            colors = SwitchDefaults.colors(checkedTrackColor = c.ink, checkedThumbColor = c.paper, uncheckedBorderColor = c.faint, uncheckedThumbColor = c.faint, uncheckedTrackColor = c.paper),
        )
    }
    Hairline()
}
