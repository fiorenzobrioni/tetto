package com.callbackdev.tetto.core.domain.widget

import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.Share
import java.time.LocalDate

/** Which caps a card shows (PLANNING.md §7, the cards' settings), from the caps in the reader's order. */
object WidgetCaps {
    /**
     * «At a glance»: the caps the reader [chosen] for this card, in the reader's order; every cap
     * when they chose none (null), and again when every one they chose has been deleted since,
     * rather than a card with nothing to show. A cap made after the choice is not added to it.
     */
    fun glance(all: List<CapState>, chosen: Set<Long>?): List<CapState> {
        // An ended one-off cap leaves the cards (owner, 6 Nov 2026): a card says what is left now.
        val states = all.filterNot { it.ended }
        if (chosen == null) return states
        return states.filter { it.cap.id in chosen }.ifEmpty { states }
    }

    /**
     * «In words»: the cap the reader [chosen] for this card, or none when it has been deleted
     * (the card says so), ended or not (a chosen one-off says it has ended); with none chosen
     * (null), the one closest to its cap: the first over in the reader's order, else the lowest
     * share left, a tie to the first, never an ended one-off. None without caps.
     */
    fun words(all: List<CapState>, chosen: Long?): CapState? {
        if (chosen != null) return all.firstOrNull { it.cap.id == chosen }
        // Never an ended one-off as the closest (owner, 6 Nov 2026): its figures are over.
        val states = all.filterNot { it.ended }
        states.firstOrNull { it.over }?.let { return it }
        return states.minWithOrNull(compareBy { Share(it.left.minor, (it.amount + it.cap.reserve).minor) })
    }

    /**
     * «In words»'s other caps, under its own on a wide card (owner, 14 Nov 2026): the ones the
     * reader [chosen], in the reader's order, never the card's own [main] nor an ended one-off; a
     * cap deleted since is simply gone. At most [most], the most the card holds side by side.
     */
    fun below(all: List<CapState>, main: CapState?, chosen: Set<Long>, most: Int = Int.MAX_VALUE): List<CapState> =
        all.filter { it.cap.id in chosen && !it.ended && it.cap.id != main?.cap?.id }.take(most.coerceAtLeast(0))
}

/**
 * What «In words» says of its cap under the figure on a tall card (PLANNING.md §7, «the cap's
 * sentence»): a kind and its values, never a string; the words are the widget's resources.
 *
 * - [Pace]: about how much a day is left until the period's last day, or less than one unit
 *   ([Pace.lessThan]), and whether spending runs ahead of the days ("Ahead of the days: about €4 a
 *   day until Sunday").
 * - [Ahead]: spending runs ahead of the days, with no pace to say (under one unit a day).
 * - [Reserve]: a day's cap, or a period's last day, with something in reserve.
 * - [Forgiven]: over: what the reserve cannot cover is forgiven when the period ends, the day it
 *   starts again; a one-off's, the day after its last, when it ends ([Forgiven.renews] false).
 * - [Ended]: a one-off cap whose dates are over (owner, 6 Nov 2026): what they spent of its cap.
 *
 * None where the cap has nothing to add to its figure and its last day: a section with nothing to
 * say is not drawn.
 */
sealed interface CapNote {
    data class Pace(val perDay: Money, val lastDay: LocalDate, val ahead: Boolean, val lessThan: Boolean = false) :
        CapNote

    data object Ahead : CapNote

    data class Reserve(val reserve: Money) : CapNote

    data class Forgiven(val startsAgain: LocalDate, val renews: Boolean = true) : CapNote

    data class Ended(val spent: Money, val cap: Money) : CapNote

    companion object {
        fun of(state: CapState): CapNote? {
            if (state.ended) return Ended(state.spent, state.amount)
            if (state.over) return Forgiven(state.startsAgain, state.renews)
            state.pace?.let { return Pace(it.perDay, state.lastDay, state.aheadOfDays, it.lessThan) }
            if (state.aheadOfDays) return Ahead
            if (state.cap.reserve.isPositive) return Reserve(state.cap.reserve)
            return null
        }
    }
}
