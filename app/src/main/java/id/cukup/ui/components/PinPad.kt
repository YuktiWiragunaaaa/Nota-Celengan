package id.cukup.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.cukup.domain.PinCode
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

/**
 * Layar PIN penuh di atas gradien. [onComplete] dipanggil saat digit terakhir diketik;
 * kembalikan false kalau salah supaya titik bergoyang dan dikosongkan.
 */
@Composable
fun PinPad(
    title: String,
    subtitle: String,
    onComplete: (String) -> Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    locked: Boolean = false,
    onFingerprint: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    val c = colors
    val haptic = LocalHapticFeedback.current
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(wrong) {
        if (wrong > 0) {
            shake.animateTo(0f, keyframes { durationMillis = 360; -18f at 60; 16f at 120; -10f at 200; 6f at 280 })
            entered = ""
        }
    }

    fun press(d: Char) {
        if (locked || entered.length >= PinCode.LENGTH) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        entered += d
        if (entered.length == PinCode.LENGTH) {
            if (onComplete(entered)) entered = "" else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                wrong++
            }
        }
    }

    Column(
        modifier.fillMaxSize().background(c.brandBrush).systemBarsPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            if (onClose != null) {
                Icon(
                    Icons.Rounded.Close, "Tutup", tint = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart).clip(CircleShape).clickable(onClick = onClose).padding(10.dp),
                )
            }
        }
        Spacer(Modifier.weight(0.6f))
        Text(title, style = Type.display, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = Type.body, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Row(Modifier.graphicsLayer { translationX = shake.value }, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(PinCode.LENGTH) { i ->
                val filled by animateFloatAsState(if (i < entered.length) 1f else 0f, label = "dot")
                Box(
                    Modifier.size(16.dp).graphicsLayer { scaleX = 0.8f + 0.2f * filled; scaleY = 0.8f + 0.2f * filled }
                        .clip(CircleShape).background(Color.White.copy(alpha = 0.3f + 0.7f * filled)),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(message ?: " ", style = Type.bodySmall, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(0.4f))
        val rows = listOf("123", "456", "789")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) { r.forEach { d -> Key(d.toString()) { press(d) } } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                if (onFingerprint != null) {
                    KeyIcon({ Icon(Icons.Rounded.Fingerprint, "Sidik jari", tint = Color.White, modifier = Modifier.size(30.dp)) }, onFingerprint)
                } else Spacer(Modifier.size(76.dp))
                Key("0") { press('0') }
                KeyIcon({ Icon(Icons.AutoMirrored.Rounded.Backspace, "Hapus", tint = Color.White, modifier = Modifier.size(24.dp)) }) {
                    entered = entered.dropLast(1)
                }
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun Key(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(76.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 28.sp, style = Type.title) }
}

@Composable
private fun KeyIcon(icon: @Composable () -> Unit, onClick: () -> Unit) {
    Box(Modifier.size(76.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { icon() }
}

/** Membuat PIN baru: ketik, lalu ketik ulang. [onDone] menerima "garam:hash". */
@Composable
fun PinSetupDialog(onCancel: () -> Unit, onDone: (String) -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        PinPad(
            title = if (first == null) "Bikin PIN" else "Ulangi PIN",
            subtitle = if (first == null) "${PinCode.LENGTH} angka buat buka Cukup." else "Ketik sekali lagi biar yakin.",
            message = message,
            onClose = onCancel,
            onComplete = { pin ->
                val f = first
                when {
                    f == null -> { first = pin; message = null; true }
                    f == pin -> { onDone(PinCode.make(pin)); true }
                    else -> { first = null; message = "Beda. Ulang dari awal ya."; false }
                }
            },
        )
    }
}
