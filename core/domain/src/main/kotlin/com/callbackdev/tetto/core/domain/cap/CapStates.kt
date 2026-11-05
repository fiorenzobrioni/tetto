package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.period.PeriodCalendar
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import com.callbackdev.tetto.core.model.Share
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * What a cap says (PLANNING.md §4.6), from the cap, what its open period spent, the rule in force,
 * today and the currency (for the pace's whole units). Pure; called after the catch-up, so the
 * open period holds today or lies ahead of it (a clock set back), never behind.
 *
 * - `left = cap + reserve − spent`; over below zero.
 * - The bar: `spent / (cap + reserve)`, held to 0..1.
 * - The mark for today, for a week, a month or a year: `(today − start + 1) / length`, today
 *   counted.
 * - Ahead of the days: not over, and the bar past the mark. No fixed threshold.
 * - The pace, for a week, a month or a year, not over, with two days left or more:
 *   `left / daysLeft`, rounded down to a whole unit; none under one unit.
 */
object CapStates {
    fun of(
        cap: Cap,
        spent: Money,
        rule: PeriodRule,
        today: LocalDate,
        currency: AppCurrency,
        lastClosedEnd: LocalDate? = null,
    ): CapState {
        val period = PeriodCalendar.openPeriod(cap.openStart, rule)
        val length = period.length
        val base = cap.amount + cap.reserve
        val left = base - spent
        val over = left.isNegative
        val daysLeft = ChronoUnit.DAYS.between(today, period.endExclusive).toInt().coerceIn(1, length)

        val bar = Share(spent.minor.coerceIn(0, base.minor), base.minor)
        val todayMark = if (cap.kind == PeriodKind.DAY) {
            null
        } else {
            val counted = ChronoUnit.DAYS.between(period.start, today).toInt() + 1
            Share(counted.coerceIn(1, length).toLong(), length.toLong())
        }
        val aheadOfDays = !over && todayMark != null && bar > todayMark

        val pace = if (cap.kind != PeriodKind.DAY && !over && daysLeft >= 2) {
            val unit = currency.unit.minor
            val perDay = left.minor / daysLeft
            val whole = perDay - perDay % unit
            if (whole >= unit) Money(whole) else null
        } else {
            null
        }

        return CapState(
            cap = cap,
            period = period,
            spent = spent,
            left = left,
            daysLeft = daysLeft,
            bar = bar,
            todayMark = todayMark,
            aheadOfDays = aheadOfDays,
            pace = pace,
            renewedToday = lastClosedEnd == today && period.start == today,
        )
    }
}
