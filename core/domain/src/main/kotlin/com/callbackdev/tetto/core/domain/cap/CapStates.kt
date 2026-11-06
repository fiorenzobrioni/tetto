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
 * - Ahead of the days: not over, and the bar past the mark by more than a slack that starts at
 *   one step of the period (a day of a week, a week of a month, a month of a year) and fades to
 *   nothing by its last day (owner, 6 Nov 2026): a big shop on the first of the month is not a
 *   warning for weeks, while a cap nearly spent with days to go still is. [aheadLine].
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
        val aheadOfDays = !over && todayMark != null && bar > aheadLine(cap.kind, todayMark, daysLeft)

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

    /**
     * The share past which spending runs ahead of the days: the mark for today ([mark], `counted /
     * length`) and a slack of [slackDays] days of the period, faded by the days left: `counted /
     * length + slack / length × daysLeft / length`, kept exact as one share. On the first day it is
     * the mark and a whole step (a week's: two days of seven); on the last, the mark and almost
     * nothing.
     */
    fun aheadLine(kind: PeriodKind, mark: Share, daysLeft: Int): Share {
        val length = mark.whole
        return Share(mark.part * length + slackDays(kind) * daysLeft, length * length)
    }

    /** One step of a period of [kind]: a day of a week, a week of a month, a month of a year. */
    private fun slackDays(kind: PeriodKind): Long = when (kind) {
        PeriodKind.DAY -> 0
        PeriodKind.WEEK -> 1
        PeriodKind.MONTH -> DAYS_IN_WEEK
        PeriodKind.YEAR -> DAYS_IN_MONTH
    }

    private const val DAYS_IN_WEEK = 7L
    private const val DAYS_IN_MONTH = 30L
}
