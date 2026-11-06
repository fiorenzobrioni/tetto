package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.model.Money

/**
 * Closing one period (PLANNING.md §4.4), the concept's formula, exact and bounded:
 *
 * - `reserveAfter = min(limit, max(0, reserve + cap − spent))`: what the period left over is
 *   carried, up to the reserve's limit, one period's cap (owner, 5 Nov 2026): a period can at most
 *   double;
 * - `forgiven = max(0, spent − cap − reserve)`: the overspend the reserve could not cover is
 *   dropped. A new period never starts in debt.
 *
 * The limit is one period's cap, the cap in force when the period closes. Where a period is cut
 * short by a change (§4.5) its own cap is its share of the days, and the [limit] is the cap that
 * runs on after it, the one the reserve is held to from then on.
 */
object Rollover {
    data class Outcome(val reserveAfter: Money, val forgiven: Money)

    fun close(cap: Money, reserve: Money, spent: Money, limit: Money = cap): Outcome {
        val carried = reserve + cap - spent
        val reserveAfter = minOf(limit, maxOf(Money.ZERO, carried))
        val forgiven = maxOf(Money.ZERO, spent - cap - reserve)
        return Outcome(reserveAfter, forgiven)
    }
}
