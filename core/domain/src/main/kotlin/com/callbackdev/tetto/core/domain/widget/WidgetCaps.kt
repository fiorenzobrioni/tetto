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
    fun glance(states: List<CapState>, chosen: Set<Long>?): List<CapState> {
        if (chosen == null) return states
        return states.filter { it.cap.id in chosen }.ifEmpty { states }
    }

    /**
     * «In words»: the cap the reader [chosen] for this card, or none when it has been deleted
     * (the card says so); with none chosen (null), the one closest to its cap: the first over in
     * the reader's order, else the lowest share left, a tie to the first. None without caps.
     */
    fun words(states: List<CapState>, chosen: Long?): CapState? {
        if (chosen != null) return states.firstOrNull { it.cap.id == chosen }
        states.firstOrNull { it.over }?.let { return it }
        return states.minWithOrNull(compareBy { Share(it.left.minor, (it.cap.amount + it.cap.reserve).minor) })
    }
}

/**
 * What «In words» says of its cap under the figure on a tall card (PLANNING.md §7, «the cap's
 * sentence»): a kind and its values, never a string; the words are the widget's resources.
 *
 * - [Pace]: about how much a day is left until the period's last day, and whether spending runs
 *   ahead of the days ("Ahead of the days: about €4 a day until Sunday").
 * - [Ahead]: spending runs ahead of the days, with no pace to say (under one unit a day).
 * - [Reserve]: a day's cap, or a period's last day, with something in reserve.
 * - [Forgiven]: over: what the reserve cannot cover is forgiven when the period ends, the day it
 *   starts again.
 *
 * None where the cap has nothing to add to its figure and its last day: a section with nothing to
 * say is not drawn.
 */
sealed interface CapNote {
    data class Pace(val perDay: Money, val lastDay: LocalDate, val ahead: Boolean) : CapNote

    data object Ahead : CapNote

    data class Reserve(val reserve: Money) : CapNote

    data class Forgiven(val startsAgain: LocalDate) : CapNote

    companion object {
        fun of(state: CapState): CapNote? {
            if (state.over) return Forgiven(state.startsAgain)
            state.pace?.let { return Pace(it, state.lastDay, state.aheadOfDays) }
            if (state.aheadOfDays) return Ahead
            if (state.cap.reserve.isPositive) return Reserve(state.cap.reserve)
            return null
        }
    }
}
