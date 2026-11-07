package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.period.PeriodCalendar
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodRule
import com.callbackdev.tetto.core.model.Share

/**
 * What a cap gives its open period (PLANNING.md §4.5; owner, 7 Nov 2026): its whole amount, or,
 * for a period that starts after the rule's period began (a period cut short by a change: a daily
 * cap turned weekly on a Thursday, after its closed days; a month start moved past the open
 * period's), its share of the days, the open days over the whole period's, rounded down. The
 * share is derived from the dates, never stored: the next period is whole again, and a change of
 * amount applies to the share at once.
 *
 * A one-off cap's dates are its own, never cut by a rule: its whole amount.
 */
object PeriodAmount {
    fun of(cap: Cap, rule: PeriodRule): Money {
        val days = partOf(cap, rule) ?: return cap.amount
        return Money(Math.multiplyExact(cap.amount.minor, days.part) / days.whole)
    }

    /**
     * The open period's days over the whole period's, for a period cut short; null for a whole
     * one, and for a one-off cap.
     */
    fun partOf(cap: Cap, rule: PeriodRule): Share? {
        if (!cap.kind.renews) return null
        val whole = PeriodCalendar.containing(cap.openStart, rule)
        if (whole.start == cap.openStart) return null
        val open = PeriodCalendar.openPeriod(cap.openStart, rule)
        return Share(open.length.toLong(), whole.length.toLong())
    }
}
