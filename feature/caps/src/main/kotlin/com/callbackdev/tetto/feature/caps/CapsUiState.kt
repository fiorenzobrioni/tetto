package com.callbackdev.tetto.feature.caps

import com.callbackdev.tetto.core.data.caps.ClearedReserve
import com.callbackdev.tetto.core.domain.cap.CapsSentence
import com.callbackdev.tetto.core.domain.cap.YourMonth
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.Spend
import java.time.LocalDate

/**
 * The caps screen (PLANNING.md §11 Phase 2): today, the currency, the caps in the reader's order, the
 * sentence; whether the first-day tip was seen (Phase 6); tiles or rows, as the reader chose; the
 * reader's [month] at the end (owner, 6 Nov 2026), once it is read.
 */
data class CapsUiState(
    val today: LocalDate,
    val currency: AppCurrency,
    val caps: List<CapState>,
    val sentence: CapsSentence,
    val tipSeen: Boolean = true,
    val layout: CapsLayout = CapsLayout.TILES,
    val month: YourMonth? = null,
) {
    fun cap(id: Long): CapState? = caps.firstOrNull { it.cap.id == id }

    /**
     * The first-day tip ("How to read a cap"), over the tiles until it is dismissed or followed to
     * the guide; never on an empty page, which has no tile to read and its own words.
     */
    val showsTip: Boolean get() = !tipSeen && caps.isNotEmpty()
}

/** A cap's sheet: this period's spends, newest first, and the last periods' lines, the latest first. */
data class CapSheetData(val capId: Long, val spends: List<Spend>, val lines: List<ClosedPeriod>)

/**
 * The numpad, open on [capId]: adding a spend, or a [refund] (owner, 6 Nov 2026), or correcting
 * [correcting], a spend or a refund of the cap's open period.
 */
data class NumpadTarget(
    val capId: Long,
    val correcting: Spend? = null,
    val refund: Boolean = correcting?.refund == true,
)

/** What the screen says after a change, with Undo where it can be undone. */
sealed interface CapsNotice {
    data class Added(val spend: Spend, val capName: String) : CapsNotice

    data class Corrected(val spendId: Long, val amount: Money, val previous: Money) : CapsNotice

    data class Deleted(val spend: Spend, val capName: String) : CapsNotice

    data class ReserveCleared(val cleared: ClearedReserve, val capName: String) : CapsNotice

    data class CapDeleted(val capName: String) : CapsNotice

    /** A correction or a deletion arrived after its period had closed (midnight passed meanwhile). */
    data object PeriodClosed : CapsNotice
}
