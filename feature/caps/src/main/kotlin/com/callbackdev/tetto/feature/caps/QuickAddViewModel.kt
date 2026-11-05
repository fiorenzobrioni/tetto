package com.callbackdev.tetto.feature.caps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.data.widget.QuickAdd
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.Spend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * The numpad over the home screen (PLANNING.md §7, Taps; §8, the launcher's shortcuts), on the cap
 * its door names ([QuickAdd.EXTRA_CAP_ID]): the cap as the caps screen shows it, after the
 * catch-up; the spend added; Undo, which puts the amount back on the numpad, since most spends
 * undone at once are a key pressed wrong.
 */
@HiltViewModel
class QuickAddViewModel
@Inject
constructor(
    savedState: SavedStateHandle,
    private val caps: CapsRepository,
    settings: SettingsRepository,
    private val clock: TodaySource,
) : ViewModel() {
    private val capId: Long = savedState.get<Long>(QuickAdd.EXTRA_CAP_ID) ?: NO_CAP

    val state: StateFlow<QuickAddUiState?> = combine(caps.states, settings.currency) { states, currency ->
        QuickAddUiState(clock.today(), currency, states.firstOrNull { it.cap.id == capId })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val phases = MutableStateFlow<QuickAddPhase>(QuickAddPhase.Typing())

    val phase: StateFlow<QuickAddPhase> = phases.asStateFlow()

    /** Add: the spend, now, in the cap's open period; the line that says so, or the cap gone. */
    fun add(amount: Money, onAdded: () -> Unit = {}) {
        val typing = phases.value as? QuickAddPhase.Typing ?: return
        phases.value = QuickAddPhase.Adding(typing.round)
        viewModelScope.launch {
            val spend = caps.addSpend(capId, amount)
            phases.value =
                if (spend == null) QuickAddPhase.Typing(round = typing.round + 1) else QuickAddPhase.Added(spend)
            if (spend != null) onAdded()
        }
    }

    /** Undo: the spend taken back, and the numpad again with its amount typed in. */
    fun undo() {
        val added = phases.value as? QuickAddPhase.Added ?: return
        viewModelScope.launch {
            val undone = caps.undoAdd(added.spend)
            phases.value = QuickAddPhase.Typing(initial = added.spend.amount.takeIf { undone }, round = added.round + 1)
        }
    }

    private companion object {
        const val NO_CAP = -1L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** The cap the numpad is open on (null: it is no longer there), today, the currency. */
data class QuickAddUiState(val today: LocalDate, val currency: AppCurrency, val cap: CapState?)

/** Where the sheet stands: typing (a new [round] after each Undo), adding, or added. */
sealed interface QuickAddPhase {
    val round: Int

    data class Typing(val initial: Money? = null, override val round: Int = 0) : QuickAddPhase

    data class Adding(override val round: Int) : QuickAddPhase

    data class Added(val spend: Spend, override val round: Int = 0) : QuickAddPhase
}
