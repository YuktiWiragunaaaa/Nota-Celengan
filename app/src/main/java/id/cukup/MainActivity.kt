package id.cukup

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.remember
import id.cukup.domain.PinCode
import id.cukup.ui.components.PinPad
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
                    s.biometricLock && s.pinHash.isNotEmpty() && LockState.locked -> LockScreen(s)
                    else -> CukupNav(onboarded = s.onboarded, openAdd = openAdd, onAddHandled = { openAdd = false })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ADD, false)) openAdd = true
    }

    private fun canFingerprint(): Boolean =
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    private fun fingerprint() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    LockState.unlock()
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Buka Cukup")
                .setNegativeButtonText("Pakai PIN")
                .setAllowedAuthenticators(BIOMETRIC_WEAK)
                .build(),
        )
    }

    @androidx.compose.runtime.Composable
    private fun LockScreen(s: Settings) {
        id.cukup.ui.components.LightStatusBarIcons(light = true)
        val useFinger = s.fingerprint && remember { canFingerprint() }
        LaunchedEffect(Unit) { if (useFinger) fingerprint() }
        var now by remember { mutableStateOf(System.currentTimeMillis()) }
        val waitUntil = LockState.waitUntil
        LaunchedEffect(waitUntil) {
            while (System.currentTimeMillis() < waitUntil) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(500) }
            now = System.currentTimeMillis()
        }
        val waiting = now < waitUntil
        PinPad(
            title = if (s.name.isBlank()) "Hai!" else "Hai, ${s.name}",
            subtitle = "Masukkan PIN Cukup.",
            locked = waiting,
            message = when {
                waiting -> "Kebanyakan salah. Coba lagi ${(waitUntil - now + 999) / 1000} detik."
                LockState.wrong > 0 -> "PIN salah. Sisa ${PinCode.MAX_TRIES - LockState.wrong} kali."
                else -> null
            },
            onFingerprint = if (useFinger) ::fingerprint else null,
            onComplete = { pin ->
                if (PinCode.verify(pin, s.pinHash)) { LockState.unlock(); true } else { LockState.fail(); false }
            },
        )
    }

    companion object {
        const val EXTRA_ADD = "id.cukup.ADD"
    }
}
