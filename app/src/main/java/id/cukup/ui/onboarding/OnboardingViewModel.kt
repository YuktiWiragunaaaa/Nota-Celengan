package id.cukup.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.data.SettingsStore
import id.cukup.domain.Allocator
import id.cukup.domain.Frequency
import id.cukup.domain.Pocket
import id.cukup.domain.Presets
import id.cukup.domain.Schedule
import id.cukup.ui.components.EditablePocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class OnboardingStep { NAME, SCHEDULE, SPLIT, START, PERMISSIONS }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.NAME,
    val name: String = "",
    val schedule: Schedule = Schedule(
        frequency = Frequency.WEEKLY,
        weekday = 5,
        anchor = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)).toEpochDay(),
    ),
    val presetId: String = Presets.all.first().id,
    val pockets: List<EditablePocket> = pocketsFor(Presets.all.first().id),
    val startAmount: Long = 0,
    val saving: Boolean = false,
) {
    val total: Int get() = pockets.sumOf { it.percent }
    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.SPLIT -> total == 100 && pockets.all { it.name.isNotBlank() }
            else -> !saving
        }
    val previewPockets: List<Pocket> get() = pockets.mapIndexed { i, p -> p.toPocket(i).copy(id = i.toLong() + 1) }
}

sealed interface OnboardingIntent {
    data class Name(val value: String) : OnboardingIntent
    data class SetSchedule(val schedule: Schedule) : OnboardingIntent
    data class ChoosePreset(val id: String) : OnboardingIntent
    data class EditPockets(val pockets: List<EditablePocket>) : OnboardingIntent
    data class StartAmount(val amount: Long) : OnboardingIntent
    data object Next : OnboardingIntent
    data object Back : OnboardingIntent
}

private fun pocketsFor(presetId: String): List<EditablePocket> =
    Presets.all.first { it.id == presetId }.pockets.mapIndexed { i, p ->
        EditablePocket(i.toLong() + 1, 0, p.name, p.emoji, p.percent, p.kind, p.tag)
    }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: MoneyRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.Name -> _state.update { it.copy(name = intent.value.take(20)) }
            is OnboardingIntent.SetSchedule -> _state.update { it.copy(schedule = intent.schedule) }
            is OnboardingIntent.ChoosePreset -> _state.update { it.copy(presetId = intent.id, pockets = pocketsFor(intent.id)) }
            is OnboardingIntent.EditPockets -> _state.update { it.copy(pockets = intent.pockets) }
            is OnboardingIntent.StartAmount -> _state.update { it.copy(startAmount = intent.amount) }
            OnboardingIntent.Back -> _state.update {
                it.copy(step = OnboardingStep.entries[(it.step.ordinal - 1).coerceAtLeast(0)])
            }
            OnboardingIntent.Next -> next()
        }
    }

    private fun next() {
        val s = _state.value
        if (!s.canContinue) return
        when (s.step) {
            OnboardingStep.START -> finish()
            OnboardingStep.PERMISSIONS -> Unit
            else -> _state.update { it.copy(step = OnboardingStep.entries[it.step.ordinal + 1]) }
        }
    }

    /** Menyimpan kantong, jadwal, dan uang awal; lalu ke langkah izin. */
    private fun finish() {
        val s = _state.value
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val pockets = s.pockets.mapIndexed { i, p -> p.toPocket(i).copy(id = 0) }
            check(Allocator.isValid(pockets))
            repository.savePockets(pockets)
            settings.update { it.copy(name = s.name.trim(), schedule = s.schedule) }
            if (s.startAmount > 0) {
                repository.addIncome(s.startAmount, "Uang awal", "", System.currentTimeMillis(), toPocketId = null)
            }
            _state.update { it.copy(saving = false, step = OnboardingStep.PERMISSIONS) }
        }
    }

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            settings.update { it.copy(onboarded = true) }
            onDone()
        }
    }
}
