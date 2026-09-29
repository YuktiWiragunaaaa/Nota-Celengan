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
    private var unlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        unlocked = savedInstanceState?.getBoolean(KEY_UNLOCKED) ?: false
        openAdd = intent?.getBooleanExtra(EXTRA_ADD, false) ?: false

        setContent {
            CukupTheme {
                val settings by settingsStore.settings.collectAsState(initial = null as Settings?)
                val s = settings
                when {
                    s == null -> Box(Modifier.fillMaxSize().background(colors.paper))
                    s.biometricLock && !unlocked -> LockScreen(onUnlock = ::authenticate)
                    else -> CukupNav(onboarded = s.onboarded, openAdd = openAdd, onAddHandled = { openAdd = false })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ADD, false)) openAdd = true
    }

    override fun onStop() {
        super.onStop()
        // Kunci lagi bila aplikasi ditinggal (bukan sekadar rotasi layar).
        if (!isChangingConfigurations) unlocked = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_UNLOCKED, unlocked)
    }

    private fun authenticate() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
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
    private fun LockScreen(onUnlock: () -> Unit) {
        val c = colors
        LaunchedEffect(Unit) { onUnlock() }
        Column(
            Modifier.fillMaxSize().background(c.paper).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(italicize("Cukup <i>terkunci.</i>"), style = Type.display, color = c.ink)
            Spacer(Modifier.height(24.dp))
            InkButton("Buka", onClick = onUnlock, modifier = Modifier.fillMaxWidth(0.6f))
        }
    }

    companion object {
        const val EXTRA_ADD = "id.cukup.ADD"
        private const val KEY_UNLOCKED = "unlocked"
    }
}
