package com.callbackdev.tetto.feature.caps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.time.DayTicks
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.domain.cap.CapsSentence
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.Spend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The caps screen, its sheets, the editor and the order (PLANNING.md §11 Phase 2).
 *
 * Live while visible and silent otherwise (§4.4, §9): the caps are read again at each local
 * midnight and at each change of clock or zone ([DayTicks]), only while the page collects; the
 * repository closes what ended before every read. Every change says so after it, with Undo where
 * it can be undone ([notices]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CapsViewModel
@Inject
constructor(
    private val caps: CapsRepository,
    private val settings: SettingsRepository,
    days: DayTicks,
    private val clock: TodaySource,
) : ViewModel() {
    val state: StateFlow<CapsUiState?> =
        combine(
            // One ticker for the caps and the month: each day (and each change of clock or zone)
            // reads both again, as two tickers did, with one receiver and one wait instead of two.
            days.days().flatMapLatest { combine(caps.states, caps.yourMonth, ::Pair) },
            settings.currency,
            settings.settings.map { it.capsTipSeen to it.capsLayout }.distinctUntilChanged(),
        ) { (states, month), currency, (tipSeen, layout) ->
            CapsUiState(clock.today(), currency, states, CapsSentence.of(states), tipSeen, layout, month)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val sheetCap = MutableStateFlow<Long?>(null)

    /** The cap's sheet open now, if any: its spends and its lines, as they change. */
    val sheet: StateFlow<CapSheetData?> = sheetCap.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            combine(caps.observeOpenSpends(id), caps.observeClosedPeriods(id, LAST_PERIODS)) { spends, lines ->
                CapSheetData(id, spends, lines)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val numpadTarget = MutableStateFlow<NumpadTarget?>(null)

    /** The numpad open now, if any. */
    val numpad: StateFlow<NumpadTarget?> = numpadTarget.asStateFlow()

    /** The amounts the open numpad offers, one touch each; none while it corrects a spend. */
    val numpadAmounts: StateFlow<List<Money>> = numpadTarget.flatMapLatest { target ->
        if (target == null || target.correcting != null || target.refund) {
            flowOf(emptyList())
        } else {
            caps.observeRecentAmounts(target.capId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val noticeChannel = Channel<CapsNotice>(Channel.BUFFERED)

    val notices: Flow<CapsNotice> = noticeChannel.receiveAsFlow()

    fun openSheet(capId: Long) {
        sheetCap.value = capId
    }

    fun closeSheet() {
        sheetCap.value = null
    }

    fun openNumpad(capId: Long, correcting: Spend? = null) {
        numpadTarget.value = NumpadTarget(capId, correcting)
    }

    /** The numpad, for money back on the cap [capId] (owner, 6 Nov 2026). */
    fun openRefund(capId: Long) {
        numpadTarget.value = NumpadTarget(capId, refund = true)
    }

    fun closeNumpad() {
        numpadTarget.value = null
    }

    /** The numpad's Add, or its Save for a correction. */
    fun confirmNumpad(target: NumpadTarget, amount: Money) {
        // Once only: a second Add from the same numpad (two touches in one frame, Enter held on a
        // keyboard) arrives after the first has closed it, and must not add the spend again.
        if (!numpadTarget.compareAndSet(target, null)) return
        val correcting = target.correcting
        when {
            correcting != null -> correctSpend(correcting, amount)
            target.refund -> addRefund(target.capId, amount)
            else -> addSpend(target.capId, amount)
        }
    }

    private fun addRefund(capId: Long, amount: Money) = launch {
        val refund = caps.addRefund(capId, amount) ?: return@launch
        noticeChannel.send(CapsNotice.Added(refund, nameOf(capId)))
    }

    private fun addSpend(capId: Long, amount: Money) = launch {
        val spend = caps.addSpend(capId, amount) ?: return@launch
        noticeChannel.send(CapsNotice.Added(spend, nameOf(capId)))
    }

    private fun correctSpend(spend: Spend, amount: Money) = launch {
        val previous = caps.editSpend(spend.id, amount)
        noticeChannel.send(
            if (previous == null) CapsNotice.PeriodClosed else CapsNotice.Corrected(spend.id, amount, previous),
        )
    }

    fun deleteSpend(spend: Spend) = launch {
        val deleted = caps.deleteSpend(spend.id)
        noticeChannel.send(
            if (deleted == null) CapsNotice.PeriodClosed else CapsNotice.Deleted(deleted, nameOf(spend.capId)),
        )
    }

    fun clearReserve(capId: Long) = launch {
        val cleared = caps.clearReserve(capId) ?: return@launch
        noticeChannel.send(CapsNotice.ReserveCleared(cleared, nameOf(capId)))
    }

    fun deleteCap(capId: Long) = launch {
        val name = nameOf(capId)
        if (sheetCap.value == capId) sheetCap.value = null
        if (caps.deleteCap(capId)) noticeChannel.send(CapsNotice.CapDeleted(name))
    }

    /** Undo, by the notice that offered it. */
    fun undo(notice: CapsNotice) = launch {
        when (notice) {
            is CapsNotice.Added -> caps.undoAdd(notice.spend)
            is CapsNotice.Corrected -> caps.undoEdit(notice.spendId, notice.previous)
            is CapsNotice.Deleted -> caps.undoDelete(notice.spend)
            is CapsNotice.ReserveCleared -> caps.undoClearReserve(notice.cleared)
            is CapsNotice.CapDeleted, CapsNotice.PeriodClosed -> Unit
        }
    }

    /** The first-day tip was dismissed, or followed to the guide: never again (PLANNING.md §11 Phase 6). */
    fun onTipSeen() = launch { settings.noteCapsTipSeen() }

    /** The caps in the order of [capIds]. */
    fun reorder(capIds: List<Long>) = launch { caps.reorder(capIds) }

    /** A new cap, or the cap [capId] changed (§4.5); the id it has. */
    suspend fun saveCap(capId: Long?, name: String, icon: CapIcon, amount: Money, kind: PeriodKind): Long =
        if (capId == null) {
            caps.addCap(name, icon, amount, kind)
        } else {
            caps.changeCap(capId, name, icon, amount, kind)
            capId
        }

    private fun nameOf(capId: Long): String = state.value?.cap(capId)?.cap?.name.orEmpty()

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** The cap's sheet shows the last three periods (owner, 5 Nov 2026). */
        const val LAST_PERIODS = 3
    }
}
