package com.callbackdev.tetto.core.domain.period

import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Periods as ranges of local dates (PLANNING.md §4.2). Pure: a date and a rule in, a range out.
 *
 * - A day is that day.
 * - A week runs seven days from the week's first day on or before the date.
 * - A month runs from its start day `d` to the next month's: each month starts on `min(d, its
 *   length)`, so with `d = 31` the months start on 31 Jan, 28 (or 29) Feb, 31 Mar, 30 Apr.
 * - A year is the calendar year, from 1 January (owner, 7 Nov 2026): the month's start day plays
 *   no part in it.
 * - A one-off cap has no rule's period: its dates are its own ([openPeriod] of a [Cap]).
 */
object PeriodCalendar {
    fun containing(date: LocalDate, rule: PeriodRule): DateRange = when (rule.kind) {
        PeriodKind.DAY -> DateRange(date, date.plusDays(1))

        PeriodKind.WEEK -> {
            val start = date.with(TemporalAdjusters.previousOrSame(rule.weekStart))
            DateRange(start, start.plusDays(7))
        }

        PeriodKind.MONTH -> {
            val month = YearMonth.from(date)
            val thisStart = monthStart(month, rule.monthStartDay)
            if (date >= thisStart) {
                DateRange(thisStart, monthStart(month.plusMonths(1), rule.monthStartDay))
            } else {
                DateRange(monthStart(month.minusMonths(1), rule.monthStartDay), thisStart)
            }
        }

        // The calendar year, whatever day the months start on (owner, 7 Nov 2026): a year's cap is
        // a holiday's or the gifts', thought of from January to December, and a salary's month
        // start ("from the 27th") made it run from 27 January to 26 January.
        PeriodKind.YEAR -> {
            val thisStart = LocalDate.of(date.year, 1, 1)
            DateRange(thisStart, thisStart.plusYears(1))
        }

        PeriodKind.ONE_OFF -> throw IllegalArgumentException("A one-off cap's dates are its own, never a rule's")
    }

    /** The period after [period]: the one its end falls in. */
    fun next(period: DateRange, rule: PeriodRule): DateRange = containing(period.endExclusive, rule)

    /**
     * A cap's open period: from its [openStart] to the end of the rule's period that holds it.
     * Usually [openStart] is that period's own start; where it is not (a period replaced mid-way,
     * after closed ones, §4.5), the open period is the rest of it, with its share of the cap
     * (`PeriodAmount`), and the next one is whole.
     */
    fun openPeriod(openStart: LocalDate, rule: PeriodRule): DateRange =
        DateRange(openStart, containing(openStart, rule).endExclusive)

    /**
     * [cap]'s open period: a one-off's own dates (owner, 6 Nov 2026), else the rule's, as
     * [openPeriod] of its start. Not for a cap that has [Cap.ended]: it has no open period.
     */
    fun openPeriod(cap: Cap, rule: PeriodRule): DateRange {
        val end = cap.endExclusive ?: return openPeriod(cap.openStart, rule)
        require(!cap.ended) { "An ended cap has no open period" }
        return DateRange(cap.openStart, end)
    }

    private fun monthStart(month: YearMonth, day: Int): LocalDate = month.atDay(minOf(day, month.lengthOfMonth()))
}
