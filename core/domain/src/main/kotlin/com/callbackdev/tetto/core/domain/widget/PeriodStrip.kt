package com.callbackdev.tetto.core.domain.widget

import com.callbackdev.tetto.core.model.DateRange
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The strip under «In words»'s cap on a wide card (owner, 14 Nov 2026; PLANNING.md §7): the cap's
 * period in its units, today's among them, so how much of it is left reads at a glance, the way the
 * family's other ring cards draw their day under their ring. Not a chart: no amount stands on it,
 * only the calendar (VISION, «No ledger»).
 *
 * The units: the period's days, up to [MAX_DAYS] (a week, a month, a one-off of a month at most);
 * a year's months; nothing for a day's cap, or a one-off too long to draw by days, where the card
 * keeps its tall form.
 *
 * @property unit what one box is: a day or a month.
 * @property first the first unit's first day.
 * @property count how many boxes.
 * @property today the box today falls in, 0-based.
 */
data class PeriodStrip(val unit: StripUnit, val first: LocalDate, val count: Int, val today: Int) {
    /** The first day of the box at [index]. */
    fun dayOf(index: Int): LocalDate = when (unit) {
        StripUnit.DAY -> first.plusDays(index.toLong())
        StripUnit.MONTH -> first.plusMonths(index.toLong())
    }

    companion object {
        /** The period's strip as of [today], or none (see the class). */
        fun of(period: DateRange, today: LocalDate): PeriodStrip? {
            val days = period.length
            if (days in MIN_DAYS..MAX_DAYS) {
                val at = ChronoUnit.DAYS.between(period.start, today).toInt().coerceIn(0, days - 1)
                return PeriodStrip(StripUnit.DAY, period.start, days, at)
            }
            // A year by its months, from the month it starts in to the month of its last day.
            val firstMonth = period.start.withDayOfMonth(1)
            val months = ChronoUnit.MONTHS.between(firstMonth, period.lastDay.withDayOfMonth(1)).toInt() + 1
            if (days > MAX_DAYS && months in MIN_MONTHS..MAX_MONTHS) {
                val at = ChronoUnit.MONTHS.between(firstMonth, today.withDayOfMonth(1)).toInt().coerceIn(0, months - 1)
                return PeriodStrip(StripUnit.MONTH, firstMonth, months, at)
            }
            return null
        }

        const val MIN_DAYS = 2
        const val MAX_DAYS = 31
        const val MIN_MONTHS = 2
        const val MAX_MONTHS = 12
    }
}

enum class StripUnit { DAY, MONTH }
