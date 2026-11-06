package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.model.Money

/**
 * The amounts the numpad offers above its keys, one touch each (owner, 6 Nov 2026): the cap's
 * habits, the €1.20 coffee typed again and again. Pure: a cap's last spends' amounts in, newest
 * first, at most [WINDOW] of them; out, at most [SHOWN] amounts.
 *
 * - The most frequent amounts of the window; a tie goes to the one used last.
 * - Shown smallest first, so an amount keeps its place from one day to the next while the habits
 *   hold, and a thumb learns where it is.
 * - Never a date, a count or a list of spends: an amount to type, nothing a ledger would say.
 */
object RecentAmounts {
    /** How many of a cap's last spends make its habits. */
    const val WINDOW = 20

    /** How many amounts the numpad has room for on a phone, at any text size. */
    const val SHOWN = 3

    fun pick(newestFirst: List<Money>): List<Money> {
        val window = newestFirst.take(WINDOW).filter { it.isPositive }
        val counts = window.groupingBy { it }.eachCount()
        // The window is newest first: an amount's first index is its last use.
        val lastUse = window.distinct().associateWith { amount -> window.indexOf(amount) }
        return counts.keys
            .sortedWith(compareByDescending<Money> { counts.getValue(it) }.thenBy { lastUse.getValue(it) })
            .take(SHOWN)
            .sorted()
    }
}
