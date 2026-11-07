package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.period.PeriodCalendar
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The reader's month at the end of the caps screen (owner, 6 Nov 2026), to set the caps against a
 * salary: what was [added] to the caps in [month] so far, what was added in the month before
 * ([addedLastMonth], over [lastMonth]), and what the caps are [worth] in a month. Two sums of what
 * the reader added and one planning figure: never a statistic, no month before the last, no
 * chart, no choice of period, and never a sum of what is left, which the envelopes keep apart.
 */
data class YourMonth(
    val month: DateRange,
    val added: Money,
    val lastMonth: DateRange,
    val addedLastMonth: Money,
    val worth: Money,
)

/** The arithmetic of [YourMonth], pure. */
object MonthSums {
    /**
     * The reader's month holding [today]: from the month start day of Settings, the salary's for a
     * reader who set it so ("from the 27th"), as a month's cap runs.
     */
    fun month(today: LocalDate, monthStartDay: Int): DateRange =
        PeriodCalendar.containing(today, monthRule(monthStartDay))

    /** The month before [month], by the same start day. */
    fun previous(month: DateRange, monthStartDay: Int): DateRange =
        PeriodCalendar.containing(month.start.minusDays(1), monthRule(monthStartDay))

    /**
     * What [caps] are worth in [month], about: a month's cap its amount; a day's for each day of
     * the month, a week's for its days over seven; a year's a twelfth. Rounded to the currency's
     * whole unit, since a week's or a year's share is an estimate anyway ("about €1,250 a month").
     * The reserves are not counted: they are what past periods left, not what a month is given.
     * A one-off cap counts its share of the month's days ([oneOffDates] gives an ended cap's, from
     * its closed line; an open one's are its own).
     */
    fun worth(
        caps: List<Cap>,
        month: DateRange,
        currency: AppCurrency,
        oneOffDates: Map<Long, DateRange> = emptyMap(),
    ): Money {
        val days = month.length.toLong()
        // In 84ths of a minor unit (seven days times twelve months), so the shares are exact and
        // the one rounding is the last: cut per cap, they would drift by a unit. A one-off's share
        // is its own fraction, cut to an 84th: a part of a hundredth of a cent, far under the last.
        val parts = caps.sumOf { cap ->
            val amount = cap.amount.minor
            when (cap.kind) {
                PeriodKind.DAY -> amount * days * PARTS
                PeriodKind.WEEK -> amount * days * (PARTS / DAYS_IN_WEEK)
                PeriodKind.MONTH -> amount * PARTS
                PeriodKind.YEAR -> amount * (PARTS / MONTHS_IN_YEAR)
                PeriodKind.ONE_OFF -> oneOffParts(amount, oneOffDates[cap.id] ?: cap.oneOffDates, month)
            }
        }
        val unit = currency.unit.minor * PARTS
        return Money((parts + unit / 2) / unit * currency.unit.minor)
    }

    /**
     * A one-off cap's share of [month] (owner, 6 Nov 2026): its amount over its days, for its days
     * in the month; nothing for dates outside it, or for an ended cap whose [dates] are not known.
     */
    private fun oneOffParts(amount: Long, dates: DateRange?, month: DateRange): Long {
        dates ?: return 0
        val from = maxOf(dates.start, month.start)
        val until = minOf(dates.endExclusive, month.endExclusive)
        if (until <= from) return 0
        val inMonth = ChronoUnit.DAYS.between(from, until)
        return Math.multiplyExact(Math.multiplyExact(amount, inMonth), PARTS) / dates.length
    }

    /** A month's rule: the week's first day plays no part in it. */
    private fun monthRule(monthStartDay: Int) = PeriodRule(PeriodKind.MONTH, DayOfWeek.MONDAY, monthStartDay)

    private const val DAYS_IN_WEEK = 7L
    private const val MONTHS_IN_YEAR = 12L
    private const val PARTS = DAYS_IN_WEEK * MONTHS_IN_YEAR
}
