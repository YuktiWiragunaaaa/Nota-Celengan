package id.cukup

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import id.cukup.data.Settings
import id.cukup.data.SettingsStore
import id.cukup.ui.CukupNav
import id.cukup.ui.components.InkButton
import id.cukup.ui.onboarding.italicize
import id.cukup.ui.theme.CukupTheme
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settingsStore: SettingsStore

    private var openAdd by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openAdd = intent?.getBooleanExtra(EXTRA_ADD, false) ?: false

        setContent {
            CukupTheme {
                val settings by settingsStore.settings.collectAsState(initial = null as Settings?)
                val s = settings
                when {
                    s == null -> Box(Modifier.fillMaxSize().background(colors.paper))
                    s.biometricLock && LockState.locked -> LockScreen(name = s.name, onUnlock = ::authenticate)
                    else -> CukupNav(onboarded = s.onboarded, openAdd = openAdd, onAddHandled = { openAdd = false })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ADD, false)) openAdd = true
    }

    private fun authenticate() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    LockState.locked = false
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Buka Cukup")
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .build(),
        )
    }

    @androidx.compose.runtime.Composable
    private fun LockScreen(name: String, onUnlock: () -> Unit) {
        val c = colors
        id.cukup.ui.components.LightStatusBarIcons(light = true)
        LaunchedEffect(Unit) { onUnlock() }
        val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
        val ring by pulse.animateFloat(
            1f, 1.18f,
            androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween<Float>(1400), androidx.compose.animation.core.RepeatMode.Reverse),
            label = "ring",
        )
        Column(
            Modifier.fillMaxSize().background(c.brandBrush).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(if (name.isBlank()) "Hai!" else "Hai, $name", style = Type.display, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("Uangmu aman di sini.", style = Type.body, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.weight(0.62f))
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(120.dp).graphicsLayer { scaleX = ring; scaleY = ring }.clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
                Box(
                    Modifier.size(88.dp).clip(CircleShape).background(Color.White).clickable(onClick = onUnlock),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Fingerprint, "Buka dengan sidik jari", tint = c.accent, modifier = Modifier.size(44.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            Text("Sentuh untuk membuka", style = Type.bodySmall, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.weight(1f))
        }
    }

    companion object {
        const val EXTRA_ADD = "id.cukup.ADD"
    }
}
