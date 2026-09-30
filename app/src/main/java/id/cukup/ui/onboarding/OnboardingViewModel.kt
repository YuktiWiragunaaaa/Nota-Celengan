package id.cukup.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.data.SettingsStore
import id.cukup.domain.Account
import id.cukup.domain.AccountKind
import id.cukup.domain.PlanBasis
import id.cukup.domain.PlanPreset
import id.cukup.domain.Presets
import id.cukup.domain.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { NAME, WALLETS, SCHEDULE, PLAN, PERMISSIONS }

/** Dompet yang sedang disiapkan. Untuk paylater, [amount] = hutang sekarang. */
data class DraftAccount(val key: Int, val name: String, val emoji: String, val kind: AccountKind, val amount: Long = 0)

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.NAME,
    val name: String = "",
    val accounts: List<DraftAccount> = listOf(DraftAccount(0, "Tunai", "💵", AccountKind.CASH)),
    val schedule: Schedule = Schedule(),
    val preset: PlanPreset? = null,
    val income: Long = 0,
    val saving: Boolean = false,
) {
    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.NAME -> name.isNotBlank()
            OnboardingStep.WALLETS -> accounts.isNotEmpty() && accounts.all { it.name.isNotBlank() }
            OnboardingStep.PLAN -> preset == null || income > 0
            else -> true
        }
}

sealed interface OnboardingIntent {
    data class Name(val value: String) : OnboardingIntent
    data class Toggle(val seedName: String) : OnboardingIntent
    data class Amount(val key: Int, val value: Long) : OnboardingIntent
    data class SetSchedule(val schedule: Schedule) : OnboardingIntent
    data class Preset(val preset: PlanPreset?) : OnboardingIntent
    data class Income(val value: Long) : OnboardingIntent
    data object Next : OnboardingIntent
    data object Back : OnboardingIntent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: MoneyRepository,
    private val store: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()
    private var nextKey = 1

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.Name -> _state.update { it.copy(name = intent.value.take(20)) }
            is OnboardingIntent.Toggle -> _state.update { s ->
                val existing = s.accounts.firstOrNull { it.name == intent.seedName }
                if (existing != null) {
                    s.copy(accounts = s.accounts - existing)
                } else {
                    val seed = Presets.accounts.first { it.name == intent.seedName }
                    s.copy(accounts = s.accounts + DraftAccount(nextKey++, seed.name, seed.emoji, seed.kind))
                }
            }
            is OnboardingIntent.Amount -> _state.update { s ->
                s.copy(accounts = s.accounts.map { if (it.key == intent.key) it.copy(amount = intent.value) else it })
            }
            is OnboardingIntent.SetSchedule -> _state.update { it.copy(schedule = intent.schedule) }
            is OnboardingIntent.Preset -> _state.update { it.copy(preset = intent.preset) }
            is OnboardingIntent.Income -> _state.update { it.copy(income = intent.value) }
            OnboardingIntent.Next -> _state.update { s ->
                if (!s.canContinue) s else s.copy(step = OnboardingStep.entries.getOrElse(s.step.ordinal + 1) { s.step })
            }
            OnboardingIntent.Back -> _state.update { s ->
                s.copy(step = OnboardingStep.entries.getOrElse(s.step.ordinal - 1) { s.step })
            }
        }
    }

    fun complete(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.setup(
                s.accounts.mapIndexed { i, d ->
                    Account(0, d.name, d.emoji, d.kind, if (d.kind == AccountKind.PAYLATER) -d.amount else d.amount, i)
                },
                s.preset,
            )
            store.update {
                it.copy(
                    name = s.name.trim(),
                    schedule = s.schedule,
                    planBasis = PlanBasis(PlanBasis.Mode.FIXED, s.income),
                    onboarded = true,
                )
            }
            onDone()
        }
    }
}
