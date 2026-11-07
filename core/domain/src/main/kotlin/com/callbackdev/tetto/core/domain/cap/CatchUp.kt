package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.period.PeriodCalendar
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import java.time.LocalDate

/**
 * What a catch-up writes for one cap (PLANNING.md §4.4): the [closed] lines, in order, and the cap's
 * new open period start and reserve. The repository applies it in one transaction.
 */
data class CatchUpPlan(val closed: List<ClosedPeriod>, val openStart: LocalDate, val reserve: Money)

/**
 * Closing every period of a cap that ended on or before today, in order (PLANNING.md §4.4). Pure:
 * the cap, what its open period spent, the rule in force and today in; the plan out, or null when
 * nothing ended, so that a catch-up run twice does nothing the second time.
 *
 * - The open period closes with its spends, as one line.
 * - The empty periods after it (a long absence: no spend can count in a period that was never
 *   open) fold into one line, from the first's start to the last's end, `periods = n`, its
 *   reserve computed period by period: a daily cap left for 400 days writes two lines, not 400.
 * - Today before the open period's start (a clock set back, a flight west) closes nothing: a
 *   period is never reopened, and a new spend counts in the open one.
 * - A one-off cap (owner, 6 Nov 2026) closes once, the day after its last: one line, what was
 *   spent and what was forgiven, no reserve; its open start moves to its end and the cap has
 *   ended. Never a new period, so an absence past its end writes that one line all the same.
 */
object CatchUp {
    fun plan(cap: Cap, spent: Money, rule: PeriodRule, today: LocalDate): CatchUpPlan? {
        if (cap.kind == PeriodKind.ONE_OFF) return oneOff(cap, spent, today)
        var period = PeriodCalendar.openPeriod(cap.openStart, rule)
        if (period.endExclusive > today) return null

        val first = Rollover.close(cap.amount, cap.reserve, spent)
        val closed = mutableListOf(
            ClosedPeriod(
                capId = cap.id,
                range = period,
                cap = cap.amount,
                spent = spent,
                reserveBefore = cap.reserve,
                reserveAfter = first.reserveAfter,
                forgiven = first.forgiven,
                periods = 1,
            ),
        )
        var reserve = first.reserveAfter

        period = PeriodCalendar.next(period, rule)
        if (period.endExclusive <= today) {
            val runStart = period.start
            val reserveBefore = reserve
            var periods = 0
            while (period.endExclusive <= today) {
                reserve = Rollover.close(cap.amount, reserve, Money.ZERO).reserveAfter
                periods++
                period = PeriodCalendar.next(period, rule)
            }
            closed += ClosedPeriod(
                capId = cap.id,
                range = DateRange(runStart, period.start),
                cap = cap.amount,
                spent = Money.ZERO,
                reserveBefore = reserveBefore,
                reserveAfter = reserve,
                // Nothing spent never goes past the cap.
                forgiven = Money.ZERO,
                periods = periods,
            )
        }
        return CatchUpPlan(closed, period.start, reserve)
    }

    private fun oneOff(cap: Cap, spent: Money, today: LocalDate): CatchUpPlan? {
        val dates = cap.oneOffDates ?: return null
        if (dates.endExclusive > today) return null
        // Nothing carries: a reserve held to nothing, and what the cap could not cover forgiven.
        val outcome = Rollover.close(cap.amount, cap.reserve, spent, limit = Money.ZERO)
        val line = ClosedPeriod(
            capId = cap.id,
            range = dates,
            cap = cap.amount,
            spent = spent,
            reserveBefore = cap.reserve,
            reserveAfter = outcome.reserveAfter,
            forgiven = outcome.forgiven,
            periods = 1,
        )
        return CatchUpPlan(listOf(line), dates.endExclusive, Money.ZERO)
    }
}
