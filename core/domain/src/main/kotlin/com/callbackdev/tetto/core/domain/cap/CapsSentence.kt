package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.Share

/**
 * The caps screen's sentence, and «In words»'s (PLANNING.md §4.6): a kind and its values, never a
 * string; the words are resources, in both languages, with plurals. Every kind carries [caps], how
 * many caps there are, so the words can tell one cap from several, and "every cap" from some.
 *
 * Which kind is said, when more than one holds (owner, 5 Nov 2026): a cap over first, then a cap
 * renewed today, then all under with the closest.
 */
sealed interface CapsSentence {
    val caps: Int

    data object NoCaps : CapsSentence {
        override val caps: Int get() = 0
    }

    /** One or more over: the [first] in the reader's order, when it starts again, and how many [others]. */
    data class Over(val first: CapState, val others: Int, override val caps: Int) : CapsSentence

    /**
     * None over, and a week's, a month's or a year's cap began a new period today: the [first]
     * in the reader's order, and how many [others]. A day's cap renews every day: not news.
     */
    data class Renewed(val first: CapState, val others: Int, override val caps: Int) : CapsSentence

    /** All under their caps: the [closest] to its cap (the lowest share left), its left and its last day. */
    data class AllUnder(val closest: CapState, override val caps: Int) : CapsSentence

    companion object {
        /** From the caps' states in the reader's order: a tie goes to the first. */
        fun of(states: List<CapState>): CapsSentence {
            if (states.isEmpty()) return NoCaps
            val caps = states.size

            val over = states.filter { it.over }
            if (over.isNotEmpty()) return Over(over.first(), over.size - 1, caps)

            val renewed = states.filter { it.renewedToday && it.cap.kind != PeriodKind.DAY }
            if (renewed.isNotEmpty()) return Renewed(renewed.first(), renewed.size - 1, caps)

            // None is over, so every share left is between zero and the whole.
            val closest = states.minWith(compareBy { Share(it.left.minor, (it.cap.amount + it.cap.reserve).minor) })
            return AllUnder(closest, caps)
        }
    }
}
