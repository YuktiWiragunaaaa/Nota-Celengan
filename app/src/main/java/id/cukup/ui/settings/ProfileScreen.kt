package id.cukup.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import id.cukup.ui.components.AvatarCropDialog
import id.cukup.ui.components.loadForCrop
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.cukup.ui.components.Avatar
import id.cukup.ui.components.CardShape
import id.cukup.ui.components.Gutter
import id.cukup.ui.components.LineField
import id.cukup.ui.components.Pill
import id.cukup.ui.components.RoundIcon
import id.cukup.ui.components.TextAction
import id.cukup.ui.components.deleteAvatar
import id.cukup.ui.components.saveAvatar
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlan: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val c = colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cropping by remember { mutableStateOf<Bitmap?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { cropping = loadForCrop(context, uri) }
    }
    cropping?.let { src ->
        AvatarCropDialog(src, onCancel = { cropping = null }, onDone = { out ->
            cropping = null
            scope.launch { if (saveAvatar(context, out)) vm.update { it.copy(avatarVersion = System.currentTimeMillis()) } }
        })
    }

    // Keping tembus pandang di tema gelap, putih bersih di tema terang (sama seperti Beranda).
    val chip = if (c.isDark) Color.White.copy(alpha = 0.08f) else c.card
    Column(Modifier.fillMaxSize().background(c.paper).verticalScroll(rememberScrollState())) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.heroBrush)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onBack, background = chip, tint = c.ink)
            }
            Box {
                Avatar(s.name, s.avatarVersion, size = 112.dp, background = chip, textColor = c.accent)
                Box(
                    Modifier.align(Alignment.BottomEnd).size(38.dp).clip(CircleShape).background(c.accent)
                        .clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.CameraAlt, "Ganti foto", tint = c.onAccent, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            Text(s.name.ifBlank { "Tanpa nama" }, style = Type.title, color = c.ink)
            if (s.avatarVersion > 0) {
                TextAction("Hapus foto", onClick = {
                    deleteAvatar(context)
                    vm.update { it.copy(avatarVersion = 0) }
                }, color = c.mute)
            }
        }
        Column(Modifier.padding(horizontal = Gutter).offset(y = (-28).dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.fillMaxWidth().clip(CardShape).background(c.card).padding(16.dp)) {
                Text("Nama panggilan", style = Type.bodySmall, color = c.mute)
                Spacer(Modifier.height(8.dp))
                LineField(s.name, { v -> vm.update { it.copy(name = v.take(20)) } }, "Nama panggilan")
            }
            ProfileLink(Icons.Rounded.Flag, "Rencana & target tabungan", onOpenPlan)
            ProfileLink(Icons.Rounded.Tune, "Setelan", onOpenSettings)
        }
    }
}

@Composable
private fun ProfileLink(icon: ImageVector, title: String, onClick: () -> Unit) {
    val c = colors
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(c.card).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(Pill).background(c.surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, style = Type.strong, color = c.ink, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.faint)
    }
}
