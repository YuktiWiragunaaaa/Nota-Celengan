package id.cukup.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.cukup.data.MoneyRepository
import id.cukup.domain.Advisor
import id.cukup.domain.Insight
import id.cukup.domain.Pocket
import id.cukup.domain.Schedule
import id.cukup.domain.Summary
import id.cukup.domain.Transaction
import id.cukup.domain.TxStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeState(
    val loaded: Boolean = false,
    val name: String = "",
    val summary: Summary? = null,
    val recent: List<Transaction> = emptyList(),
    val pendingCount: Int = 0,
    val pocketsById: Map<Long, Pocket> = emptyMap(),
    val pocketIndex: Map<Long, Int> = emptyMap(),
    val schedule: Schedule = Schedule(),
    val nextPayday: LocalDate? = null,
    val insights: List<Insight> = emptyList(),
    val chart: String = "DONUT",
    val reading: List<String> = emptyList(),
    val avatarVersion: Long = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: MoneyRepository,
    private val settingsStore: id.cukup.data.SettingsStore,
) : ViewModel() {

    fun setChart(type: String) {
        viewModelScope.launch { settingsStore.update { it.copy(chart = type) } }
    }


    val state: StateFlow<HomeState> = combine(
        repository.summary,
        repository.transactions,
        repository.pending,
        repository.settings,
    ) { summary, txs, pending, settings ->
        val pockets = summary.pockets.map { it.pocket }
        HomeState(
            loaded = true,
            name = settings.name,
            summary = summary,
            recent = txs.filter { it.status == TxStatus.CONFIRMED }.take(5),
            pendingCount = pending.size,
            pocketsById = pockets.associateBy { it.id },
            pocketIndex = pockets.mapIndexed { i, p -> p.id to i }.toMap(),
            schedule = settings.schedule,
            nextPayday = repository.cycle(settings.schedule).nextPayday,
            chart = settings.chart,
            reading = id.cukup.domain.ChartReader.read(summary),
            avatarVersion = settings.avatarVersion,
            insights = Advisor.insights(
                summary, txs, pockets,
                repository.cycle(settings.schedule), repository.previousCycle(settings.schedule), LocalDate.now(),
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}
